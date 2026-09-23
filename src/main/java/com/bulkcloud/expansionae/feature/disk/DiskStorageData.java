package com.bulkcloud.expansionae.feature.disk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.nbt.CompoundNBT;
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
        revisionCounter = 0;

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
                repaired = true;
                continue;
            }

            UUID uuid = diskTag.getUniqueId(TAG_UUID);
            if (uuidOccurrences.getOrDefault(uuid, 0) > 1) {
                quarantinedDuplicateRecords
                        .computeIfAbsent(uuid, ignored -> new ArrayList<>())
                        .add(diskTag.copy());
                continue;
            }

            ListNBT rawKeys = diskTag.getList(TAG_KEYS, 10);
            long[] rawAmounts = diskTag.getLongArray(TAG_AMOUNTS);

            int readableEntries = Math.min(rawKeys.size(), rawAmounts.length);
            if (rawKeys.size() != rawAmounts.length) {
                repaired = true;
            }

            ListNBT keys = new ListNBT();
            long[] amounts = new long[readableEntries];
            int writeIndex = 0;
            long itemCount = 0;

            for (int entryIndex = 0; entryIndex < readableEntries; entryIndex++) {
                long amount = rawAmounts[entryIndex];
                if (amount <= 0) {
                    repaired = true;
                    continue;
                }

                keys.add(rawKeys.getCompound(entryIndex).copy());
                amounts[writeIndex++] = amount;
                itemCount = saturatedAdd(itemCount, amount);
            }

            if (writeIndex != amounts.length) {
                amounts = Arrays.copyOf(amounts, writeIndex);
            }

            if (diskTag.getLong(TAG_ITEM_COUNT) != itemCount) {
                repaired = true;
            }

            // capacity=0 is the intentional legacy/unbound representation. Records
            // written before tier binding did not contain this tag and are bound once,
            // on first legitimate access by a DISK ItemStack.
            long capacity = Math.max(0, diskTag.getLong(TAG_CAPACITY));

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

        nbt.put(TAG_DISKS, list);
        return nbt;
    }

    public DiskRecord get(UUID uuid) {
        return disks.get(uuid);
    }

    public DiskRecord getOrCreate(UUID uuid) {
        return getOrCreate(uuid, 0);
    }

    public DiskRecord getOrCreate(UUID uuid, long capacity) {
        requireNotQuarantined(uuid);
        DiskRecord existing = disks.get(uuid);
        if (existing != null) {
            return existing.capacity == 0 && capacity > 0
                    ? bindCapacity(uuid, capacity)
                    : existing;
        }

        DiskRecord created =
                new DiskRecord(
                        new ListNBT(),
                        new long[0],
                        0,
                        Math.max(0, capacity),
                        nextRevision());
        disks.put(uuid, created);
        setDirty(true);
        return created;
    }

    public DiskRecord bindCapacity(UUID uuid, long capacity) {
        requireNotQuarantined(uuid);
        DiskRecord existing = disks.get(uuid);
        if (existing == null || existing.capacity != 0 || capacity <= 0) {
            return existing;
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
        requireNotQuarantined(uuid);
        long revision = nextRevision();
        disks.put(
                uuid,
                new DiskRecord(
                        (ListNBT) keys.copy(),
                        amounts.clone(),
                        itemCount,
                        Math.max(0, capacity),
                        revision));
        setDirty(true);
        return revision;
    }

    public void remove(UUID uuid) {
        boolean removed = disks.remove(uuid) != null;
        removed |= quarantinedDuplicateRecords.remove(uuid) != null;
        if (removed) {
            nextRevision();
            setDirty(true);
        }
    }

    private void requireNotQuarantined(UUID uuid) {
        if (quarantinedDuplicateRecords.containsKey(uuid)) {
            throw new IllegalStateException(
                    "DISK UUID " + uuid
                            + " has duplicate persisted backing records and is quarantined");
        }
    }

    private long nextRevision() {
        return ++revisionCounter;
    }

    private static long saturatedAdd(long left, long right) {
        if (right > 0 && left > Long.MAX_VALUE - right) {
            return Long.MAX_VALUE;
        }
        return left + right;
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
