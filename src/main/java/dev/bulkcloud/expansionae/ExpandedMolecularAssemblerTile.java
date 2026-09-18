package dev.bulkcloud.expansionae;

import java.util.EnumSet;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.hooks.BasicEventHooks;
import net.minecraftforge.items.IItemHandler;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.config.Upgrades;
import appeng.api.implementations.IPowerChannelState;
import appeng.api.implementations.tiles.ICraftingMachine;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.events.MENetworkEventSubscribe;
import appeng.api.networking.events.MENetworkPowerStatusChange;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.util.AECableType;
import appeng.api.util.AEPartLocation;
import appeng.core.Api;
import appeng.me.GridAccessException;
import appeng.parts.automation.UpgradeInventory;
import appeng.tile.grid.AENetworkInvTileEntity;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.util.InventoryAdaptor;
import appeng.util.Platform;
import appeng.util.helpers.ItemHandlerUtil;
import appeng.util.inv.InvOperation;
import appeng.util.inv.WrapperChainedItemHandler;

/**
 * Eight-lane Extended Molecular Assembler backport.
 *
 * Every lane owns a 3x3 crafting grid and one output slot. Jobs pushed by the
 * AE2 crafting service are assigned to the first free lane and progress in
 * parallel while sharing the machine's speed cards and network energy.
 */
