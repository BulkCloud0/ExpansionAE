package dev.bulkcloud.expansionae;

import java.util.EnumSet;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.inventory.Inventory;
import net.minecraft.nbt.CompoundNBT;
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
import net.minecraftforge.items.IItemHandler;

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
 * AdvancedAE Reaction Chamber adapted to the AE2 8.4 item/fluid split.
 */
public final class ReactionChamberTile extends AENetworkPowerTileEntity
        implements IGridTickable, IUpgradeableHost {
    public static final int INPUT_SLOTS = 9;
    public static final int OUTPUT_SLOT = 9;
    private static final int PROCESSING_STEPS = 200;
    private static final int TANK_CAPACITY = 16000;

    private final AppEngInternalInventory inventory = new AppEngInternalInventory(this, 10);
    private final UpgradeInventory upgrades;
    private final FluidTank inputTank;
    private final FluidTank outputTank;
    private final LazyOptional<IFluidHandler> fluidCapability;

    private ReactionChamberRecipe cachedRecipe;
    private int processingTime;

    public ReactionChamberTile(TileEntityType<?> type) {
        super(type);
        getProxy().setValidSides(EnumSet.allOf(Direction.class));
        getProxy().setIdlePowerUsage(0.0);
        setInternalMaxPower(500000);

        inventory.setMaxStackSize(OUTPUT_SLOT, 64);
        upgrades = new UpgradeInventory(this, 4) {
            @Override
            public int getMaxInstalled(Upgrades upgrade) {
                return upgrade == Upgrades.SPEED ? 4 : 0;
            }
        };

        inputTank = new FluidTank(TANK_CAPACITY) {
            @Override protected void onContentsChanged() { chamberChanged(); }
        };
        outputTank = new FluidTank(TANK_CAPACITY) {
            @Override protected void onContentsChanged() { chamberChanged(); }
        };
        fluidCapability = LazyOptional.of(ChamberFluidHandler::new);
    }

    @Override
    public AECableType getCableConnectionType(AEPartLocation dir) {
        return AECableType.COVERED;
    }

    public int getProcessingTime() { return processingTime; }
    public int getMaxProcessingTime() { return PROCESSING_STEPS; }
    public FluidStack getInputFluid() { return inputTank.getFluid().copy(); }
    public FluidStack getOutputFluid() { return outputTank.getFluid().copy(); }

    @Override
    public IItemHandler getInternalInventory() {
        return inventory;
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
        cachedRecipe = null;
        if (slot < INPUT_SLOTS) processingTime = 0;
        chamberChanged();
    }

    private void chamberChanged() {
        cachedRecipe = null;
        saveChanges();
        markForUpdate();
        try {
            getProxy().getTick().wakeDevice(getProxy().getNode());
        } catch (GridAccessException ignored) {
        }
    }

    @Nullable
    private ReactionChamberRecipe getTask() {
        if (world == null) return null;
        if (cachedRecipe != null
                && cachedRecipe.matchesMachine(inventory, inputTank.getFluid())
                && canAccept(cachedRecipe)) {
            return cachedRecipe;
        }

        cachedRecipe = null;
        Inventory probe = new Inventory(INPUT_SLOTS);
        for (int slot = 0; slot < INPUT_SLOTS; slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            probe.setInventorySlotContents(slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
        }
        for (ReactionChamberRecipe candidate :
                world.getRecipeManager().getRecipes(ReactionChamberRecipe.TYPE, probe, world)) {
            if (candidate.matchesMachine(inventory, inputTank.getFluid()) && canAccept(candidate)) {
                cachedRecipe = candidate;
                break;
            }
        }
        return cachedRecipe;
    }

    private boolean canAccept(ReactionChamberRecipe recipe) {
        ItemStack itemOutput = recipe.getOutputItem();
        if (!itemOutput.isEmpty() && !inventory.insertItem(OUTPUT_SLOT, itemOutput, true).isEmpty()) {
            return false;
        }

        FluidStack fluidOutput = recipe.getOutputFluid();
        if (!fluidOutput.isEmpty()) {
            FluidStack current = outputTank.getFluid();
            if (!current.isEmpty() && !current.isFluidEqual(fluidOutput)) return false;
            if (current.getAmount() + fluidOutput.getAmount() > outputTank.getCapacity()) return false;
        }
        return true;
    }

    private boolean hasWork() {
        return getTask() != null;
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(1, 20, !hasWork(), true);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        ReactionChamberRecipe task = getTask();
        if (task == null) {
            processingTime = 0;
            return TickRateModulation.SLEEP;
        }

        int speed = 1 + upgrades.getInstalledUpgrades(Upgrades.SPEED);
        int steps = Math.max(1, ticksSinceLastCall) * speed;
        double energyPerStep = Math.max(1.0, task.getEnergy() / (double) PROCESSING_STEPS);
        double requested = energyPerStep * steps;

        double available = extractAEPower(requested, Actionable.SIMULATE, PowerMultiplier.CONFIG);
        IEnergySource source = this;
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
        if (processingTime >= PROCESSING_STEPS) {
            finish(task);
        }
        markForUpdate();
        return TickRateModulation.URGENT;
    }

    private void finish(ReactionChamberRecipe recipe) {
        if (!recipe.matchesMachine(inventory, inputTank.getFluid()) || !canAccept(recipe)) {
            processingTime = 0;
            cachedRecipe = null;
            return;
        }

        for (ReactionChamberRecipe.Input input : recipe.getInputs()) {
            int remaining = input.getAmount();
            for (int slot = 0; slot < INPUT_SLOTS && remaining > 0; slot++) {
                ItemStack stack = inventory.getStackInSlot(slot);
                if (stack.isEmpty() || !input.getIngredient().test(stack)) continue;
                int consume = Math.min(remaining, stack.getCount());
                inventory.extractItem(slot, consume, false);
                remaining -= consume;
            }
        }

        FluidStack fluidInput = recipe.getInputFluid();
        if (!fluidInput.isEmpty()) {
            inputTank.drain(fluidInput.getAmount(), IFluidHandler.FluidAction.EXECUTE);
        }

        ItemStack itemOutput = recipe.getOutputItem();
        if (!itemOutput.isEmpty()) {
            inventory.insertItem(OUTPUT_SLOT, itemOutput, false);
        }

        FluidStack fluidOutput = recipe.getOutputFluid();
        if (!fluidOutput.isEmpty()) {
            outputTank.fill(fluidOutput, IFluidHandler.FluidAction.EXECUTE);
        }

        processingTime = 0;
        cachedRecipe = null;
        saveChanges();
    }

    @Override
    public CompoundNBT write(CompoundNBT data) {
        super.write(data);
        inventory.writeToNBT(data, "inventory");
        upgrades.writeToNBT(data, "upgrades");
        data.putInt("processingTime", processingTime);
        data.put("inputTank", inputTank.writeToNBT(new CompoundNBT()));
        data.put("outputTank", outputTank.writeToNBT(new CompoundNBT()));
        return data;
    }

    @Override
    public void read(BlockState state, CompoundNBT data) {
        super.read(state, data);
        inventory.readFromNBT(data, "inventory");
        upgrades.readFromNBT(data, "upgrades");
        processingTime = data.getInt("processingTime");
        inputTank.readFromNBT(data.getCompound("inputTank"));
        outputTank.readFromNBT(data.getCompound("outputTank"));
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

    private final class ChamberFluidHandler implements IFluidHandler {
        @Override public int getTanks() { return 2; }
        @Nonnull @Override public FluidStack getFluidInTank(int tank) {
            return tank == 0 ? inputTank.getFluid().copy() : outputTank.getFluid().copy();
        }
        @Override public int getTankCapacity(int tank) { return TANK_CAPACITY; }
        @Override public boolean isFluidValid(int tank, @Nonnull FluidStack stack) { return tank == 0; }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return inputTank.fill(resource, action);
        }

        @Nonnull
        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return outputTank.drain(resource, action);
        }

        @Nonnull
        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return outputTank.drain(maxDrain, action);
        }
    }
}
