package com.bulkcloud.expansionae.feature.disk;

import java.util.UUID;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.ListNBT;

import com.bulkcloud.expansionae.ExpansionAE;
import com.bulkcloud.expansionae.ae2.ExpansionAEApi;
import com.bulkcloud.expansionae.core.registry.ExpansionAEItems;

import appeng.api.config.Actionable;
import appeng.api.storage.cells.ICellInventoryHandler;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;

public final class DiskRuntimeValidator {
    private static final String PERSISTENCE_PHASE_ENV = "EXPANSIONAE_PERSISTENCE_PHASE";
    private static final UUID PERSISTENCE_TEST_UUID =
            UUID.fromString("0d15c000-0000-4000-8000-000000000165");
    private static final long PERSISTENCE_TEST_AMOUNT = 321L;

    private DiskRuntimeValidator() {
    }

    public static void validate() {
        DiskStorageData storage = DiskStorageService.getCurrent();
        if (storage == null) {
            throw new IllegalStateException("DISK runtime validation requires loaded overworld storage");
        }

        IItemStorageChannel channel =
                ExpansionAEApi.get().storage().getStorageChannel(IItemStorageChannel.class);

        validateTransientStorage(storage, channel);
        validatePersistencePhase(storage, channel);
    }

    private static void validateTransientStorage(
            DiskStorageData storage,
            IItemStorageChannel channel) {
        ItemStack primaryStack = new ItemStack(ExpansionAEItems.DISK_1K.get());
        ICellInventoryHandler<IAEItemStack> primary = open(primaryStack, channel, "primary");

        IAEItemStack stone = stone(channel, 600);
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
        ICellInventoryHandler<IAEItemStack> alias = open(aliasStack, channel, "alias");
        requireStoredCount(alias, 600);

        IAEItemStack additional = stone(channel, 500);
        IAEItemStack capacityRemainder = alias.injectItems(additional, Actionable.MODULATE, null);
        if (capacityRemainder == null || capacityRemainder.getStackSize() != 100) {
            throw new IllegalStateException("1k DISK capacity contract expected a remainder of 100 from 500");
        }

        requireStoredCount(alias, 1000);
        requireStoredCount(primary, 1000);

        IAEItemStack request250 = stone(channel, 250);
        IAEItemStack extracted250 = alias.extractItems(request250, Actionable.MODULATE, null);
        if (extracted250 == null || extracted250.getStackSize() != 250) {
            throw new IllegalStateException("DISK alias extraction did not return 250 items");
        }
        requireStoredCount(primary, 750);

        IAEItemStack request750 = stone(channel, 750);
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

    private static void validatePersistencePhase(
            DiskStorageData storage,
            IItemStorageChannel channel) {
        String phase = System.getenv(PERSISTENCE_PHASE_ENV);
        if (phase == null || phase.isEmpty()) {
            return;
        }

        if ("write".equals(phase)) {
            validatePersistenceWrite(storage, channel);
            return;
        }

        if ("read".equals(phase)) {
            validatePersistenceRead(storage, channel);
            return;
        }

        throw new IllegalStateException(
                "Unknown " + PERSISTENCE_PHASE_ENV + " value: " + phase);
    }

    private static void validatePersistenceWrite(
            DiskStorageData storage,
            IItemStorageChannel channel) {
        storage.remove(PERSISTENCE_TEST_UUID);
        storage.put(PERSISTENCE_TEST_UUID, new ListNBT(), new long[0], 0);

        ItemStack stack = stackForUuid(PERSISTENCE_TEST_UUID);
        ICellInventoryHandler<IAEItemStack> handler = open(stack, channel, "persistence writer");

        IAEItemStack remainder =
                handler.injectItems(stone(channel, PERSISTENCE_TEST_AMOUNT), Actionable.MODULATE, null);
        if (remainder != null) {
            throw new IllegalStateException("Persistence write phase rejected part of the test payload");
        }

        requireStoredCount(handler, PERSISTENCE_TEST_AMOUNT);

        DiskStorageData.DiskRecord record = storage.get(PERSISTENCE_TEST_UUID);
        if (record == null || record.getItemCount() != PERSISTENCE_TEST_AMOUNT) {
            throw new IllegalStateException("Persistence write phase did not update the backing record");
        }

        ExpansionAE.LOGGER.info(
                "DISK persistence write phase validated ({} items staged for restart)",
                PERSISTENCE_TEST_AMOUNT);
    }

    private static void validatePersistenceRead(
            DiskStorageData storage,
            IItemStorageChannel channel) {
        DiskStorageData.DiskRecord record = storage.get(PERSISTENCE_TEST_UUID);
        if (record == null || record.getItemCount() != PERSISTENCE_TEST_AMOUNT) {
            throw new IllegalStateException(
                    "Persistence read phase did not recover the expected backing record after restart");
        }

        ItemStack stack = stackForUuid(PERSISTENCE_TEST_UUID);
        ICellInventoryHandler<IAEItemStack> handler = open(stack, channel, "persistence reader");
        requireStoredCount(handler, PERSISTENCE_TEST_AMOUNT);

        IAEItemStack extracted =
                handler.extractItems(stone(channel, PERSISTENCE_TEST_AMOUNT), Actionable.MODULATE, null);
        if (extracted == null || extracted.getStackSize() != PERSISTENCE_TEST_AMOUNT) {
            throw new IllegalStateException("Persistence read phase could not extract the recovered payload");
        }

        requireStoredCount(handler, 0);

        DiskStorageData.DiskRecord emptyRecord = storage.get(PERSISTENCE_TEST_UUID);
        if (emptyRecord == null || emptyRecord.getItemCount() != 0) {
            throw new IllegalStateException(
                    "Persistence read phase did not preserve the empty backing record after extraction");
        }

        storage.remove(PERSISTENCE_TEST_UUID);

        ExpansionAE.LOGGER.info(
                "DISK persistence read phase validated (backing data survived server restart)");
    }

    private static ItemStack stackForUuid(UUID uuid) {
        ItemStack stack = new ItemStack(ExpansionAEItems.DISK_1K.get());
        stack.getOrCreateTag().putUniqueId(DiskCellInventory.TAG_UUID, uuid);
        return stack;
    }

    private static ICellInventoryHandler<IAEItemStack> open(
            ItemStack stack,
            IItemStorageChannel channel,
            String label) {
        ICellInventoryHandler<IAEItemStack> handler =
                ExpansionAEApi.get().registries().cell().getCellInventory(stack, null, channel);
        if (handler == null || handler.getCellInv() == null) {
            throw new IllegalStateException("AE2 did not provide a DISK cell inventory handler for " + label);
        }
        return handler;
    }

    private static IAEItemStack stone(IItemStorageChannel channel, long amount) {
        IAEItemStack stack = channel.createStack(new ItemStack(Items.STONE));
        if (stack == null) {
            throw new IllegalStateException("AE2 item channel could not create a stone stack");
        }
        stack.setStackSize(amount);
        return stack;
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