public final class ExpandedMolecularAssemblerTile extends AENetworkInvTileEntity
        implements IGridTickable, ICraftingMachine, IPowerChannelState {
    public static final int LANES = 8;
    private static final int SLOTS_PER_LANE = 10;

    private final Lane[] lanes = new Lane[LANES];
    private final IItemHandler combinedInventory;
    private final UpgradeInventory upgrades;
    private boolean powered;

    public ExpandedMolecularAssemblerTile(TileEntityType<?> type) {
        super(type);
        getProxy().setIdlePowerUsage(0.0);
        getProxy().setValidSides(EnumSet.allOf(Direction.class));

        IItemHandler[] handlers = new IItemHandler[LANES];
        for (int i = 0; i < LANES; i++) {
            lanes[i] = new Lane(i);
            handlers[i] = lanes[i].inventory;
        }
        combinedInventory = new WrapperChainedItemHandler(handlers);
        upgrades = new UpgradeInventory(this, 5) {
            @Override
            public int getMaxInstalled(Upgrades upgrade) {
                return upgrade == Upgrades.SPEED ? 5 : 0;
            }
        };
    }

    public IItemHandler getLaneInventory(int lane) {
        return lanes[Math.max(0, Math.min(LANES - 1, lane))].inventory;
    }

    public int getCraftingProgress(int lane) {
        return (int) lanes[Math.max(0, Math.min(LANES - 1, lane))].progress;
    }

    @Override
    public boolean pushPattern(ICraftingPatternDetails pattern, CraftingInventory table, Direction direction) {
        if (pattern == null || !pattern.isCraftable()) {
            return false;
        }
        for (Lane lane : lanes) {
            if (lane.accept(pattern, table, direction)) {
                wake();
                saveChanges();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean acceptsPlans() {
        for (Lane lane : lanes) {
            if (lane.isFree()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public AECableType getCableConnectionType(AEPartLocation dir) {
        return AECableType.COVERED;
    }

    @Override
    public CompoundNBT write(CompoundNBT data) {
        super.write(data);
        upgrades.writeToNBT(data, "upgrades");
        for (int i = 0; i < LANES; i++) {
            data.put("lane" + i, lanes[i].write());
        }
        return data;
    }

    @Override
    public void read(BlockState state, CompoundNBT data) {
        super.read(state, data);
        upgrades.readFromNBT(data, "upgrades");
        for (int i = 0; i < LANES; i++) {
            if (data.contains("lane" + i)) {
                lanes[i].read(data.getCompound("lane" + i));
            }
        }
    }

    @Override
    public IItemHandler getInternalInventory() {
        return combinedInventory;
    }

    @Override
    public IItemHandler getInventoryByName(String name) {
        if ("molecular_assembler".equals(name)) {
            return combinedInventory;
        }
        if ("upgrades".equals(name)) {
            return upgrades;
        }
        return null;
    }

    @Override
    protected IItemHandler getItemHandlerForSide(Direction side) {
        return combinedInventory;
    }

    @Override
    public void onChangeInventory(IItemHandler inventory, int slot, InvOperation operation,
            ItemStack removed, ItemStack added) {
        wake();
    }

    @Override
    public void getDrops(World world, BlockPos pos, List<ItemStack> drops) {
        super.getDrops(world, pos, drops);
        for (ItemStack stack : upgrades) {
            if (!stack.isEmpty()) {
                drops.add(stack);
            }
        }
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(1, 1, !hasWork(), false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        boolean work = false;
        for (Lane lane : lanes) {
            work |= lane.tick(Math.max(1, ticksSinceLastCall));
        }
        updatePowerState();
        return work ? TickRateModulation.FASTER : TickRateModulation.SLEEP;
    }

    private boolean hasWork() {
        for (Lane lane : lanes) {
            if (!lane.isFree()) {
                return true;
            }
        }
        return false;
    }

    private void wake() {
        try {
            getProxy().getTick().wakeDevice(getProxy().getNode());
        } catch (GridAccessException ignored) {
        }
    }

    private int speedBonus() {
        switch (upgrades.getInstalledUpgrades(Upgrades.SPEED)) {
            case 1: return 13;
            case 2: return 17;
            case 3: return 20;
            case 4: return 25;
            case 5: return 50;
            default: return 10;
        }
    }

    private double speedTax() {
        switch (upgrades.getInstalledUpgrades(Upgrades.SPEED)) {
            case 1: return 1.3;
            case 2: return 1.7;
            case 3: return 2.0;
            case 4: return 2.5;
            case 5: return 5.0;
            default: return 1.0;
        }
    }

    private int usePower(int ticks) {
        double tax = speedTax();
        int bonus = speedBonus();
        try {
            return (int) (getProxy().getEnergy().extractAEPower(
                    ticks * bonus * tax, Actionable.MODULATE, PowerMultiplier.CONFIG) / tax);
        } catch (GridAccessException e) {
            return 0;
        }
    }

    @MENetworkEventSubscribe
    public void onPowerEvent(MENetworkPowerStatusChange event) {
        updatePowerState();
    }

    private void updatePowerState() {
        boolean next = false;
        try {
            next = getProxy().isActive()
                    && getProxy().getEnergy().extractAEPower(
                            1, Actionable.SIMULATE, PowerMultiplier.CONFIG) > 0.0001;
        } catch (GridAccessException ignored) {
        }
        if (next != powered) {
            powered = next;
            markForUpdate();
        }
    }

    @Override public boolean isPowered() { return powered; }
    @Override public boolean isActive() { return powered; }

    @Override
    public int getInstalledUpgrades(Upgrades upgrade) {
        return upgrades.getInstalledUpgrades(upgrade);
    }

    private final class Lane {
        private final int index;
        private final AppEngInternalInventory inventory =
                new AppEngInternalInventory(ExpandedMolecularAssemblerTile.this, SLOTS_PER_LANE);
        private final CraftingInventory crafting = new CraftingInventory(new Container(null, -1) {
            @Override public boolean canInteractWith(net.minecraft.entity.player.PlayerEntity player) { return false; }
        }, 3, 3);
        private ICraftingPatternDetails plan;
        private AEPartLocation outputDirection = AEPartLocation.INTERNAL;
        private double progress;

        Lane(int index) {
            this.index = index;
            inventory.setMaxStackSize(9, 64);
        }

        boolean isFree() {
            return plan == null && ItemHandlerUtil.isEmpty(inventory);
        }

        boolean accept(ICraftingPatternDetails pattern, CraftingInventory table, Direction direction) {
            if (!isFree()) {
                return false;
            }
            for (int slot = 0; slot < 9; slot++) {
                ItemStack stack = table.getStackInSlot(slot);
                inventory.setStackInSlot(slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
            }
            plan = pattern;
            outputDirection = direction == null ? AEPartLocation.INTERNAL : AEPartLocation.fromFacing(direction);
            progress = 0;
            return true;
        }

        boolean tick(int ticks) {
            if (plan == null) {
                return !inventory.getStackInSlot(9).isEmpty() && pushOutput();
            }

            if (!inventory.getStackInSlot(9).isEmpty()) {
                return pushOutput();
            }

            for (int slot = 0; slot < 9; slot++) {
                crafting.setInventorySlotContents(slot, inventory.getStackInSlot(slot));
            }
            ItemStack result = plan.getOutput(crafting, getWorld());
            if (result.isEmpty()) {
                clearInvalidInputs();
                return false;
            }

            progress += usePower(ticks);
            if (progress < 100) {
                return true;
            }

            progress = 0;
            ItemStack output = result.copy();
            if (!inventory.insertItem(9, output, true).isEmpty()) {
                return true;
            }

            if (getWorld() instanceof ServerWorld) {
                BasicEventHooks.firePlayerCraftingEvent(
                        Platform.getPlayer((ServerWorld) getWorld()), output, crafting);
            }

            inventory.setStackInSlot(9, output);
            for (int slot = 0; slot < 9; slot++) {
                inventory.setStackInSlot(slot,
                        Platform.getContainerItem(crafting.getStackInSlot(slot)));
            }
            saveChanges();
            return pushOutput();
        }

        private void clearInvalidInputs() {
            if (plan == null) return;
            for (int slot = 0; slot < 9; slot++) {
                ItemStack stack = inventory.getStackInSlot(slot);
                if (!stack.isEmpty() && !plan.isValidItemForSlot(slot, stack, getWorld())) {
                    ItemStack remainder = pushTo(stack.copy(), outputDirection);
                    inventory.setStackInSlot(slot, remainder);
                }
            }
        }

        boolean pushOutput() {
            ItemStack output = inventory.getStackInSlot(9);
            if (output.isEmpty()) {
                if (allInputsEmpty()) {
                    plan = null;
                    outputDirection = AEPartLocation.INTERNAL;
                }
                return false;
            }
            ItemStack remaining = pushTo(output.copy(), outputDirection);
            inventory.setStackInSlot(9, remaining);
            if (remaining.isEmpty() && allInputsEmpty()) {
                plan = null;
                outputDirection = AEPartLocation.INTERNAL;
                saveChanges();
            }
            return remaining.getCount() != output.getCount();
        }

        private boolean allInputsEmpty() {
            for (int i = 0; i < 9; i++) {
                if (!inventory.getStackInSlot(i).isEmpty()) return false;
            }
            return true;
        }

        private ItemStack pushTo(ItemStack stack, AEPartLocation direction) {
            if (stack.isEmpty()) return stack;
            if (direction == AEPartLocation.INTERNAL) {
                for (Direction side : Direction.values()) {
                    stack = pushSide(stack, side);
                    if (stack.isEmpty()) break;
                }
                return stack;
            }
            return pushSide(stack, direction.getFacing());
        }

        private ItemStack pushSide(ItemStack stack, Direction side) {
            if (stack.isEmpty()) return stack;
            TileEntity target = getWorld().getTileEntity(getPos().offset(side));
            if (target == null) return stack;
            InventoryAdaptor adaptor = InventoryAdaptor.getAdaptor(target, side.getOpposite());
            return adaptor == null ? stack : adaptor.addItems(stack);
        }

        CompoundNBT write() {
            CompoundNBT tag = new CompoundNBT();
            inventory.writeToNBT(tag, "inventory");
            tag.putDouble("progress", progress);
            tag.putInt("direction", outputDirection.ordinal());
            if (plan != null) {
                ItemStack pattern = plan.getPattern();
                if (!pattern.isEmpty()) {
                    tag.put("pattern", pattern.write(new CompoundNBT()));
                }
            }
            return tag;
        }

        void read(CompoundNBT tag) {
            inventory.readFromNBT(tag, "inventory");
            progress = tag.getDouble("progress");
            outputDirection = AEPartLocation.fromOrdinal(tag.getInt("direction"));
            plan = null;
            if (tag.contains("pattern") && getWorld() != null) {
                ItemStack pattern = ItemStack.read(tag.getCompound("pattern"));
                plan = Api.instance().crafting().decodePattern(pattern, getWorld());
                if (plan != null && !plan.isCraftable()) {
                    plan = null;
                }
            }
        }
    }
}
