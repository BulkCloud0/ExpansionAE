package com.bulkcloud.expansionae.feature.disk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.WorldSavedData;

import com.bulkcloud.expansionae.ExpansionAE;

public final class DiskStorageData extends WorldSavedData {
    public static final String DATA_NAME = "expansionae_disk_storage";

    private static final String TAG_DISKS = "disks";
    private static final String TAG_UUID = "uuid";
    private static final String TAG_KEYS = "keys";
    private static final String TAG_AMOUNTS = "amounts";
    private static final String TAG_ITEM_COUNT = "item_count";
    private static final String TAG_CAPACITY = "capacity";

    private final Map<UUID, DiskRecord> disks = new HashMap<>();
    private final Map<UUID, List<CompoundNBT>> quarantinedDuplicateRecords = new HashMap<>();
    private final Map<UUID, List<CompoundNBT>> quarantinedInvalidRecords = new HashMap<>();
    private final List<CompoundNBT> quarantinedMalformedRecords = new ArrayList<>();
    private INBT quarantinedRootDisksTag;
    private long revisionCounter;

    public DiskStorageData() {
        super(DATA_NAME);
    }

    public static DiskStorageData get(ServerWorld world) {
        return world.getSavedData().getOrCreate(DiskStorageData::new, DATA_NAME);
    }

