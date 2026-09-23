package com.bulkcloud.expansionae.feature.disk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;

final class DiskQuarantineExportTest {
    @TempDir
    Path tempDir;

    @Test
    void exportWritesMetadataAndDefensiveRawPayloadWithoutMutation() throws Exception {
        UUID id = UUID.randomUUID();
        CompoundNBT disk = new CompoundNBT();
        disk.putUniqueId("uuid", id);
        disk.putString("keys", "wrong-type");
        disk.putLongArray("amounts", new long[] { 9L });
        disk.putLong("item_count", 9L);
        disk.putLong("capacity", 1_000L);
        disk.putString("custom_debug_payload", "preserve-me");

        DiskStorageData data = loadSingle(disk);
        long revisionBefore = data.getRevisionCounterForDiagnostics();
        CompoundNBT persistedBefore = data.write(new CompoundNBT());

        DiskStorageData.QuarantineSnapshot snapshot =
                data.getQuarantineSnapshots(id).get(0);
        Path exported = DiskQuarantineExport.exportSnapshot(
                tempDir,
                snapshot,
                "disk-" + id);

        assertTrue(Files.isRegularFile(exported));
        String text = new String(
                Files.readAllBytes(exported),
                StandardCharsets.UTF_8);
        assertTrue(text.contains("reason=INVALID_KEYS_AMOUNTS"));
        assertTrue(text.contains("uuid=" + id));
        assertTrue(text.contains("custom_debug_payload"));
        assertTrue(text.contains("preserve-me"));
        assertTrue(text.contains("payload_snbt="));

        assertEquals(revisionBefore, data.getRevisionCounterForDiagnostics());
        assertEquals(persistedBefore, data.write(new CompoundNBT()));
    }

    @Test
    void repeatedExportNeverSilentlyOverwrites() throws Exception {
        UUID id = UUID.randomUUID();
        CompoundNBT disk = new CompoundNBT();
        disk.putUniqueId("uuid", id);
        disk.put("keys", new ListNBT());
        disk.putLongArray("amounts", new long[0]);
        disk.putLong("item_count", 0L);
        disk.putDouble("capacity", 1000.5D);

        DiskStorageData data = loadSingle(disk);
        DiskStorageData.QuarantineSnapshot snapshot =
                data.getQuarantineSnapshots(id).get(0);

        Path first = DiskQuarantineExport.exportSnapshot(
                tempDir,
                snapshot,
                "same-stem");
        Path second = DiskQuarantineExport.exportSnapshot(
                tempDir,
                snapshot,
                "same-stem");

        assertNotEquals(first, second);
        assertTrue(Files.exists(first));
        assertTrue(Files.exists(second));
        assertFalse(Files.isDirectory(first));
    }

    @Test
    void duplicateUuidExportsEveryRawRecord() throws Exception {
        UUID id = UUID.randomUUID();

        CompoundNBT first = validEmptyRecord(id, 1_000L);
        first.putString("marker", "first");
        CompoundNBT second = validEmptyRecord(id, 4_000L);
        second.putString("marker", "second");

        ListNBT disks = new ListNBT();
        disks.add(first);
        disks.add(second);

        CompoundNBT root = new CompoundNBT();
        root.put("disks", disks);

        DiskStorageData data = new DiskStorageData();
        data.read(root);

        List<Path> exported = DiskQuarantineExport.exportSnapshots(
                tempDir,
                data.getQuarantineSnapshots(id),
                "duplicate-" + id);

        assertEquals(2, exported.size());
        String firstText = new String(
                Files.readAllBytes(exported.get(0)),
                StandardCharsets.UTF_8);
        String secondText = new String(
                Files.readAllBytes(exported.get(1)),
                StandardCharsets.UTF_8);

        assertTrue(firstText.contains("duplicate_count=2"));
        assertTrue(secondText.contains("duplicate_count=2"));
        assertTrue(firstText.contains("first") || firstText.contains("second"));
        assertTrue(secondText.contains("first") || secondText.contains("second"));
        assertNotEquals(firstText, secondText);
    }

    @Test
    void globalRootExportPreservesRawPayload() throws Exception {
        CompoundNBT root = new CompoundNBT();
        root.putString("disks", "raw-global-root");

        DiskStorageData data = new DiskStorageData();
        data.read(root);
        DiskStorageData.QuarantineSnapshot global =
                data.getQuarantineSnapshots().get(0);

        Path exported = DiskQuarantineExport.exportSnapshot(
                tempDir,
                global,
                "global-root");
        String text = new String(
                Files.readAllBytes(exported),
                StandardCharsets.UTF_8);

        assertTrue(text.contains("reason=INVALID_ROOT_DISKS"));
        assertTrue(text.contains("raw-global-root"));
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
