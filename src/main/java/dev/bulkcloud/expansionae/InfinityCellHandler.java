package dev.bulkcloud.expansionae;

import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.cells.CellState;
import appeng.api.storage.cells.ICellHandler;
import appeng.api.storage.cells.ICellInventoryHandler;
import appeng.api.storage.cells.ISaveProvider;
import appeng.api.storage.channels.IFluidStorageChannel;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import appeng.core.Api;
import appeng.me.storage.BasicCellInventoryHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.fluid.Fluids;
import net.minecraftforge.fluids.FluidStack;

/** Read-only, fixed-resource cells. Inserting an item never destroys it. */
public final class InfinityCellHandler implements ICellHandler {
    @Override public boolean isCell(ItemStack stack) {
        return stack.getItem() == ExpansionAE.WATER_CELL.get() || stack.getItem() == ExpansionAE.COBBLE_CELL.get();
    }
    @Override @SuppressWarnings({"unchecked", "rawtypes"})
    public <T extends IAEStack<T>> ICellInventoryHandler<T> getCellInventory(ItemStack stack,
            ISaveProvider host, IStorageChannel<T> channel) {
        if (!isCell(stack)) return null;
        IStorageChannel<?> expected;
        IAEStack<?> resource;
        if (stack.getItem() == ExpansionAE.WATER_CELL.get()) {
            IFluidStorageChannel fluids = Api.instance().storage().getStorageChannel(IFluidStorageChannel.class);
            expected = fluids;
            resource = fluids.createStack(new FluidStack(Fluids.WATER, 1000));
        } else {
            IItemStorageChannel items = Api.instance().storage().getStorageChannel(IItemStorageChannel.class);
            expected = items;
            resource = items.createStack(new ItemStack(Items.COBBLESTONE));
        }
        if (channel != expected || resource == null) return null;
        return new BasicCellInventoryHandler(new Inventory(channel, resource), channel);
    }
    @Override public <T extends IAEStack<T>> CellState getStatusForCell(ItemStack stack, ICellInventoryHandler<T> inv) {
        return CellState.TYPES_FULL;
    }
    @Override public <T extends IAEStack<T>> double cellIdleDrain(ItemStack stack, ICellInventoryHandler<T> inv) { return 0; }

    private static final class Inventory<T extends IAEStack<T>> implements IMEInventoryHandler<T> {
        private final IStorageChannel<T> channel;
        private final T prototype;
        Inventory(IStorageChannel<T> channel, T prototype) { this.channel = channel; this.prototype = prototype.copy(); }
        @Override public T injectItems(T input, Actionable mode, IActionSource source) { return input; }
        @Override public T extractItems(T request, Actionable mode, IActionSource source) {
            return request != null && request.getStackSize() > 0 && prototype.isSameType(request) ? request.copy() : null;
        }
        @Override public IItemList<T> getAvailableItems(IItemList<T> out) {
            out.add(prototype.copy().setStackSize(Integer.MAX_VALUE));
            return out;
        }
        @Override public IStorageChannel<T> getChannel() { return channel; }
        @Override public AccessRestriction getAccess() { return AccessRestriction.READ; }
        @Override public boolean isPrioritized(T input) { return false; }
        @Override public boolean canAccept(T input) { return false; }
        @Override public int getPriority() { return 0; }
        @Override public int getSlot() { return 0; }
        @Override public boolean validForPass(int pass) { return true; }
    }
}
