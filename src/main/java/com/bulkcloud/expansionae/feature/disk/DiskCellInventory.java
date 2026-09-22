package com.bulkcloud.expansionae.feature.disk;

import java.util.UUID;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraftforge.items.IItemHandler;

import com.bulkcloud.expansionae.ExpansionAE;
import com.bulkcloud.expansionae.ae2.ExpansionAEApi;

import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.cells.CellState;
import appeng.api.storage.cells.ICellInventory;
import appeng.api.storage.cells.ICellInventoryHandler;
import appeng.api.storage.cells.ISaveProvider;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;

public final class DiskCellInventory implements ICellInventory<IAEItemStack> {
    static final String TAG_UUID = "expansionae_disk_uuid";
    static final String TAG_ITEM_COUNT = "expansionae_disk_item_count";
    static final String TAG_TYPE_COUNT = "expansionae_disk_type_count";

    private static final long NO_RECORD_REVISION = Long.MIN_VALUE;

    private final DiskStorageCellItem cellType;
    private final ItemStack cellStack;
    private final ISaveProvider saveProvider;
    private final IItemStorageChannel channel;

    private IItemList<IAEItemStack> contents;
    private UUID loadedUuid;
    private long loadedRevision = NO_RECORD_REVISION;
    private boolean dirty;
    private boolean missingRecordWarningLogged;

    public DiskCellInventory(DiskStorageCellItem cellType, ItemStack cellStack, ISaveProvider saveProvider) {
        this.cellType = cellType;
        this.cellStack = cellStack;
        this.saveProvider = saveProvider;
        this.channel = ExpansionAEApi.get().storage().getStorageChannel(IItemStorageChannel.class);
    }

    private IItemList<IAEItemStack> contents() {
        UUID uuid = getUuid();

        if (uuid == null) {
            if (contents == null || loadedUuid != null) {
                contents = channel.createList();
                loadedUuid = null;
                loadedRevision = NO_RECORD_REVISION;
            }
            return contents;
        }

        DiskStorageData storage = DiskStorageService.getCurrent();
        if (storage == null) {
            // Never cache a transient empty server state.
            return channel.createList();
        }

        DiskStorageData.DiskRecord record = storage.get(uuid);
        long revision = record == null ? NO_RECORD_REVISION : record.getRevision();

        if (contents != null && uuid.equals(loadedUuid) && loadedRevision == revision) {
            return contents;
        }

        IItemList<IAEItemStack> loaded = channel.createList();
        if (record != null) {
            ListNBT keys = record.getKeys();
            long[] amounts = record.getAmounts();
            int count = Math.min(keys.size(), amounts.length);

            for (int i = 0; i < count; i++) {
                IAEItemStack stack = channel.createFromNBT(keys.getCompound(i));
                if (stack != null && amounts[i] > 0) {
                    stack.setStackSize(amounts[i]);
                    loaded.add(stack);
                }
            }
        }

        contents = loaded;
        loadedUuid = uuid;
        loadedRevision = revision;
        return contents;
    }

    @Override
    public IAEItemStack injectItems(IAEItemStack input, Actionable mode, IActionSource source) {
        if (input == null || input.getStackSize() <= 0) {
            return null;
        }

        if (DiskStorageService.getCurrent() == null || hasMissingBackingRecord()) {
            // Fail closed when the authoritative server-side backing store is unavailable.
            return input;
        }

        if (isNonEmptyStorageCell(input)) {
            return input;
        }

        long accepted = Math.min(input.getStackSize(), getRemainingItemCount());
        if (accepted <= 0) {
            return input;
        }

        if (mode == Actionable.MODULATE) {
            IAEItemStack existing = contents().findPrecise(input);
            if (existing == null) {
                IAEItemStack inserted = input.copy();
                inserted.setStackSize(accepted);
                contents().add(inserted);
            } else {
                existing.incStackSize(accepted);
            }
            changed();
        }

        if (accepted == input.getStackSize()) {
            return null;
        }

        IAEItemStack remainder = input.copy();
        remainder.setStackSize(input.getStackSize() - accepted);
        return remainder;
    }

