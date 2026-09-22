package com.bulkcloud.expansionae.feature.disk;

import net.minecraft.item.ItemStack;

import appeng.api.storage.IStorageChannel;
import appeng.api.storage.cells.ICellHandler;
import appeng.api.storage.cells.ICellInventoryHandler;
import appeng.api.storage.cells.ISaveProvider;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.core.Api;
import appeng.me.storage.BasicCellInventoryHandler;

public final class DiskCellHandler implements ICellHandler {
    public static final DiskCellHandler INSTANCE = new DiskCellHandler();

    private DiskCellHandler() {
    }

    @Override
    public boolean isCell(ItemStack stack) {
        return stack.getItem() instanceof DiskStorageCellItem;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public <T extends IAEStack<T>> ICellInventoryHandler<T> getCellInventory(
            ItemStack stack,
            ISaveProvider host,
            IStorageChannel<T> requestedChannel) {
        if (!(stack.getItem() instanceof DiskStorageCellItem)) {
            return null;
        }

        IItemStorageChannel itemChannel =
                Api.instance().storage().getStorageChannel(IItemStorageChannel.class);

        if (requestedChannel != itemChannel) {
            return null;
        }

        DiskCellInventory inventory =
                new DiskCellInventory((DiskStorageCellItem) stack.getItem(), stack, host);

        return (ICellInventoryHandler<T>) new BasicCellInventoryHandler<IAEItemStack>(
                inventory, itemChannel);
    }
}