    @Override
    public void read(CompoundNBT nbt) {
        disks.clear();
        quarantinedDuplicateRecords.clear();
        quarantinedInvalidRecords.clear();
        quarantinedMalformedRecords.clear();
        quarantinedRootDisksTag = null;
        revisionCounter = 0;

        if (nbt.contains(TAG_DISKS) && !nbt.contains(TAG_DISKS, 9)) {
            INBT rawDisksTag = nbt.get(TAG_DISKS);
            quarantinedRootDisksTag = rawDisksTag == null ? null : rawDisksTag.copy();
            ExpansionAE.LOGGER.error(
                    "DISK storage root '{}' tag has invalid NBT type {}. "
                            + "Quarantining the entire DISK storage and blocking mutations to avoid overwriting raw data.",
                    TAG_DISKS,
                    nbt.getTagId(TAG_DISKS));
            return;
        }

        if (nbt.contains(TAG_DISKS, 9)) {
            ListNBT rawRootList = (ListNBT) nbt.get(TAG_DISKS);
            if (!rawRootList.isEmpty() && rawRootList.getTagType() != 10) {
                quarantinedRootDisksTag = rawRootList.copy();
                ExpansionAE.LOGGER.error(
                        "DISK storage root '{}' list contains element type {} instead of compound records. "
                                + "Quarantining the entire storage instead of interpreting it as an empty list.",
                        TAG_DISKS,
                        rawRootList.getTagType());
                return;
            }
        }

        boolean repaired = false;
        ListNBT list = nbt.getList(TAG_DISKS, 10);
        Map<UUID, Integer> uuidOccurrences = new HashMap<>();

        for (int i = 0; i < list.size(); i++) {
            CompoundNBT diskTag = list.getCompound(i);
            if (diskTag.hasUniqueId(TAG_UUID)) {
                UUID uuid = diskTag.getUniqueId(TAG_UUID);
                uuidOccurrences.put(uuid, uuidOccurrences.getOrDefault(uuid, 0) + 1);
            }
        }

        for (Map.Entry<UUID, Integer> occurrence : uuidOccurrences.entrySet()) {
            if (occurrence.getValue() > 1) {
                ExpansionAE.LOGGER.error(
                        "DISK storage contains {} persisted records for UUID {}. "
                                + "Preserving them in quarantine and blocking access to avoid arbitrary data loss.",
                        occurrence.getValue(),
                        occurrence.getKey());
            }
        }

        for (int i = 0; i < list.size(); i++) {
            CompoundNBT diskTag = list.getCompound(i);
            if (!diskTag.hasUniqueId(TAG_UUID)) {
                quarantinedMalformedRecords.add(diskTag.copy());
                ExpansionAE.LOGGER.error(
                        "DISK storage contains a persisted record without a valid UUID. "
                                + "Preserving it in quarantine and blocking automatic repair to avoid data loss.");
                continue;
            }

            UUID uuid = diskTag.getUniqueId(TAG_UUID);
            if (uuidOccurrences.getOrDefault(uuid, 0) > 1) {
                quarantinedDuplicateRecords
                        .computeIfAbsent(uuid, ignored -> new ArrayList<>())
                        .add(diskTag.copy());
                continue;
            }

            boolean invalidStructure = false;
            if (!diskTag.contains(TAG_KEYS) || !diskTag.contains(TAG_AMOUNTS)) {
                // Every ExpansionAE backing format since the first DISK implementation
                // writes both authoritative fields, including for empty records.
                // Missing either side is therefore corruption, not a legacy encoding.
                invalidStructure = true;
            }
            if (diskTag.contains(TAG_KEYS) && !diskTag.contains(TAG_KEYS, 9)) {
                invalidStructure = true;
            }
            if (diskTag.contains(TAG_AMOUNTS) && !diskTag.contains(TAG_AMOUNTS, 12)) {
                invalidStructure = true;
            }
            if (diskTag.contains(TAG_CAPACITY) && !diskTag.contains(TAG_CAPACITY, 4)) {
                // capacity has always been written with putLong since tier binding
                // was introduced. Reject other numeric NBT types as structural
                // corruption instead of allowing getLong() to coerce/truncate them.
                invalidStructure = true;
            }

            ListNBT rawKeyList = diskTag.contains(TAG_KEYS, 9)
                    ? (ListNBT) diskTag.get(TAG_KEYS)
                    : new ListNBT();
            if (!rawKeyList.isEmpty() && rawKeyList.getTagType() != 10) {
                invalidStructure = true;
            }

            if (invalidStructure) {
                quarantinedInvalidRecords
                        .computeIfAbsent(uuid, ignored -> new ArrayList<>())
                        .add(diskTag.copy());
                ExpansionAE.LOGGER.error(
                        "DISK {} backing record contains structurally invalid NBT types. "
                                + "Preserving it in quarantine instead of normalizing it to empty data.",
                        uuid);
                continue;
            }

            ListNBT rawKeys = diskTag.getList(TAG_KEYS, 10);
            long[] rawAmounts = diskTag.getLongArray(TAG_AMOUNTS);

            if (rawKeys.size() != rawAmounts.length) {
                quarantinedInvalidRecords
                        .computeIfAbsent(uuid, ignored -> new ArrayList<>())
                        .add(diskTag.copy());
                ExpansionAE.LOGGER.error(
                        "DISK {} backing record has {} keys but {} amounts. "
                                + "Preserving the raw record in quarantine instead of truncating unmatched entries.",
                        uuid,
                        rawKeys.size(),
                        rawAmounts.length);
                continue;
            }

            boolean negativeAmount = false;
            boolean itemCountOverflow = false;
            for (long amount : rawAmounts) {
                if (amount < 0) {
                    negativeAmount = true;
                    break;
                }
            }

            if (negativeAmount) {
                quarantinedInvalidRecords
                        .computeIfAbsent(uuid, ignored -> new ArrayList<>())
                        .add(diskTag.copy());
                ExpansionAE.LOGGER.error(
                        "DISK {} backing record contains a negative item amount. "
                                + "Preserving the raw record in quarantine instead of discarding the entry.",
                        uuid);
                continue;
            }

            ListNBT keys = new ListNBT();
            long[] amounts = new long[rawAmounts.length];
            int writeIndex = 0;
            long itemCount = 0;

            for (int entryIndex = 0; entryIndex < rawAmounts.length; entryIndex++) {
                long amount = rawAmounts[entryIndex];
                if (amount == 0) {
                    repaired = true;
                    continue;
                }

                if (itemCount > Long.MAX_VALUE - amount) {
                    itemCountOverflow = true;
                    break;
                }

                keys.add(rawKeys.getCompound(entryIndex).copy());
                amounts[writeIndex++] = amount;
                itemCount += amount;
            }

            if (itemCountOverflow) {
                quarantinedInvalidRecords
                        .computeIfAbsent(uuid, ignored -> new ArrayList<>())
                        .add(diskTag.copy());
                ExpansionAE.LOGGER.error(
                        "DISK {} backing item amounts overflow long item_count. "
                                + "Preserving the raw record in quarantine.",
                        uuid);
                continue;
            }

            if (writeIndex != amounts.length) {
                amounts = Arrays.copyOf(amounts, writeIndex);
            }

            if (diskTag.getLong(TAG_ITEM_COUNT) != itemCount) {
                repaired = true;
            }

            // capacity=0 is the intentional legacy/unbound representation. Records
            // written before tier binding did not contain this tag and are bound once,
            // on first legitimate access by a DISK ItemStack. Preserve negative values
            // as invalid metadata so they fail closed instead of being reinterpreted as
            // a legitimate legacy record.
            long capacity = diskTag.getLong(TAG_CAPACITY);

            disks.put(
                    uuid,
                    new DiskRecord(
                            keys,
                            amounts,
                            itemCount,
                            capacity,
                            nextRevision()));
        }

        if (repaired) {
            setDirty(true);
        }
    }