    private boolean isNonEmptyStorageCell(IAEItemStack input) {
        ItemStack nestedStack = input.createItemStack();
        if (!ExpansionAEApi.get().registries().cell().isCellHandled(nestedStack)) {
            return false;
        }

        ICellInventoryHandler<IAEItemStack> nested =
                ExpansionAEApi.get().registries().cell().getCellInventory(nestedStack, null, channel);
        if (nested == null) {
            return false;
        }

        return !nested.getAvailableItems(channel.createList()).isEmpty();
    }

    @Override
    public IAEItemStack extractItems(IAEItemStack request, Actionable mode, IActionSource source) {
        if (request == null || request.getStackSize() <= 0) {
            return null;
        }

        if (DiskStorageService.getCurrent() == null || hasMissingBackingRecord()) {
            return null;
        }

        IAEItemStack existing = contents().findPrecise(request);
        if (existing == null || existing.getStackSize() <= 0) {
            return null;
        }

        long amount = Math.min(request.getStackSize(), existing.getStackSize());
        IAEItemStack result = existing.copy();
        result.setStackSize(amount);

        if (mode == Actionable.MODULATE) {
            existing.decStackSize(amount);
            changed();
        }

        return result;
    }

    @Override
    public IItemList<IAEItemStack> getAvailableItems(IItemList<IAEItemStack> out) {
        if (DiskStorageService.getCurrent() == null || hasMissingBackingRecord()) {
            return out;
        }

        for (IAEItemStack stack : contents()) {
            if (stack.getStackSize() > 0) {
                out.add(stack);
            }
        }
        return out;
    }

    @Override
    public IStorageChannel<IAEItemStack> getChannel() {
        return channel;
    }

    @Override
    public ItemStack getItemStack() {
        return cellStack;
    }

    @Override
    public double getIdleDrain() {
        return cellType.getIdleDrain();
    }

    @Override
    public FuzzyMode getFuzzyMode() {
        return cellType.getFuzzyMode(cellStack);
    }

    @Override
    public IItemHandler getConfigInventory() {
        return cellType.getConfigInventory(cellStack);
    }

    @Override
    public IItemHandler getUpgradesInventory() {
        return cellType.getUpgradesInventory(cellStack);
    }

    @Override
    public int getBytesPerType() {
        return 0;
    }

    @Override
    public boolean canHoldNewItem() {
        return !hasMissingBackingRecord() && getRemainingItemCount() > 0;
    }

    @Override
    public long getTotalBytes() {
        return cellType.getCapacity();
    }

    @Override
    public long getFreeBytes() {
        return Math.max(0, getTotalBytes() - getStoredItemCount());
    }

    @Override
    public long getUsedBytes() {
        return getStoredItemCount();
    }

    @Override
    public long getTotalItemTypes() {
        return Long.MAX_VALUE;
    }

    @Override
    public long getStoredItemCount() {
        if (DiskStorageService.getCurrent() == null || hasMissingBackingRecord()) {
            return cachedCount(TAG_ITEM_COUNT);
        }

        long total = 0;
        for (IAEItemStack stack : contents()) {
            if (stack.getStackSize() > 0) {
                total += stack.getStackSize();
            }
        }
        return total;
    }

    @Override
    public long getStoredItemTypes() {
        if (DiskStorageService.getCurrent() == null || hasMissingBackingRecord()) {
            return cachedCount(TAG_TYPE_COUNT);
        }

        long total = 0;
        for (IAEItemStack stack : contents()) {
            if (stack.getStackSize() > 0) {
                total++;
            }
        }
        return total;
    }

    private long cachedCount(String key) {
        CompoundNBT tag = cellStack.getTag();
        return tag == null ? 0 : Math.max(0, tag.getLong(key));
    }

    @Override
    public long getRemainingItemTypes() {
        return getRemainingItemCount() > 0 ? Long.MAX_VALUE - getStoredItemTypes() : 0;
    }

    @Override
    public long getRemainingItemCount() {
        return getFreeBytes();
    }

    @Override
    public int getUnusedItemCount() {
        return 0;
    }

    @Override
    public CellState getStatusForCell() {
        if (getStoredItemCount() == 0) {
            return CellState.EMPTY;
        }
        return getRemainingItemCount() > 0 ? CellState.NOT_EMPTY : CellState.FULL;
    }

