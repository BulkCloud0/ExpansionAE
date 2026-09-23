package com.bulkcloud.expansionae.feature.disk;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.nbt.INBT;

final class DiskQuarantineExport {
    private static final int MAX_COLLISION_ATTEMPTS = 10_000;

    private DiskQuarantineExport() {
    }

    static List<Path> exportSnapshots(
            Path exportRoot,
            List<DiskStorageData.QuarantineSnapshot> snapshots,
            String stem) throws IOException {
        if (snapshots == null || snapshots.isEmpty()) {
            return Collections.emptyList();
        }

        List<Path> exported = new ArrayList<>();
        for (int index = 0; index < snapshots.size(); index++) {
            String indexedStem = snapshots.size() == 1
                    ? stem
                    : stem + "-record-" + (index + 1);
            exported.add(exportSnapshot(exportRoot, snapshots.get(index), indexedStem));
        }
        return Collections.unmodifiableList(exported);
    }

    static Path exportSnapshot(
            Path exportRoot,
            DiskStorageData.QuarantineSnapshot snapshot,
            String stem) throws IOException {
        if (exportRoot == null) {
            throw new IllegalArgumentException("Export root cannot be null");
        }
        if (snapshot == null) {
            throw new IllegalArgumentException("Quarantine snapshot cannot be null");
        }

        Files.createDirectories(exportRoot);
        String safeStem = sanitizeStem(stem);
        long timestamp = System.currentTimeMillis();

        Path file = null;
        for (int attempt = 0; attempt < MAX_COLLISION_ATTEMPTS; attempt++) {
            String suffix = attempt == 0 ? "" : "-" + attempt;
            Path candidate = exportRoot.resolve(
                    safeStem + "-" + timestamp + suffix + ".snbt");
            try {
                file = Files.createFile(candidate);
                break;
            } catch (FileAlreadyExistsException collision) {
                // Try the next deterministic collision suffix without overwriting.
            }
        }

        if (file == null) {
            throw new IOException(
                    "Could not allocate a unique quarantine export filename");
        }

        INBT payload = snapshot.getRawPayload();
        StringBuilder contents = new StringBuilder();
        contents.append("# ExpansionAE DISK quarantine export\n");
        contents.append("timestamp_utc=").append(Instant.now()).append('\n');
        contents.append("uuid=")
                .append(snapshot.getUuid() == null
                        ? "<global/anonymous>"
                        : snapshot.getUuid().toString())
                .append('\n');
        contents.append("reason=").append(snapshot.getReason().name()).append('\n');
        contents.append("duplicate_count=")
                .append(snapshot.getDuplicateCount())
                .append('\n');
        contents.append("payload_snbt=")
                .append(payload == null ? "<null>" : payload.toString())
                .append('\n');

        Files.write(
                file,
                contents.toString().getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.WRITE);
        return file;
    }

    private static String sanitizeStem(String stem) {
        String value = stem == null || stem.trim().isEmpty()
                ? "disk-quarantine"
                : stem.trim();
        return value.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
