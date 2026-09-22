package com.bulkcloud.expansionae.feature.disk;

import javax.annotation.Nullable;

import com.bulkcloud.expansionae.ae2.AE2Bridge;

import appeng.api.storage.IStorageChannel;
import appeng.api.storage.cells.CellState;
import appeng.api.storage.cells.ICellHandler;
import appeng.api.storage.cells.ICellInventoryHandler;
import appeng.api.storage.cells.ISaveProvider;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEStack;
import net.minecraft.item.ItemStack;

public final class DiskCellHandler implements ICellHandler {

    public static final DiskCellHandler INSTANCE = new DiskCellHandler();

    private DiskCellHandler() {
    }

    @Override
    public boolean isCell(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof DiskItem;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    @Override
    public <T extends IAEStack<T>> ICellInventoryHandler<T> getCellInventory(
            ItemStack stack,
            @Nullable ISaveProvider host,
            IStorageChannel<T> requestedChannel) {

        if (!this.isCell(stack)) {
            return null;
        }

        IItemStorageChannel itemChannel =
                AE2Bridge.api().storage().getStorageChannel(IItemStorageChannel.class);

        if (requestedChannel != itemChannel) {
            return null;
        }

        return (ICellInventoryHandler<T>) new DiskCellInventoryHandler(stack, host, itemChannel);
    }

    @Override
    public <T extends IAEStack<T>> CellState getStatusForCell(
            ItemStack stack,
            ICellInventoryHandler<T> handler) {

        if (!this.isCell(stack)) {
            return CellState.ABSENT;
        }

        long stored = DiskItem.getStoredCount(stack);
        if (stored <= 0L) {
            return CellState.EMPTY;
        }

        long capacity = ((DiskItem) stack.getItem()).getCapacity();
        return stored >= capacity ? CellState.FULL : CellState.NOT_EMPTY;
    }

    @Override
    public <T extends IAEStack<T>> double cellIdleDrain(
            ItemStack stack,
            ICellInventoryHandler<T> handler) {

        if (!this.isCell(stack)) {
            return 0.0D;
        }

        return ((DiskItem) stack.getItem()).getIdleDrain();
    }
}