    @Override
    public void persist() {
        if (!dirty || hasMissingBackingRecord()) {
            return;
        }

        DiskStorageData storage = DiskStorageService.getCurrent();
        if (storage == null) {
            // Preserve dirty=true so a later server-side save can retry.
            return;
        }

        long itemCount = countLoadedItems();
        long typeCount = countLoadedTypes();

        if (itemCount <= 0) {
            UUID uuid = getUuid();

            if (uuid == null) {
                // A never-used empty DISK does not need a backing record yet.
                if (cellStack.hasTag()) {
                    cellStack.getTag().remove(TAG_ITEM_COUNT);
                    cellStack.getTag().remove(TAG_TYPE_COUNT);
                }

                loadedUuid = null;
                loadedRevision = NO_RECORD_REVISION;
            } else {
                // UUIDs are storage identities. Creative copies or other exact ItemStack
                // clones with the same UUID intentionally remain aliases of the same DISK.
                // Keep an empty record instead of deleting it so every alias observes the
                // transition to empty and no clone becomes an orphan.
                long revision = storage.put(uuid, new ListNBT(), new long[0], 0);

                CompoundNBT tag = cellStack.getOrCreateTag();
                tag.putLong(TAG_ITEM_COUNT, 0);
                tag.putLong(TAG_TYPE_COUNT, 0);

                loadedUuid = uuid;
                loadedRevision = revision;
            }

            missingRecordWarningLogged = false;
            dirty = false;
            return;
        }

        UUID uuid = ensureUuid();
        ListNBT keys = new ListNBT();
        long[] amounts = new long[(int) typeCount];

        int index = 0;
        for (IAEItemStack stack : contents()) {
            if (stack.getStackSize() <= 0) {
                continue;
            }

            CompoundNBT key = new CompoundNBT();
            stack.writeToNBT(key);
            keys.add(key);
            amounts[index++] = stack.getStackSize();
        }

        if (index != amounts.length) {
            long[] trimmed = new long[index];
            System.arraycopy(amounts, 0, trimmed, 0, index);
            amounts = trimmed;
        }

        long revision = storage.put(uuid, keys, amounts, itemCount);

        CompoundNBT tag = cellStack.getOrCreateTag();
        tag.putLong(TAG_ITEM_COUNT, itemCount);
        tag.putLong(TAG_TYPE_COUNT, typeCount);

        loadedUuid = uuid;
        loadedRevision = revision;
        missingRecordWarningLogged = false;
        dirty = false;
    }

    private long countLoadedItems() {
        long total = 0;
        if (contents == null) {
            return 0;
        }

        for (IAEItemStack stack : contents) {
            if (stack.getStackSize() > 0) {
                total += stack.getStackSize();
            }
        }
        return total;
    }

    private long countLoadedTypes() {
        long total = 0;
        if (contents == null) {
            return 0;
        }

        for (IAEItemStack stack : contents) {
            if (stack.getStackSize() > 0) {
                total++;
            }
        }
        return total;
    }

    private void changed() {
        dirty = true;

        // The actual contents live outside the Drive/ItemStack NBT, so save them
        // immediately. The host save provider only persists the cell stack metadata.
        persist();

        if (saveProvider != null) {
            saveProvider.saveChanges(this);
        }
    }

    private boolean hasMissingBackingRecord() {
        UUID uuid = getUuid();
        DiskStorageData storage = DiskStorageService.getCurrent();
        if (uuid == null || storage == null || storage.get(uuid) != null) {
            return false;
        }

        // Once a DISK has a UUID, its backing record is permanent, including when
        // empty. A missing record therefore always means corrupted/incomplete
        // persistence. Never recreate it implicitly: aliases may have stale cached
        // counts and must not be able to overwrite the missing authoritative state.
        if (!missingRecordWarningLogged) {
            ExpansionAE.LOGGER.error(
                    "DISK {} references missing backing data. Blocking reads/writes to avoid silently recreating or overwriting storage.",
                    uuid);
            missingRecordWarningLogged = true;
        }
        return true;
    }

    private UUID getUuid() {
        CompoundNBT tag = cellStack.getTag();
        if (tag == null || !tag.hasUniqueId(TAG_UUID)) {
            return null;
        }
        return tag.getUniqueId(TAG_UUID);
    }

    private UUID ensureUuid() {
        UUID uuid = getUuid();
        if (uuid != null) {
            return uuid;
        }

        uuid = UUID.randomUUID();
        cellStack.getOrCreateTag().putUniqueId(TAG_UUID, uuid);
        return uuid;
    }
}
