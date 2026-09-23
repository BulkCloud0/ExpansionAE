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
import appeng.api.implementations.items.IStorageCell;
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
    private boolean invalidRecordWarningLogged;
    private UUID validatedUuid;
    private long validatedRevision = NO_RECORD_REVISION;
    private boolean validatedRecordInvalid;

    public DiskCellInventory(DiskStorageCellItem cellType, ItemStack cellStack, ISaveProvider saveProvider) {
        this.cellType = cellType;
        this.cellStack = cellStack;
        this.saveProvider = saveProvider;
        this.channel = ExpansionAEApi.get().storage().getStorageChannel(IItemStorageChannel.class);
        DiskAliasNotifier.track(this);
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

        if (DiskStorageService.getCurrent() == null || hasInvalidBackingRecord()) {
            // Fail closed when the authoritative server-side backing store is unavailable.
            return input;
        }

        if (isSelfAlias(input) || isUnsafeNestedStorageCell(input)) {
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

            IAEItemStack delta = input.copy();
            delta.setStackSize(accepted);
            DiskAliasNotifier.notifyOtherGrids(this, delta, source);
        }

        if (accepted == input.getStackSize()) {
            return null;
        }

        IAEItemStack remainder = input.copy();
        remainder.setStackSize(input.getStackSize() - accepted);
        return remainder;
    }

    private boolean isSelfAlias(IAEItemStack input) {
        UUID ownUuid = getUuid();
        if (ownUuid == null) {
            return false;
        }

        ItemStack nestedStack = input.createItemStack();
        if (!(nestedStack.getItem() instanceof DiskStorageCellItem)
                || !nestedStack.hasTag()
                || !nestedStack.getTag().hasUniqueId(TAG_UUID)) {
            return false;
        }

        return ownUuid.equals(nestedStack.getTag().getUniqueId(TAG_UUID));
    }

    private boolean isUnsafeNestedStorageCell(IAEItemStack input) {
        ItemStack nestedStack = input.createItemStack();
        if (!ExpansionAEApi.get().registries().cell().isCellHandled(nestedStack)) {
            return false;
        }

        ICellInventoryHandler<IAEItemStack> nested =
                ExpansionAEApi.get().registries().cell().getCellInventory(nestedStack, null, channel);
        if (nested == null) {
            // A handled cell that cannot be opened has unknown state. Fail closed.
            return true;
        }

        ICellInventory<IAEItemStack> nestedCell = nested.getCellInv();
        if (nestedCell instanceof DiskCellInventory) {
            // ExpansionAE DISKs use an external UUID-backed store. Even a currently
            // empty DISK can gain contents later through another alias with the same
            // UUID, after it has already been inserted here. Forbid ExpansionAE DISK
            // nesting entirely to avoid that time-of-check/time-of-use capacity bypass.
            return true;
        }

        if (!(nestedStack.getItem() instanceof IStorageCell)) {
            // Custom ICellHandler implementations are not guaranteed to keep their
            // contents in the ItemStack. An apparently empty cell may be backed by an
            // external identity and gain contents through another alias after nesting.
            // Unknown custom storage therefore fails closed.
            return true;
        }

        IStorageCell<?> nativeCell = (IStorageCell<?>) nestedStack.getItem();
        if (nativeCell.storableInStorageCell()) {
            // Match AE2's own BasicCellInventory semantics for special cells that
            // explicitly opt into being stored inside another storage cell.
            return false;
        }

        return !nested.getAvailableItems(channel.createList()).isEmpty();
    }

    @Override
    public IAEItemStack extractItems(IAEItemStack request, Actionable mode, IActionSource source) {
        if (request == null || request.getStackSize() <= 0) {
            return null;
        }

        if (DiskStorageService.getCurrent() == null || hasInvalidBackingRecord()) {
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

            IAEItemStack delta = result.copy();
            delta.setStackSize(-amount);
            DiskAliasNotifier.notifyOtherGrids(this, delta, source);
        }

        return result;
    }

    @Override
    public IItemList<IAEItemStack> getAvailableItems(IItemList<IAEItemStack> out) {
        if (DiskStorageService.getCurrent() == null
                || hasInvalidBackingRecord()
                || !DiskAliasExposure.shouldExposeToGrid(cellStack, saveProvider)) {
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
        return !hasInvalidBackingRecord() && getRemainingItemCount() > 0;
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
        if (DiskStorageService.getCurrent() == null || hasInvalidBackingRecord()) {
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
        if (DiskStorageService.getCurrent() == null || hasInvalidBackingRecord()) {
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
        if (!dirty || hasInvalidBackingRecord()) {
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
                long revision = storage.put(uuid, new ListNBT(), new long[0], 0, cellType.getCapacity());

                CompoundNBT tag = cellStack.getOrCreateTag();
                tag.putLong(TAG_ITEM_COUNT, 0);
                tag.putLong(TAG_TYPE_COUNT, 0);

                loadedUuid = uuid;
                loadedRevision = revision;
            }

            invalidRecordWarningLogged = false;
            dirty = false;
            return;
        }

        UUID uuid = ensureUuid();
        ListNBT keys = new ListNBT();
        long[] amounts = new long[(int) typeCount];

        // Persist the already-loaded dirty snapshot directly. Calling contents()
        // here is unsafe for the first write: ensureUuid() gives the ItemStack a
        // UUID before a backing record exists, so contents() would interpret that
        // as a cache miss and replace the dirty in-memory list with an empty one.
        int index = 0;
        for (IAEItemStack stack : contents) {
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

        long revision = storage.put(uuid, keys, amounts, itemCount, cellType.getCapacity());

        CompoundNBT tag = cellStack.getOrCreateTag();
        tag.putLong(TAG_ITEM_COUNT, itemCount);
        tag.putLong(TAG_TYPE_COUNT, typeCount);

        loadedUuid = uuid;
        loadedRevision = revision;
        invalidRecordWarningLogged = false;
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

    private boolean hasInvalidBackingRecord() {
        CompoundNBT tag = cellStack.getTag();
        if (tag != null && tag.contains(TAG_UUID) && !tag.hasUniqueId(TAG_UUID)) {
            if (!invalidRecordWarningLogged) {
                ExpansionAE.LOGGER.error(
                        "DISK ItemStack contains malformed UUID metadata. Blocking reads/writes without replacing the identity tag.");
                invalidRecordWarningLogged = true;
            }
            return true;
        }

        UUID uuid = getUuid();
        DiskStorageData storage = DiskStorageService.getCurrent();
        if (storage == null) {
            return false;
        }

        if (storage.isGloballyQuarantined()) {
            if (!invalidRecordWarningLogged) {
                ExpansionAE.LOGGER.error(
                        "DISK storage root is quarantined due to structurally invalid persisted NBT. Blocking all DISK reads/writes until explicit recovery.");
                invalidRecordWarningLogged = true;
            }
            return true;
        }

        if (uuid == null) {
            return false;
        }

        DiskStorageData.DiskRecord record = storage.get(uuid);
        if (record == null) {
            // Once a DISK has a UUID, its backing record is permanent, including
            // when empty. Never recreate a missing authoritative record implicitly.
            if (!invalidRecordWarningLogged) {
                if (storage.isQuarantined(uuid)) {
                    ExpansionAE.LOGGER.error(
                            "DISK {} has quarantined persisted backing data. Blocking reads/writes without normalizing, choosing or deleting the quarantined record(s).",
                            uuid);
                } else {
                    ExpansionAE.LOGGER.error(
                            "DISK {} references missing backing data. Blocking reads/writes to avoid silently recreating or overwriting storage.",
                            uuid);
                }
                invalidRecordWarningLogged = true;
            }
            return true;
        }

        long expectedCapacity = cellType.getCapacity();

        if (record.getCapacity() == 0) {
            // Migration path for records created before tiers were bound to UUIDs.
            // Never mutate legacy metadata until the payload itself is known-valid:
            // binding an undecodable record could permanently attach corrupted data
            // to whichever tier happened to open it first.
            if (record.getItemCount() > expectedCapacity) {
                if (!invalidRecordWarningLogged) {
                    ExpansionAE.LOGGER.error(
                            "Legacy DISK {} contains {} items, which exceeds the {}-item capacity of this DISK tier. Blocking access.",
                            uuid,
                            record.getItemCount(),
                            expectedCapacity);
                    invalidRecordWarningLogged = true;
                }
                return true;
            }

            if (hasInvalidBackingPayload(uuid, record, expectedCapacity)) {
                return true;
            }

            record = storage.bindCapacity(uuid, expectedCapacity);
        }

        if (record == null || record.getCapacity() != expectedCapacity) {
            if (!invalidRecordWarningLogged) {
                ExpansionAE.LOGGER.error(
                        "DISK {} is bound to capacity {} but was opened as capacity {}. Blocking cross-tier UUID alias access.",
                        uuid,
                        record == null ? 0 : record.getCapacity(),
                        expectedCapacity);
                invalidRecordWarningLogged = true;
            }
            return true;
        }

        if (record.getItemCount() > expectedCapacity) {
            if (!invalidRecordWarningLogged) {
                ExpansionAE.LOGGER.error(
                        "DISK {} backing data contains {} items, exceeding its bound {}-item capacity. Blocking access without truncating data.",
                        uuid,
                        record.getItemCount(),
                        expectedCapacity);
                invalidRecordWarningLogged = true;
            }
            return true;
        }

        if (hasInvalidBackingPayload(uuid, record, expectedCapacity)) {
            return true;
        }

        invalidRecordWarningLogged = false;
        return false;
    }

    private boolean hasInvalidBackingPayload(
            UUID uuid,
            DiskStorageData.DiskRecord record,
            long expectedCapacity) {
        long revision = record.getRevision();
        if (uuid.equals(validatedUuid) && validatedRevision == revision) {
            return validatedRecordInvalid;
        }

        ListNBT keys = record.getKeys();
        long[] amounts = record.getAmounts();
        boolean invalid = keys.size() != amounts.length;
        long total = 0;
        IItemList<IAEItemStack> decodedContents = channel.createList();

        if (!invalid) {
            for (int i = 0; i < amounts.length; i++) {
                long amount = amounts[i];
                if (amount <= 0 || amount > expectedCapacity - total) {
                    invalid = true;
                    break;
                }

                IAEItemStack decoded;
                try {
                    decoded = channel.createFromNBT(keys.getCompound(i));
                } catch (RuntimeException exception) {
                    decoded = null;
                }

                if (decoded == null) {
                    invalid = true;
                    break;
                }

                decoded.setStackSize(amount);
                decodedContents.add(decoded);
                total += amount;
            }
        }

        if (!invalid && total != record.getItemCount()) {
            invalid = true;
        }

        validatedUuid = uuid;
        validatedRevision = revision;
        validatedRecordInvalid = invalid;

        if (invalid) {
            if (!invalidRecordWarningLogged) {
                ExpansionAE.LOGGER.error(
                        "DISK {} backing data contains undecodable or inconsistent item entries. Blocking access without rewriting data.",
                        uuid);
                invalidRecordWarningLogged = true;
            }
            return true;
        }

        contents = decodedContents;
        loadedUuid = uuid;
        loadedRevision = revision;
        return false;
    }

    void refreshCachedMetadataFromBacking() {
        UUID uuid = getUuid();
        DiskStorageData storage = DiskStorageService.getCurrent();
        if (uuid == null || storage == null || storage.isGloballyQuarantined()) {
            return;
        }

        DiskStorageData.DiskRecord record = storage.get(uuid);
        if (record == null
                || record.getCapacity() != cellType.getCapacity()) {
            return;
        }

        long itemCount = record.getItemCount();
        long typeCount = record.getAmounts().length;

        CompoundNBT tag = cellStack.getOrCreateTag();
        boolean changed = tag.getLong(TAG_ITEM_COUNT) != itemCount
                || tag.getLong(TAG_TYPE_COUNT) != typeCount;

        if (!changed) {
            return;
        }

        tag.putLong(TAG_ITEM_COUNT, itemCount);
        tag.putLong(TAG_TYPE_COUNT, typeCount);

        if (saveProvider != null) {
            saveProvider.saveChanges(this);
        }
    }


    DiskStorageData.QuarantineSnapshot snapshotRuntimeDiagnostic() {
        CompoundNBT tag = cellStack.getTag();
        if (tag != null && tag.contains(TAG_UUID) && !tag.hasUniqueId(TAG_UUID)) {
            return DiskStorageData.runtimeSnapshot(
                    null,
                    DiskStorageData.QuarantineReason.MALFORMED_ITEMSTACK_UUID,
                    tag.copy(),
                    cellType.getCapacity());
        }

        UUID uuid = getUuid();
        DiskStorageData storage = DiskStorageService.getCurrent();
        if (uuid == null || storage == null || storage.isGloballyQuarantined()) {
            return null;
        }

        DiskStorageData.DiskRecord record = storage.get(uuid);
        if (record == null) {
            if (storage.isQuarantined(uuid)) {
                return null;
            }
            CompoundNBT payload = tag == null ? new CompoundNBT() : tag.copy();
            return DiskStorageData.runtimeSnapshot(
                    uuid,
                    DiskStorageData.QuarantineReason.MISSING_BACKING,
                    payload,
                    cellType.getCapacity());
        }

        long expectedCapacity = cellType.getCapacity();
        if (record.getCapacity() < 0) {
            return null;
        }

        if (record.getCapacity() > 0
                && record.getCapacity() != expectedCapacity) {
            return DiskStorageData.runtimeSnapshot(
                    uuid,
                    DiskStorageData.QuarantineReason.TIER_CAPACITY_MISMATCH,
                    DiskStorageData.snapshotRecordForDiagnostics(uuid, record),
                    expectedCapacity);
        }

        if (record.getItemCount() > expectedCapacity) {
            return DiskStorageData.runtimeSnapshot(
                    uuid,
                    DiskStorageData.QuarantineReason.OVER_CAPACITY,
                    DiskStorageData.snapshotRecordForDiagnostics(uuid, record),
                    expectedCapacity);
        }

        ListNBT keys = record.getKeys();
        long[] amounts = record.getAmounts();
        if (keys.size() != amounts.length) {
            return DiskStorageData.runtimeSnapshot(
                    uuid,
                    DiskStorageData.QuarantineReason.INVALID_KEYS_AMOUNTS,
                    DiskStorageData.snapshotRecordForDiagnostics(uuid, record),
                    expectedCapacity);
        }

        long total = 0;
        for (int i = 0; i < amounts.length; i++) {
            long amount = amounts[i];
            if (amount <= 0) {
                return DiskStorageData.runtimeSnapshot(
                        uuid,
                        DiskStorageData.QuarantineReason.INVALID_KEYS_AMOUNTS,
                        DiskStorageData.snapshotRecordForDiagnostics(uuid, record),
                        expectedCapacity);
            }
            if (amount > expectedCapacity - total) {
                return DiskStorageData.runtimeSnapshot(
                        uuid,
                        DiskStorageData.QuarantineReason.OVER_CAPACITY,
                        DiskStorageData.snapshotRecordForDiagnostics(uuid, record),
                        expectedCapacity);
            }

            IAEItemStack decoded;
            try {
                decoded = channel.createFromNBT(keys.getCompound(i));
            } catch (RuntimeException exception) {
                decoded = null;
            }
            if (decoded == null) {
                return DiskStorageData.runtimeSnapshot(
                        uuid,
                        DiskStorageData.QuarantineReason.UNDECODABLE_ITEM_KEY,
                        DiskStorageData.snapshotRecordForDiagnostics(uuid, record),
                        expectedCapacity);
            }

            total += amount;
        }

        if (total != record.getItemCount()) {
            return DiskStorageData.runtimeSnapshot(
                    uuid,
                    DiskStorageData.QuarantineReason.INCONSISTENT_ITEM_COUNT,
                    DiskStorageData.snapshotRecordForDiagnostics(uuid, record),
                    expectedCapacity);
        }

        return null;
    }

    UUID getUuidForAliasSync() {
        return getUuid();
    }

    ISaveProvider getSaveProviderForAliasSync() {
        return saveProvider;
    }

    private UUID getUuid() {
        CompoundNBT tag = cellStack.getTag();
        if (tag == null || !tag.hasUniqueId(TAG_UUID)) {
            return null;
        }
        return tag.getUniqueId(TAG_UUID);
    }

    private UUID ensureUuid() {
        CompoundNBT tag = cellStack.getTag();
        if (tag != null && tag.contains(TAG_UUID) && !tag.hasUniqueId(TAG_UUID)) {
            throw new IllegalStateException(
                    "Cannot allocate a new DISK UUID over malformed persisted identity metadata");
        }

        UUID uuid = getUuid();
        if (uuid != null) {
            return uuid;
        }

        uuid = UUID.randomUUID();
        cellStack.getOrCreateTag().putUniqueId(TAG_UUID, uuid);
        return uuid;
    }
}