    @Override
    public CompoundNBT write(CompoundNBT nbt) {
        if (quarantinedRootDisksTag != null) {
            nbt.put(TAG_DISKS, quarantinedRootDisksTag.copy());
            return nbt;
        }

        ListNBT list = new ListNBT();

        for (Map.Entry<UUID, DiskRecord> entry : disks.entrySet()) {
            CompoundNBT diskTag = new CompoundNBT();
            diskTag.putUniqueId(TAG_UUID, entry.getKey());

            DiskRecord record = entry.getValue();
            diskTag.put(TAG_KEYS, record.keys.copy());
            diskTag.putLongArray(TAG_AMOUNTS, record.amounts.clone());
            diskTag.putLong(TAG_ITEM_COUNT, record.itemCount);
            diskTag.putLong(TAG_CAPACITY, record.capacity);

            list.add(diskTag);
        }

        for (List<CompoundNBT> quarantined : quarantinedDuplicateRecords.values()) {
            for (CompoundNBT diskTag : quarantined) {
                list.add(diskTag.copy());
            }
        }

        for (List<CompoundNBT> quarantined : quarantinedInvalidRecords.values()) {
            for (CompoundNBT diskTag : quarantined) {
                list.add(diskTag.copy());
            }
        }

        for (CompoundNBT diskTag : quarantinedMalformedRecords) {
            list.add(diskTag.copy());
        }

        nbt.put(TAG_DISKS, list);
        return nbt;
    }

    public DiskRecord get(UUID uuid) {
        return disks.get(uuid);
    }


    public List<QuarantineSnapshot> getQuarantineSnapshots() {
        List<QuarantineSnapshot> snapshots = new ArrayList<>();

        if (quarantinedRootDisksTag != null) {
            snapshots.add(new QuarantineSnapshot(
                    null,
                    QuarantineReason.INVALID_ROOT_DISKS,
                    quarantinedRootDisksTag,
                    1));
            return Collections.unmodifiableList(snapshots);
        }

        List<UUID> duplicateIds = new ArrayList<>(quarantinedDuplicateRecords.keySet());
        Collections.sort(duplicateIds);
        for (UUID uuid : duplicateIds) {
            List<CompoundNBT> records = quarantinedDuplicateRecords.get(uuid);
            int duplicateCount = records.size();
            for (CompoundNBT record : records) {
                snapshots.add(new QuarantineSnapshot(
                        uuid,
                        QuarantineReason.DUPLICATE_PERSISTED_UUID,
                        record,
                        duplicateCount));
            }
        }

        List<UUID> invalidIds = new ArrayList<>(quarantinedInvalidRecords.keySet());
        Collections.sort(invalidIds);
        for (UUID uuid : invalidIds) {
            for (CompoundNBT record : quarantinedInvalidRecords.get(uuid)) {
                snapshots.add(new QuarantineSnapshot(
                        uuid,
                        classifyInvalidRecord(record),
                        record,
                        1));
            }
        }

        for (CompoundNBT record : quarantinedMalformedRecords) {
            snapshots.add(new QuarantineSnapshot(
                    null,
                    QuarantineReason.MISSING_PERSISTED_UUID,
                    record,
                    1));
        }

        List<UUID> liveIds = new ArrayList<>(disks.keySet());
        Collections.sort(liveIds);
        for (UUID uuid : liveIds) {
            DiskRecord record = disks.get(uuid);
            if (record.capacity < 0) {
                snapshots.add(new QuarantineSnapshot(
                        uuid,
                        QuarantineReason.NEGATIVE_CAPACITY,
                        snapshotRecord(uuid, record),
                        1));
            }
        }

        return Collections.unmodifiableList(snapshots);
    }

