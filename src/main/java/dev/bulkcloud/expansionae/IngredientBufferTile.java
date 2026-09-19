package dev.bulkcloud.expansionae;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import appeng.fluids.util.AEFluidInventory;
import appeng.fluids.util.IAEFluidInventory;
import appeng.fluids.util.IAEFluidTank;
import appeng.tile.AEBaseInvTileEntity;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.util.inv.InvOperation;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

/**
 * 1.16.5 adaptation of ExtendedAE's 36-type GenericStack ingredient buffer.
 * Each logical slot can contain either an item stack or up to 64 buckets of one fluid.
 */
public final class IngredientBufferTile extends AEBaseInvTileEntity implements IAEFluidInventory {
    public static final int SLOTS = 36;
    public static final int FLUID_CAPACITY = 64000;

    private final AppEngInternalInventory items = new AppEngInternalInventory(this, SLOTS);
    private final AEFluidInventory fluids = new AEFluidInventory(this, SLOTS, FLUID_CAPACITY);
    private final IItemHandler sharedItems = new SharedItemHandler();
    private final IFluidHandler sharedFluids = new SharedFluidHandler();
    private final LazyOptional<IItemHandler> itemCapability = LazyOptional.of(() -> sharedItems);
    private final LazyOptional<IFluidHandler> fluidCapability = LazyOptional.of(() -> sharedFluids);

    public IngredientBufferTile(TileEntityType<?> type) {
        super(type);
    }

    public IItemHandler getBufferInventory() { return sharedItems; }
    public IFluidHandler getFluidInventory() { return sharedFluids; }

    @Nonnull
    @Override
    public IItemHandler getInternalInventory() { return items; }

    @Override
    protected @Nonnull IItemHandler getItemHandlerForSide(@Nonnull Direction side) { return sharedItems; }

    @Override
    public void onChangeInventory(IItemHandler inv, int slot, InvOperation operation,
            ItemStack removed, ItemStack added) {
        saveChanges();
        markForUpdate();
    }

    @Override
    public void onFluidInventoryChanged(IAEFluidTank inv, int slot) {
        saveChanges();
        markForUpdate();
    }

    @Override
    public boolean isRemote() { return world != null && world.isRemote(); }

    @Override
    public CompoundNBT write(CompoundNBT data) {
        super.write(data);
        fluids.writeToNBT(data, "fluids");
        return data;
    }

    @Override
    public void read(BlockState state, CompoundNBT data) {
        super.read(state, data);
        fluids.readFromNBT(data, "fluids");
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return itemCapability.cast();
        if (cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) return fluidCapability.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void remove() {
        super.remove();
        itemCapability.invalidate();
        fluidCapability.invalidate();
    }

    private boolean fluidSlotEmpty(int slot) { return fluids.getFluidInTank(slot).isEmpty(); }
    private boolean itemSlotEmpty(int slot) { return items.getStackInSlot(slot).isEmpty(); }

    private final class SharedItemHandler implements IItemHandler {
        @Override public int getSlots() { return SLOTS; }
        @Override public ItemStack getStackInSlot(int slot) { return items.getStackInSlot(slot); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return fluidSlotEmpty(slot) ? items.insertItem(slot, stack, simulate) : stack;
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return items.extractItem(slot, amount, simulate);
        }
        @Override public int getSlotLimit(int slot) { return items.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return fluidSlotEmpty(slot); }
    }

    private final class SharedFluidHandler implements IFluidHandler {
        @Override public int getTanks() { return SLOTS; }
        @Nonnull @Override public FluidStack getFluidInTank(int tank) { return fluids.getFluidInTank(tank); }
        @Override public int getTankCapacity(int tank) { return FLUID_CAPACITY; }
        @Override public boolean isFluidValid(int tank, @Nonnull FluidStack stack) {
            return itemSlotEmpty(tank) && fluids.isFluidValid(tank, stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) return 0;
            for (int i = 0; i < SLOTS; i++) {
                FluidStack present = fluids.getFluidInTank(i);
                if (itemSlotEmpty(i) && !present.isEmpty() && present.isFluidEqual(resource)) {
                    return fluids.fill(i, resource, action == FluidAction.EXECUTE);
                }
            }
            for (int i = 0; i < SLOTS; i++) {
                if (itemSlotEmpty(i) && fluidSlotEmpty(i)) {
                    return fluids.fill(i, resource, action == FluidAction.EXECUTE);
                }
            }
            return 0;
        }

        @Nonnull
        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) return FluidStack.EMPTY;
            FluidStack result = FluidStack.EMPTY;
            int remaining = resource.getAmount();
            for (int i = 0; i < SLOTS && remaining > 0; i++) {
                FluidStack wanted = resource.copy();
                wanted.setAmount(remaining);
                FluidStack drained = fluids.drain(i, wanted, action == FluidAction.EXECUTE);
                if (drained.isEmpty()) continue;
                if (result.isEmpty()) result = drained.copy();
                else result.setAmount(result.getAmount() + drained.getAmount());
                remaining -= drained.getAmount();
            }
            return result;
        }

        @Nonnull
        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0) return FluidStack.EMPTY;
            for (int i = 0; i < SLOTS; i++) {
                if (!fluidSlotEmpty(i)) return fluids.drain(i, maxDrain, action == FluidAction.EXECUTE);
            }
            return FluidStack.EMPTY;
        }
    }
}
