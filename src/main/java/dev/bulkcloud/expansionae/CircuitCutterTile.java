package dev.bulkcloud.expansionae;

import java.util.EnumSet;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.config.Upgrades;
import appeng.api.implementations.IUpgradeableHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.util.AECableType;
import appeng.api.util.AEPartLocation;
import appeng.api.util.IConfigManager;
import appeng.me.GridAccessException;
import appeng.parts.automation.UpgradeInventory;
import appeng.tile.grid.AENetworkPowerTileEntity;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.util.inv.InvOperation;

/**
 * Backport of ExtendedAE's Circuit Slicer. It cuts storage blocks directly into
 * processor prints and preserves the upstream 2/3/5/10/50 speed curve.
 */
public final class CircuitCutterTile extends AENetworkPowerTileEntity
        implements IGridTickable, IUpgradeableHost {
    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    public static final int TANK_CAPACITY = 16000;
    public static final int MAX_PROCESSING_TIME = 200;

    private final AppEngInternalInventory inventory = new AppEngInternalInventory(this, 2);
    private final UpgradeInventory upgrades;
    private final FluidTank tank;
    private final IFluidHandler fillOnlyTank = new FillOnlyFluidHandler();
    private final LazyOptional<IFluidHandler> fluidCapability = LazyOptional.of(() -> fillOnlyTank);
    private final IItemHandler externalItems = new ExternalItemHandler();

    private CircuitCutterRecipe cachedRecipe;
    private int processingTime;
    private boolean autoExport;

    public CircuitCutterTile(TileEntityType<?> type) {
        super(type);
        getProxy().setValidSides(EnumSet.allOf(Direction.class));
        getProxy().setIdlePowerUsage(0.0);
        setInternalMaxPower(8000);

        inventory.setMaxStackSize(INPUT_SLOT, 64);
        inventory.setMaxStackSize(OUTPUT_SLOT, 64);
        upgrades = new UpgradeInventory(this, 4) {
            @Override
            public int getMaxInstalled(Upgrades upgrade) {
                return upgrade == Upgrades.SPEED ? 4 : 0;
            }
        };
        tank = new FluidTank(TANK_CAPACITY) {
            @Override
            protected void onContentsChanged() {
                machineChanged(false);
            }
        };
    }

    @Override
    public AECableType getCableConnectionType(AEPartLocation dir) {
        return AECableType.COVERED;
    }

    public int getProcessingTime() { return processingTime; }
    public int getMaxProcessingTime() { return MAX_PROCESSING_TIME; }
    public int getFluidAmount() { return tank.getFluidAmount(); }
    public boolean isAutoExport() { return autoExport; }

    public void setAutoExport(boolean autoExport) {
        if (this.autoExport != autoExport) {
            this.autoExport = autoExport;
            saveChanges();
            markForUpdate();
            wake();
        }
    }

    public IFluidHandler getFluidHandler() {
        return fillOnlyTank;
    }

    @Override
    public IItemHandler getInternalInventory() {
        return inventory;
    }

    @Override
    protected IItemHandler getItemHandlerForSide(@Nonnull Direction facing) {
        return externalItems;
    }

    @Override
    public IItemHandler getInventoryByName(String name) {
        if ("inv".equals(name)) return inventory;
        if ("upgrades".equals(name)) return upgrades;
        return null;
    }

    @Override
    public int getInstalledUpgrades(Upgrades upgrade) {
        return upgrades.getInstalledUpgrades(upgrade);
    }

    @Override
    public IConfigManager getConfigManager() {
        return null;
    }

    @Override
    public void onChangeInventory(IItemHandler inv, int slot, InvOperation operation,
            ItemStack removed, ItemStack added) {
        if (inv == inventory) {
            machineChanged(slot == INPUT_SLOT);
        } else {
            saveChanges();
            wake();
        }
    }

    private void machineChanged(boolean resetProgress) {
        cachedRecipe = null;
        if (resetProgress) processingTime = 0;
        saveChanges();
        markForUpdate();
        wake();
    }

    private void wake() {
        try {
            getProxy().getTick().wakeDevice(getProxy().getNode());
        } catch (GridAccessException ignored) {
        }
    }

    @Nullable
    private CircuitCutterRecipe getTask() {
        if (world == null) return null;
        ItemStack input = inventory.getStackInSlot(INPUT_SLOT);
        FluidStack fluid = tank.getFluid();
        if (cachedRecipe != null && cachedRecipe.matchesMachine(input, fluid) && canAccept(cachedRecipe)) {
            return cachedRecipe;
        }

        cachedRecipe = null;
        Inventory probe = new Inventory(1);
        probe.setInventorySlotContents(0, input.isEmpty() ? ItemStack.EMPTY : input.copy());
        for (CircuitCutterRecipe candidate :
                world.getRecipeManager().getRecipes(CircuitCutterRecipe.TYPE, probe, world)) {
            if (candidate.matchesMachine(input, fluid) && canAccept(candidate)) {
                cachedRecipe = candidate;
                break;
            }
        }
        return cachedRecipe;
    }

    private boolean canAccept(CircuitCutterRecipe recipe) {
        return inventory.insertItem(OUTPUT_SLOT, recipe.getOutputItem(), true).isEmpty();
    }

    private boolean hasWork() {
        return getTask() != null;
    }

    private boolean hasAutoExportWork() {
        return autoExport && !inventory.getStackInSlot(OUTPUT_SLOT).isEmpty();
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(1, 20, !hasWork() && !hasAutoExportWork(), true);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        if (hasAutoExportWork() && pushOutput()) {
            return TickRateModulation.URGENT;
        }

        CircuitCutterRecipe task = getTask();
        if (task == null) {
            processingTime = 0;
            return hasAutoExportWork() ? TickRateModulation.IDLE : TickRateModulation.SLEEP;
        }

        int speed = speedForCards(upgrades.getInstalledUpgrades(Upgrades.SPEED));
        int steps = Math.max(1, ticksSinceLastCall) * speed;
        double energyPerStep = Math.max(1.0, task.getEnergy() / (double) MAX_PROCESSING_TIME);
        double requested = energyPerStep * steps;

        IEnergySource source = this;
        double available = extractAEPower(requested, Actionable.SIMULATE, PowerMultiplier.CONFIG);
        if (available + 0.001 < requested) {
            try {
                IEnergyGrid grid = getProxy().getEnergy();
                available = grid.extractAEPower(requested, Actionable.SIMULATE, PowerMultiplier.CONFIG);
                source = grid;
            } catch (GridAccessException ignored) {
                return TickRateModulation.SLOWER;
            }
        }
        if (available + 0.001 < requested) {
            return TickRateModulation.SLOWER;
        }

        source.extractAEPower(requested, Actionable.MODULATE, PowerMultiplier.CONFIG);
        processingTime += steps;
        if (processingTime >= MAX_PROCESSING_TIME) {
            finish(task);
        }
        markForUpdate();
        return TickRateModulation.URGENT;
    }

    private static int speedForCards(int cards) {
        switch (cards) {
            case 1: return 3;
            case 2: return 5;
            case 3: return 10;
            case 4: return 50;
            default: return 2;
        }
    }

    private void finish(CircuitCutterRecipe recipe) {
        ItemStack input = inventory.getStackInSlot(INPUT_SLOT);
        if (!recipe.matchesMachine(input, tank.getFluid()) || !canAccept(recipe)) {
            processingTime = 0;
            cachedRecipe = null;
            return;
        }

        inventory.extractItem(INPUT_SLOT, recipe.getInputAmount(), false);
        FluidStack fluid = recipe.getInputFluid();
        if (!fluid.isEmpty()) {
            tank.drain(fluid.getAmount(), IFluidHandler.FluidAction.EXECUTE);
        }
        inventory.insertItem(OUTPUT_SLOT, recipe.getOutputItem(), false);

        processingTime = 0;
        cachedRecipe = null;
        saveChanges();
    }

    private boolean pushOutput() {
        if (world == null || world.isRemote()) return false;
        ItemStack output = inventory.getStackInSlot(OUTPUT_SLOT);
        if (output.isEmpty()) return false;

        for (Direction direction : Direction.values()) {
            TileEntity targetTile = world.getTileEntity(pos.offset(direction));
            if (targetTile == null || targetTile instanceof CircuitCutterTile) continue;
            IItemHandler target = targetTile
                    .getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, direction.getOpposite())
                    .orElse(null);
            if (target == null) continue;

            ItemStack remainder = ItemHandlerHelper.insertItemStacked(target, output.copy(), false);
            if (remainder.getCount() != output.getCount()) {
                inventory.setStackInSlot(OUTPUT_SLOT, remainder);
                return true;
            }
        }
        return false;
    }

    @Override
    public CompoundNBT write(CompoundNBT data) {
        super.write(data);
        inventory.writeToNBT(data, "inventory");
        upgrades.writeToNBT(data, "upgrades");
        data.put("tank", tank.writeToNBT(new CompoundNBT()));
        data.putInt("processingTime", processingTime);
        data.putBoolean("autoExport", autoExport);
        return data;
    }

    @Override
    public void read(BlockState state, CompoundNBT data) {
        super.read(state, data);
        inventory.readFromNBT(data, "inventory");
        upgrades.readFromNBT(data, "upgrades");
        if (data.contains("tank")) tank.readFromNBT(data.getCompound("tank"));
        processingTime = data.getInt("processingTime");
        autoExport = data.getBoolean("autoExport");
        cachedRecipe = null;
    }

    @Override
    public void getDrops(World world, BlockPos pos, List<ItemStack> drops) {
        super.getDrops(world, pos, drops);
        for (ItemStack stack : upgrades) {
            if (!stack.isEmpty()) drops.add(stack);
        }
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return fluidCapability.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void remove() {
        super.remove();
        fluidCapability.invalidate();
    }

    private final class FillOnlyFluidHandler implements IFluidHandler {
        @Override public int getTanks() { return 1; }
        @Nonnull @Override public FluidStack getFluidInTank(int tankIndex) { return tank.getFluid().copy(); }
        @Override public int getTankCapacity(int tankIndex) { return TANK_CAPACITY; }
        @Override public boolean isFluidValid(int tankIndex, @Nonnull FluidStack stack) { return tank.isFluidValid(stack); }
        @Override public int fill(FluidStack resource, FluidAction action) { return tank.fill(resource, action); }
        @Nonnull @Override public FluidStack drain(FluidStack resource, FluidAction action) { return FluidStack.EMPTY; }
        @Nonnull @Override public FluidStack drain(int maxDrain, FluidAction action) { return FluidStack.EMPTY; }
    }

    private final class ExternalItemHandler implements IItemHandler {
        @Override public int getSlots() { return 2; }
        @Override public ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return slot == INPUT_SLOT ? inventory.insertItem(INPUT_SLOT, stack, simulate) : stack;
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == OUTPUT_SLOT ? inventory.extractItem(OUTPUT_SLOT, amount, simulate) : ItemStack.EMPTY;
        }
        @Override public int getSlotLimit(int slot) { return inventory.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return slot == INPUT_SLOT; }
    }
}
