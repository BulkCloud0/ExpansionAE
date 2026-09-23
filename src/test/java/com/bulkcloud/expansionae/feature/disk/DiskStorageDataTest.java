package com.bulkcloud.expansionae.feature.disk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;

final class DiskStorageDataTest {

    @Test
    void roundTripPreservesDiskRecord() {
        DiskStorageData data = new DiskStorageData();
        UUID id = UUID.randomUUID();

        ListNBT keys = new ListNBT();
        CompoundNBT stone = new CompoundNBT();
        stone.putString("id", "minecraft:stone");
        keys.add(stone);

        long revision = data.put(id, keys, new long[] { 37L }, 37L, 4_000L);
        assertTrue(revision > 0L);

        CompoundNBT saved = data.write(new CompoundNBT());

        DiskStorageData loaded = new DiskStorageData();
        loaded.read(saved);

        DiskStorageData.DiskRecord record = loaded.get(id);
        assertNotNull(record);
        assertEquals(37L, record.getItemCount());
        assertEquals(4_000L, record.getCapacity());
        assertEquals(37L, record.getAmounts()[0]);
        assertEquals("minecraft:stone", record.getKeys().getCompound(0).getString("id"));
        assertTrue(record.getRevision() > 0L);
    }

    @Test
    void legacyRecordBindsCapacityOnlyOnce() {
        DiskStorageData data = new DiskStorageData();
        UUID id = UUID.randomUUID();

        data.put(id, new ListNBT(), new long[0], 0L);
        assertEquals(0L, data.get(id).getCapacity());

        DiskStorageData.DiskRecord bound = data.bindCapacity(id, 1_000L);
        assertNotNull(bound);
        assertEquals(1_000L, bound.getCapacity());

        DiskStorageData.DiskRecord secondAttempt = data.bindCapacity(id, 4_000L);
        assertNotNull(secondAttempt);
        assertEquals(1_000L, secondAttempt.getCapacity());
    }

    @Test
    void everyWriteGetsANewRuntimeRevision() {
        DiskStorageData data = new DiskStorageData();
        UUID id = UUID.randomUUID();
        ListNBT keys = new ListNBT();

        long first = data.put(id, keys, new long[0], 0L);
        long second = data.put(id, keys, new long[0], 0L);

        assertNotEquals(first, second);
        assertTrue(second > first);

        data.remove(id);
        assertNull(data.get(id));

        long third = data.put(id, keys, new long[0], 0L);
        assertTrue(third > second);
    }

    @Test
    void recordsExposeDefensiveCopies() {
        DiskStorageData data = new DiskStorageData();
        UUID id = UUID.randomUUID();

        ListNBT keys = new ListNBT();
        CompoundNBT key = new CompoundNBT();
        key.putString("id", "minecraft:diamond");
        keys.add(key);

        data.put(id, keys, new long[] { 4L }, 4L);
        DiskStorageData.DiskRecord record = data.get(id);
        assertNotNull(record);

        long[] amounts = record.getAmounts();
        amounts[0] = 999L;

        ListNBT copiedKeys = record.getKeys();
        copiedKeys.getCompound(0).putString("id", "minecraft:dirt");

        assertEquals(4L, record.getAmounts()[0]);
        assertEquals("minecraft:diamond", record.getKeys().getCompound(0).getString("id"));
    }


    @Test
    void emptyRecordSurvivesSaveAndLoadForSharedUuidAliases() {
        DiskStorageData data = new DiskStorageData();
        UUID id = UUID.randomUUID();

        data.put(id, new ListNBT(), new long[0], 0L);

        CompoundNBT saved = data.write(new CompoundNBT());

        DiskStorageData loaded = new DiskStorageData();
        loaded.read(saved);

        DiskStorageData.DiskRecord record = loaded.get(id);
        assertNotNull(record);
        assertEquals(0L, record.getItemCount());
        assertEquals(0, record.getKeys().size());
        assertEquals(0, record.getAmounts().length);
    }

    @Test
    void duplicateUuidRecordsAreQuarantinedAndPreserved() {
        UUID id = UUID.randomUUID();

        CompoundNBT first = new CompoundNBT();
        first.putUniqueId("uuid", id);
        first.put("keys", new ListNBT());
        first.putLongArray("amounts", new long[0]);
        first.putLong("item_count", 11L);
        first.putLong("capacity", 1_000L);

        CompoundNBT second = new CompoundNBT();
        second.putUniqueId("uuid", id);
        second.put("keys", new ListNBT());
        second.putLongArray("amounts", new long[0]);
        second.putLong("item_count", 22L);
        second.putLong("capacity", 4_000L);

        ListNBT disks = new ListNBT();
        disks.add(first);
        disks.add(second);

        CompoundNBT root = new CompoundNBT();
        root.put("disks", disks);

        DiskStorageData loaded = new DiskStorageData();
        loaded.read(root);

        assertNull(loaded.get(id));
        assertThrows(
                IllegalStateException.class,
                () -> loaded.put(id, new ListNBT(), new long[0], 0L, 1_000L));
        assertThrows(
                IllegalStateException.class,
                () -> loaded.getOrCreate(id, 1_000L));

        CompoundNBT preserved = loaded.write(new CompoundNBT());
        ListNBT preservedDisks = preserved.getList("disks", 10);
        assertEquals(2, preservedDisks.size());

        long itemCountTotal = 0L;
        for (int i = 0; i < preservedDisks.size(); i++) {
            CompoundNBT disk = preservedDisks.getCompound(i);
            assertEquals(id, disk.getUniqueId("uuid"));
            itemCountTotal += disk.getLong("item_count");
        }
        assertEquals(33L, itemCountTotal);
    }

    @Test
    void loadRepairsMismatchedAndInvalidAmounts() {
        UUID id = UUID.randomUUID();

        ListNBT keys = new ListNBT();
        CompoundNBT stone = new CompoundNBT();
        stone.putString("id", "minecraft:stone");
        keys.add(stone);

        CompoundNBT dirt = new CompoundNBT();
        dirt.putString("id", "minecraft:dirt");
        keys.add(dirt);

        CompoundNBT disk = new CompoundNBT();
        disk.putUniqueId("uuid", id);
        disk.put("keys", keys);
        disk.putLongArray("amounts", new long[] { 5L, -3L, 99L });
        disk.putLong("item_count", 12345L);

        ListNBT disks = new ListNBT();
        disks.add(disk);

        CompoundNBT root = new CompoundNBT();
        root.put("disks", disks);

        DiskStorageData loaded = new DiskStorageData();
        loaded.read(root);

        DiskStorageData.DiskRecord record = loaded.get(id);
        assertNotNull(record);
        assertEquals(5L, record.getItemCount());
        assertEquals(1, record.getKeys().size());
        assertEquals("minecraft:stone", record.getKeys().getCompound(0).getString("id"));
        assertEquals(1, record.getAmounts().length);
        assertEquals(5L, record.getAmounts()[0]);

        CompoundNBT normalized = loaded.write(new CompoundNBT());
        CompoundNBT normalizedDisk = normalized.getList("disks", 10).getCompound(0);
        assertEquals(5L, normalizedDisk.getLong("item_count"));
        assertEquals(1, normalizedDisk.getList("keys", 10).size());
        assertEquals(1, normalizedDisk.getLongArray("amounts").length);
    }
}