    public List<QuarantineSnapshot> getQuarantineSnapshots(UUID uuid) {
        if (uuid == null) {
            return Collections.emptyList();
        }

        List<QuarantineSnapshot> matches = new ArrayList<>();
        for (QuarantineSnapshot snapshot : getQuarantineSnapshots()) {
            if (uuid.equals(snapshot.getUuid())) {
                matches.add(snapshot);
            }
        }
        return Collections.unmodifiableList(matches);
    }

    long getRevisionCounterForDiagnostics() {
        return revisionCounter;
    }

    private static QuarantineReason classifyInvalidRecord(CompoundNBT record) {
        if (record.contains(TAG_CAPACITY) && !record.contains(TAG_CAPACITY, 4)) {
            return QuarantineReason.INVALID_CAPACITY_TAG;
        }
        if (!record.contains(TAG_KEYS) || !record.contains(TAG_AMOUNTS)) {
            return QuarantineReason.INCOMPLETE_KEYS_AMOUNTS;
        }
        if (!record.contains(TAG_KEYS, 9) || !record.contains(TAG_AMOUNTS, 12)) {
            return QuarantineReason.INVALID_KEYS_AMOUNTS;
        }

        ListNBT keys = (ListNBT) record.get(TAG_KEYS);
        if (!keys.isEmpty() && keys.getTagType() != 10) {
            return QuarantineReason.INVALID_KEYS_AMOUNTS;
        }

        long[] amounts = record.getLongArray(TAG_AMOUNTS);
        if (keys.size() != amounts.length) {
            return QuarantineReason.INVALID_KEYS_AMOUNTS;
        }

        long total = 0;
        for (long amount : amounts) {
            if (amount < 0 || total > Long.MAX_VALUE - amount) {
                return QuarantineReason.INVALID_KEYS_AMOUNTS;
            }
            total += amount;
        }

        return QuarantineReason.INVALID_RECORD_STRUCTURE;
    }

    static QuarantineSnapshot runtimeSnapshot(
            UUID uuid,
            QuarantineReason reason,
            INBT rawPayload) {
        return new QuarantineSnapshot(uuid, reason, rawPayload, 1);
    }

    static CompoundNBT snapshotRecordForDiagnostics(UUID uuid, DiskRecord record) {
        return snapshotRecord(uuid, record);
    }

    private static CompoundNBT snapshotRecord(UUID uuid, DiskRecord record) {
        CompoundNBT tag = new CompoundNBT();
        tag.putUniqueId(TAG_UUID, uuid);
        tag.put(TAG_KEYS, record.getKeys());
        tag.putLongArray(TAG_AMOUNTS, record.getAmounts());
        tag.putLong(TAG_ITEM_COUNT, record.getItemCount());
        tag.putLong(TAG_CAPACITY, record.getCapacity());
        return tag;
    }

