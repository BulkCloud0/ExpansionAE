package com.bulkcloud.expansionae.feature.disk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;

final class DiskQuarantineDiagnosticsTest {
    @Test
    void invalidCapacitySnapshotIsReadOnlyAndDefensive() {
        UUID id = UUID.randomUUID();
        CompoundNBT disk = validEmptyRecord(id, 1_000L);
        disk.putDouble("capacity", 1000.75D);

        DiskStorageData data = loadSingle(disk);
        long revisionBefore = data.getRevisionCounterForDiagnostics();
        CompoundNBT persistedBefore = data.write(new CompoundNBT());

        List<DiskStorageData.QuarantineSnapshot> snapshots =
                data.getQuarantineSnapshots(id);

        assertEquals(1, snapshots.size());
        assertEquals(
                DiskStorageData.QuarantineReason.INVALID_CAPACITY_TAG,
                snapshots.get(0).getReason());
        assertNull(snapshots.get(0).getStoredCapacity());

        INBT raw = snapshots.get(0).getRawPayload();
        assertTrue(raw instanceof CompoundNBT);
        ((CompoundNBT) raw).putString("diagnostic_mutation", "must-not-leak");

        List<DiskStorageData.QuarantineSnapshot> secondRead =
                data.getQuarantineSnapshots(id);
        assertEquals(1, secondRead.size());
        assertFalse(
                ((CompoundNBT) secondRead.get(0).getRawPayload())
                        .contains("diagnostic_mutation"));

        assertEquals(revisionBefore, data.getRevisionCounterForDiagnostics());
        assertEquals(persistedBefore, data.write(new CompoundNBT()));
    }

    @Test
    void duplicateUuidProducesOneSnapshotPerRawRecord() {
        UUID id = UUID.randomUUID();
        ListNBT disks = new ListNBT();
        disks.add(validEmptyRecord(id, 1_000L));
        disks.add(validEmptyRecord(id, 4_000L));

        CompoundNBT root = new CompoundNBT();
        root.put("disks", disks);

        DiskStorageData data = new DiskStorageData();
        data.read(root);

        List<DiskStorageData.QuarantineSnapshot> snapshots =
                data.getQuarantineSnapshots(id);
        assertEquals(2, snapshots.size());
        for (DiskStorageData.QuarantineSnapshot snapshot : snapshots) {
            assertEquals(
                    DiskStorageData.QuarantineReason.DUPLICATE_PERSISTED_UUID,
                    snapshot.getReason());
            assertEquals(2, snapshot.getDuplicateCount());
            assertEquals(id, snapshot.getUuid());
        }
    }

    @Test
    void malformedRecordWithoutUuidIsVisibleToDiagnostics() {
        CompoundNBT malformed = new CompoundNBT();
        malformed.put("keys", new ListNBT());
        malformed.putLongArray("amounts", new long[0]);
        malformed.putLong("item_count", 0L);
        malformed.putLong("capacity", 1_000L);

        DiskStorageData data = loadSingle(malformed);
        List<DiskStorageData.QuarantineSnapshot> snapshots =
                data.getQuarantineSnapshots();

        assertEquals(1, snapshots.size());
        assertEquals(
                DiskStorageData.QuarantineReason.MISSING_PERSISTED_UUID,
                snapshots.get(0).getReason());
        assertNull(snapshots.get(0).getUuid());
    }

    @Test
    void invalidRootIsReportedWithoutInterpretingRecords() {
        CompoundNBT root = new CompoundNBT();
        root.putString("disks", "raw-root");

        DiskStorageData data = new DiskStorageData();
        data.read(root);

        List<DiskStorageData.QuarantineSnapshot> snapshots =
                data.getQuarantineSnapshots();
        assertEquals(1, snapshots.size());
        assertEquals(
                DiskStorageData.QuarantineReason.INVALID_ROOT_DISKS,
                snapshots.get(0).getReason());
        assertNull(snapshots.get(0).getUuid());
        assertNotNull(snapshots.get(0).getRawPayload());
    }

    @Test
    void negativeCapacityIsExposedAsFailClosedDiagnostic() {
        UUID id = UUID.randomUUID();
        DiskStorageData data = loadSingle(validEmptyRecord(id, -1L));

        List<DiskStorageData.QuarantineSnapshot> snapshots =
                data.getQuarantineSnapshots(id);
        assertEquals(1, snapshots.size());
        assertEquals(
                DiskStorageData.QuarantineReason.NEGATIVE_CAPACITY,
                snapshots.get(0).getReason());
        assertEquals(Long.valueOf(-1L), snapshots.get(0).getStoredCapacity());
        assertEquals(Long.valueOf(0L), snapshots.get(0).getItemCount());
        assertEquals(Integer.valueOf(0), snapshots.get(0).getTypeCount());
    }

    private static DiskStorageData loadSingle(CompoundNBT disk) {
        ListNBT disks = new ListNBT();
        disks.add(disk);

        CompoundNBT root = new CompoundNBT();
        root.put("disks", disks);

        DiskStorageData data = new DiskStorageData();
        data.read(root);
        return data;
    }

    private static CompoundNBT validEmptyRecord(UUID id, long capacity) {
        CompoundNBT disk = new CompoundNBT();
        disk.putUniqueId("uuid", id);
        disk.put("keys", new ListNBT());
        disk.putLongArray("amounts", new long[0]);
        disk.putLong("item_count", 0L);
        disk.putLong("capacity", capacity);
        return disk;
    }
}
