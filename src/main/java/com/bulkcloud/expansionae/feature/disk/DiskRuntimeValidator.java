package com.bulkcloud.expansionae.feature.disk;

import java.util.UUID;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import com.bulkcloud.expansionae.ExpansionAE;
import com.bulkcloud.expansionae.ae2.ExpansionAEApi;
import com.bulkcloud.expansionae.core.registry.ExpansionAEItems;

import appeng.api.config.Actionable;
import appeng.api.storage.cells.ICellInventoryHandler;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;

public final class DiskRuntimeValidator {
    private DiskRuntimeValidator() {
    }

    public static void validate() {
        DiskStorageData storage = DiskStorageService.getCurrent();
        if (storage == null) {
            throw new IllegalStateException("DISK runtime validation requires loaded overworld storage");
        }

        IItemStorageChannel channel =
                ExpansionAEApi.get().storage().getStorageChannel(IItemStorageChannel.class);

        ItemStack primaryStack = new ItemStack(ExpansionAEItems.DISK_1K.get());
        ICellInventoryHandler<IAEItemStack> primary =
                ExpansionAEApi.get().registries().cell().getCellInventory(primaryStack, null, channel);
        if (primary == null || primary.getCellInv() == null) {
            throw new IllegalStateException("AE2 did not provide a DISK cell inventory handler");
        }

        IAEItemStack stone = channel.createStack(new ItemStack(Items.STONE));
        if (stone == null) {
            throw new IllegalStateException("AE2 item channel could not create a stone stack");
        }

        stone.setStackSize(600);
        IAEItemStack firstRemainder = primary.injectItems(stone, Actionable.MODULATE, null);
        if (firstRemainder != null) {
            throw new IllegalStateException("1k DISK rejected part of the initial 600 item insertion");
        }
        requireStoredCount(primary, 600);

        if (!primaryStack.hasTag() || !primaryStack.getTag().hasUniqueId(DiskCellInventory.TAG_UUID)) {
            throw new IllegalStateException("1k DISK did not assign a backing UUID after first insertion");
        }
        UUID uuid = primaryStack.getTag().getUniqueId(DiskCellInventory.TAG_UUID);

        ItemStack aliasStack = primaryStack.copy();
        ICellInventoryHandler<IAEItemStack> alias =
                ExpansionAEApi.get().registries().cell().getCellInventory(aliasStack, null, channel);
        if (alias == null || alias.getCellInv() == null) {
            throw new IllegalStateException("AE2 did not provide a handler for a DISK alias");
        }
        requireStoredCount(alias, 600);

        IAEItemStack additional = channel.createStack(new ItemStack(Items.STONE));
        if (additional == null) {
            throw new IllegalStateException("AE2 item channel could not create the capacity test stack");
        }
        additional.setStackSize(500);

        IAEItemStack capacityRemainder = alias.injectItems(additional, Actionable.MODULATE, null);
        if (capacityRemainder == null || capacityRemainder.getStackSize() != 100) {
            throw new IllegalStateException("1k DISK capacity contract expected a remainder of 100 from 500");
        }

        requireStoredCount(alias, 1000);
        requireStoredCount(primary, 1000);

        IAEItemStack request250 = channel.createStack(new ItemStack(Items.STONE));
        if (request250 == null) {
            throw new IllegalStateException("AE2 item channel could not create the first extraction request");
        }
        request250.setStackSize(250);

        IAEItemStack extracted250 = alias.extractItems(request250, Actionable.MODULATE, null);
        if (extracted250 == null || extracted250.getStackSize() != 250) {
            throw new IllegalStateException("DISK alias extraction did not return 250 items");
        }
        requireStoredCount(primary, 750);

        IAEItemStack request750 = channel.createStack(new ItemStack(Items.STONE));
        if (request750 == null) {
            throw new IllegalStateException("AE2 item channel could not create the final extraction request");
        }
        request750.setStackSize(750);

        IAEItemStack extracted750 = primary.extractItems(request750, Actionable.MODULATE, null);
        if (extracted750 == null || extracted750.getStackSize() != 750) {
            throw new IllegalStateException("DISK primary extraction did not return the remaining 750 items");
        }

        requireStoredCount(alias, 0);

        DiskStorageData.DiskRecord emptyRecord = storage.get(uuid);
        if (emptyRecord == null || emptyRecord.getItemCount() != 0) {
            throw new IllegalStateException("Empty DISK backing record was not preserved for UUID aliases");
        }

        storage.remove(uuid);

        ExpansionAE.LOGGER.info(
                "DISK storage runtime validated (capacity, insert/extract, UUID alias sync, empty backing record)");
    }

    private static void requireStoredCount(
            ICellInventoryHandler<IAEItemStack> handler,
            long expected) {
        long actual = handler.getCellInv().getStoredItemCount();
        if (actual != expected) {
            throw new IllegalStateException(
                    "DISK stored item count mismatch: expected " + expected + " but got " + actual);
        }
    }
}