    public enum QuarantineReason {
        INVALID_ROOT_DISKS,
        MISSING_PERSISTED_UUID,
        DUPLICATE_PERSISTED_UUID,
        INVALID_CAPACITY_TAG,
        INCOMPLETE_KEYS_AMOUNTS,
        INVALID_KEYS_AMOUNTS,
        INVALID_RECORD_STRUCTURE,
        NEGATIVE_CAPACITY,
        MALFORMED_ITEMSTACK_UUID,
        MISSING_BACKING,
        OVER_CAPACITY,
        TIER_CAPACITY_MISMATCH,
        UNDECODABLE_ITEM_KEY,
        INCONSISTENT_ITEM_COUNT
    }

    public static final class QuarantineSnapshot {
        private final UUID uuid;
        private final QuarantineReason reason;
        private final INBT rawPayload;
        private final int duplicateCount;

        private QuarantineSnapshot(
                UUID uuid,
                QuarantineReason reason,
                INBT rawPayload,
                int duplicateCount) {
            this.uuid = uuid;
            this.reason = reason;
            this.rawPayload = rawPayload == null ? null : rawPayload.copy();
            this.duplicateCount = duplicateCount;
        }

        public UUID getUuid() {
            return uuid;
        }

        public QuarantineReason getReason() {
            return reason;
        }

        public INBT getRawPayload() {
            return rawPayload == null ? null : rawPayload.copy();
        }

        public int getDuplicateCount() {
            return duplicateCount;
        }

        public Long getStoredCapacity() {
            if (!(rawPayload instanceof CompoundNBT)) {
                return null;
            }
            CompoundNBT tag = (CompoundNBT) rawPayload;
            return tag.contains(TAG_CAPACITY, 4) ? tag.getLong(TAG_CAPACITY) : null;
        }

        public Long getItemCount() {
            if (!(rawPayload instanceof CompoundNBT)) {
                return null;
            }
            CompoundNBT tag = (CompoundNBT) rawPayload;
            return tag.contains(TAG_ITEM_COUNT, 4) ? tag.getLong(TAG_ITEM_COUNT) : null;
        }

        String sortKey() {
            String id = uuid == null ? "~" : uuid.toString();
            String payload = rawPayload == null ? "" : rawPayload.toString();
            return id + "|" + reason.name() + "|" + payload;
        }

        public Integer getTypeCount() {
            if (!(rawPayload instanceof CompoundNBT)) {
                return null;
            }
            CompoundNBT tag = (CompoundNBT) rawPayload;
            if (!tag.contains(TAG_KEYS, 9)) {
                return null;
            }
            ListNBT keys = (ListNBT) tag.get(TAG_KEYS);
            if (!keys.isEmpty() && keys.getTagType() != 10) {
                return null;
            }
            return keys.size();
        }
    }

    boolean isGloballyQuarantined() {
        return quarantinedRootDisksTag != null;
    }

    boolean isQuarantined(UUID uuid) {
        return quarantinedDuplicateRecords.containsKey(uuid)
                || quarantinedInvalidRecords.containsKey(uuid);
    }

    public DiskRecord getOrCreate(UUID uuid) {
        return getOrCreate(uuid, 0);
    }

    public DiskRecord getOrCreate(UUID uuid, long capacity) {
        requireWritableUuid(uuid);
        if (capacity < 0) {
            throw new IllegalArgumentException("DISK capacity cannot be negative");
        }
        DiskRecord existing = disks.get(uuid);
        if (existing != null) {
            // Existing legacy records must never be tier-bound as a side effect of
            // generic lookup/creation. The caller that owns the storage semantics
            // must validate the payload first, then call bindCapacity explicitly.
            return existing;
        }

        DiskRecord created =
                new DiskRecord(
                        new ListNBT(),
                        new long[0],
                        0,
                        capacity,
                        nextRevision());
        disks.put(uuid, created);
        setDirty(true);
        return created;
    }

