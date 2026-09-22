package com.bulkcloud.expansionae.feature.disk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

        long revision = data.put(id, keys, new long[] { 37L }, 37L);
        assertTrue(revision > 0L);

        CompoundNBT saved = data.save(new CompoundNBT());

        DiskStorageData loaded = new DiskStorageData();
        loaded.load(saved);

        DiskStorageData.DiskRecord record = loaded.get(id);
        assertNotNull(record);
        assertEquals(37L, record.getItemCount());
        assertEquals(37L, record.getAmounts()[0]);
        assertEquals("minecraft:stone", record.getKeys().getCompound(0).getString("id"));
        assertTrue(record.getRevision() > 0L);
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

        CompoundNBT saved = data.save(new CompoundNBT());

        DiskStorageData loaded = new DiskStorageData();
        loaded.load(saved);

        DiskStorageData.DiskRecord record = loaded.get(id);
        assertNotNull(record);
        assertEquals(0L, record.getItemCount());
        assertEquals(0, record.getKeys().size());
        assertEquals(0, record.getAmounts().length);
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
        disk.putUUID("uuid", id);
        disk.put("keys", keys);
        disk.putLongArray("amounts", new long[] { 5L, -3L, 99L });
        disk.putLong("item_count", 12345L);

        ListNBT disks = new ListNBT();
        disks.add(disk);

        CompoundNBT root = new CompoundNBT();
        root.put("disks", disks);

        DiskStorageData loaded = new DiskStorageData();
        loaded.load(root);

        DiskStorageData.DiskRecord record = loaded.get(id);
        assertNotNull(record);
        assertEquals(5L, record.getItemCount());
        assertEquals(1, record.getKeys().size());
        assertEquals("minecraft:stone", record.getKeys().getCompound(0).getString("id"));
        assertEquals(1, record.getAmounts().length);
        assertEquals(5L, record.getAmounts()[0]);

        CompoundNBT normalized = loaded.save(new CompoundNBT());
        CompoundNBT normalizedDisk = normalized.getList("disks", 10).getCompound(0);
        assertEquals(5L, normalizedDisk.getLong("item_count"));
        assertEquals(1, normalizedDisk.getList("keys", 10).size());
        assertEquals(1, normalizedDisk.getLongArray("amounts").length);
    }
}
