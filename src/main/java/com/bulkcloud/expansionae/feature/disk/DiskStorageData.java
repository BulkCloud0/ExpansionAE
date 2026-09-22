package com.bulkcloud.expansionae.feature.disk;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.WorldSavedData;

public final class DiskStorageData extends WorldSavedData {
    public static final String DATA_NAME = "expansionae_disk_storage";

    private static final String TAG_DISKS = "disks";
    private static final String TAG_UUID = "uuid";
    private static final String TAG_KEYS = "keys";
    private static final String TAG_AMOUNTS = "amounts";
    private static final String TAG_ITEM_COUNT = "item_count";

    private final Map<UUID, DiskRecord> disks = new HashMap<>();

    public DiskStorageData() {
        super(DATA_NAME);
    }

    public static DiskStorageData get(ServerWorld world) {
        return world.getDataStorage().get(DiskStorageData::new, DATA_NAME);
    }

    @Override
    public void load(CompoundNBT nbt) {
        disks.clear();

        ListNBT list = nbt.getList(TAG_DISKS, 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundNBT diskTag = list.getCompound(i);
            if (!diskTag.hasUUID(TAG_UUID)) {
                continue;
            }

            UUID uuid = diskTag.getUUID(TAG_UUID);
            ListNBT keys = diskTag.getList(TAG_KEYS, 10);
            long[] amounts = diskTag.getLongArray(TAG_AMOUNTS);
            long itemCount = diskTag.getLong(TAG_ITEM_COUNT);

            disks.put(uuid, new DiskRecord(keys, amounts, itemCount));
        }
    }

    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        ListNBT list = new ListNBT();

        for (Map.Entry<UUID, DiskRecord> entry : disks.entrySet()) {
            CompoundNBT diskTag = new CompoundNBT();
            diskTag.putUUID(TAG_UUID, entry.getKey());

            DiskRecord record = entry.getValue();
            diskTag.put(TAG_KEYS, record.keys.copy());
            diskTag.putLongArray(TAG_AMOUNTS, record.amounts.clone());
            diskTag.putLong(TAG_ITEM_COUNT, record.itemCount);

            list.add(diskTag);
        }

        nbt.put(TAG_DISKS, list);
        return nbt;
    }

    public DiskRecord get(UUID uuid) {
        return disks.get(uuid);
    }

    public DiskRecord getOrCreate(UUID uuid) {
        return disks.computeIfAbsent(uuid, ignored -> {
            setDirty();
            return new DiskRecord(new ListNBT(), new long[0], 0);
        });
    }

    public void put(UUID uuid, ListNBT keys, long[] amounts, long itemCount) {
        disks.put(uuid, new DiskRecord((ListNBT) keys.copy(), amounts.clone(), itemCount));
        setDirty();
    }

    public void remove(UUID uuid) {
        if (disks.remove(uuid) != null) {
            setDirty();
        }
    }

    public static final class DiskRecord {
        private final ListNBT keys;
        private final long[] amounts;
        private final long itemCount;

        private DiskRecord(ListNBT keys, long[] amounts, long itemCount) {
            this.keys = keys;
            this.amounts = amounts;
            this.itemCount = itemCount;
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
    }
}