    public DiskRecord bindCapacity(UUID uuid, long capacity) {
        requireWritableUuid(uuid);
        if (capacity < 0) {
            throw new IllegalArgumentException("DISK capacity cannot be negative");
        }
        DiskRecord existing = disks.get(uuid);
        if (existing == null || existing.capacity != 0 || capacity == 0) {
            return existing;
        }
        if (existing.itemCount > capacity) {
            throw new IllegalStateException(
                    "Legacy DISK " + uuid
                            + " contains " + existing.itemCount
                            + " items, exceeding requested binding capacity " + capacity);
        }

        DiskRecord bound = new DiskRecord(
                existing.getKeys(),
                existing.getAmounts(),
                existing.itemCount,
                capacity,
                nextRevision());
        disks.put(uuid, bound);
        setDirty(true);
        return bound;
    }

    public long put(UUID uuid, ListNBT keys, long[] amounts, long itemCount) {
        return put(uuid, keys, amounts, itemCount, 0);
    }

    public long put(
            UUID uuid,
            ListNBT keys,
            long[] amounts,
            long itemCount,
            long capacity) {
        requireWritableUuid(uuid);
        validateRecordForWrite(keys, amounts, itemCount, capacity);
        long revision = nextRevision();
        disks.put(
                uuid,
                new DiskRecord(
                        (ListNBT) keys.copy(),
                        amounts.clone(),
                        itemCount,
                        capacity,
                        revision));
        setDirty(true);
        return revision;
    }

    public void remove(UUID uuid) {
        requireWritableUuid(uuid);
        if (disks.remove(uuid) != null) {
            nextRevision();
            setDirty(true);
        }
    }

    private void requireWritableUuid(UUID uuid) {
        if (uuid == null) {
            throw new IllegalArgumentException("DISK UUID cannot be null");
        }
        requireStorageWritable();
        requireNotQuarantined(uuid);
    }

    private static void validateRecordForWrite(
            ListNBT keys,
            long[] amounts,
            long itemCount,
            long capacity) {
        if (keys == null || amounts == null) {
            throw new IllegalArgumentException("DISK keys/amounts cannot be null");
        }
        if (capacity < 0) {
            throw new IllegalArgumentException("DISK capacity cannot be negative");
        }
        if (keys.size() != amounts.length) {
            throw new IllegalArgumentException(
                    "DISK keys/amounts must have the same number of entries");
        }
        if (!keys.isEmpty() && keys.getTagType() != 10) {
            throw new IllegalArgumentException(
                    "DISK keys must be compound NBT entries");
        }

        long computedCount = 0;
        for (long amount : amounts) {
            if (amount <= 0) {
                throw new IllegalArgumentException(
                        "DISK persisted item amounts must be positive");
            }
            if (computedCount > Long.MAX_VALUE - amount) {
                throw new IllegalArgumentException(
                        "DISK persisted item amounts overflow item_count");
            }
            computedCount += amount;
        }

        if (itemCount != computedCount) {
            throw new IllegalArgumentException(
                    "DISK item_count must equal the sum of persisted amounts");
        }
    }

    private void requireStorageWritable() {
        if (quarantinedRootDisksTag != null) {
            throw new IllegalStateException(
                    "DISK storage root is quarantined because its persisted '"
                            + TAG_DISKS
                            + "' tag has an invalid NBT type");
        }
    }

    private void requireNotQuarantined(UUID uuid) {
        if (isQuarantined(uuid)) {
            throw new IllegalStateException(
                    "DISK UUID " + uuid
                            + " has quarantined persisted backing data and cannot be mutated generically");
        }
    }

    private long nextRevision() {
        return ++revisionCounter;
    }

    public static final class DiskRecord {
        private final ListNBT keys;
        private final long[] amounts;
        private final long itemCount;
        private final long capacity;
        private final long revision;

        private DiskRecord(
                ListNBT keys,
                long[] amounts,
                long itemCount,
                long capacity,
                long revision) {
            this.keys = keys;
            this.amounts = amounts;
            this.itemCount = itemCount;
            this.capacity = capacity;
            this.revision = revision;
        }

        public ListNBT getKeys() {
            return (ListNBT) keys.copy();
        }

        public long[] getAmounts() {
            return amounts.clone();
        }

        public long getItemCount() {
            return itemCount;
        }

        public long getCapacity() {
            return capacity;
        }

        public long getRevision() {
            return revision;
        }
    }
}
