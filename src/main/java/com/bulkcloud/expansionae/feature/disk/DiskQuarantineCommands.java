package com.bulkcloud.expansionae.feature.disk;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.UUIDArgument;
import net.minecraft.util.text.StringTextComponent;

public final class DiskQuarantineCommands {
    private static final int PAGE_SIZE = 10;

    private DiskQuarantineCommands() {
    }

    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(
                Commands.literal("expansionae")
                        .requires(DiskQuarantinePermissions::canRead)
                        .then(Commands.literal("disk")
                                .then(Commands.literal("quarantine")
                                        .then(Commands.literal("list")
                                                .executes(context -> list(context.getSource(), 1))
                                                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                                        .executes(context -> list(
                                                                context.getSource(),
                                                                IntegerArgumentType.getInteger(
                                                                        context,
                                                                        "page")))))
                                        .then(Commands.literal("inspect")
                                                .then(Commands.argument("uuid", new UUIDArgument())
                                                        .executes(context -> inspect(
                                                                context.getSource(),
                                                                context.getArgument(
                                                                        "uuid",
                                                                        UUID.class)))))
                                        .then(Commands.literal("aliases")
                                                .then(Commands.argument("uuid", new UUIDArgument())
                                                        .executes(context -> aliases(
                                                                context.getSource(),
                                                                context.getArgument(
                                                                        "uuid",
                                                                        UUID.class)))))
                                        .then(Commands.literal("export")
                                                .requires(DiskQuarantinePermissions::canExport)
                                                .then(Commands.argument("uuid", new UUIDArgument())
                                                        .executes(context -> export(
                                                                context.getSource(),
                                                                context.getArgument(
                                                                        "uuid",
                                                                        UUID.class)))))
                                        .then(Commands.literal("export-global")
                                                .requires(DiskQuarantinePermissions::canExport)
                                                .executes(context -> exportGlobal(
                                                        context.getSource()))))));
    }

    private static int list(CommandSource source, int page) {
        DiskStorageData data = DiskStorageService.getCurrent();
        if (data == null) {
            source.sendFeedback(
                    new StringTextComponent("ExpansionAE DISK storage is not available."),
                    false);
            return 0;
        }

        List<DiskStorageData.QuarantineSnapshot> snapshots =
                collectSnapshots(data);
        if (snapshots.isEmpty()) {
            source.sendFeedback(
                    new StringTextComponent("No DISK quarantine diagnostics are present."),
                    false);
            return 1;
        }

        int pageCount = (snapshots.size() + PAGE_SIZE - 1) / PAGE_SIZE;
        if (page > pageCount) {
            source.sendFeedback(
                    new StringTextComponent(
                            "DISK quarantine page " + page
                                    + " is out of range; last page is " + pageCount + "."),
                    false);
            return 0;
        }

        source.sendFeedback(
                new StringTextComponent(
                        "DISK quarantine diagnostics page " + page + "/" + pageCount
                                + " (" + snapshots.size() + " entries)"),
                false);

        int start = (page - 1) * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, snapshots.size());
        for (int index = start; index < end; index++) {
            source.sendFeedback(
                    new StringTextComponent(formatSnapshot(snapshots.get(index))),
                    false);
        }
        return end - start;
    }

    private static int inspect(CommandSource source, UUID uuid) {
        DiskStorageData data = DiskStorageService.getCurrent();
        if (data == null) {
            source.sendFeedback(
                    new StringTextComponent("ExpansionAE DISK storage is not available."),
                    false);
            return 0;
        }

        List<DiskStorageData.QuarantineSnapshot> snapshots =
                collectSnapshots(data, uuid);
        if (snapshots.isEmpty()) {
            source.sendFeedback(
                    new StringTextComponent(
                            "No DISK quarantine diagnostic exists for UUID " + uuid + "."),
                    false);
            return 0;
        }

        source.sendFeedback(
                new StringTextComponent(
                        "DISK quarantine diagnostic for " + uuid
                                + " (" + snapshots.size() + " record(s))"),
                false);
        for (DiskStorageData.QuarantineSnapshot snapshot : snapshots) {
            source.sendFeedback(
                    new StringTextComponent(formatSnapshot(snapshot)),
                    false);
        }
        return snapshots.size();
    }



    private static int aliases(CommandSource source, UUID uuid) {
        List<DiskAliasNotifier.AliasSnapshot> aliases =
                DiskAliasNotifier.snapshotLoadedAliases(uuid);

        source.sendFeedback(
                new StringTextComponent(
                        "Loaded DISK alias views for " + uuid
                                + ": " + aliases.size()
                                + " (loaded/in-memory only; no chunks were loaded)"),
                false);

        if (aliases.isEmpty()) {
            return 1;
        }

        for (DiskAliasNotifier.AliasSnapshot alias : aliases) {
            String location = alias.hasLocation()
                    ? alias.getDimension()
                            + " @ "
                            + alias.getX() + ","
                            + alias.getY() + ","
                            + alias.getZ()
                    : "<location unavailable>";
            source.sendFeedback(
                    new StringTextComponent(
                            alias.getHostType()
                                    + " | active=" + value(alias.isActive())
                                    + " | " + location
                                    + " | slot=?"),
                    false);
        }

        return aliases.size();
    }

    private static int export(CommandSource source, UUID uuid) {
        DiskStorageData data = DiskStorageService.getCurrent();
        if (data == null) {
            source.sendFeedback(
                    new StringTextComponent("ExpansionAE DISK storage is not available."),
                    false);
            return 0;
        }

        List<DiskStorageData.QuarantineSnapshot> snapshots =
                collectSnapshots(data, uuid);
        if (snapshots.isEmpty()) {
            source.sendFeedback(
                    new StringTextComponent(
                            "No DISK quarantine diagnostic exists for UUID " + uuid + "."),
                    false);
            return 0;
        }

        try {
            Path exportRoot = Paths.get("expansionae-quarantine");
            List<Path> exported = DiskQuarantineExport.exportSnapshots(
                    exportRoot,
                    snapshots,
                    "disk-" + uuid.toString());
            for (Path path : exported) {
                source.sendFeedback(
                        new StringTextComponent(
                                "Exported DISK quarantine payload: "
                                        + path.toAbsolutePath().normalize()),
                        false);
            }
            return exported.size();
        } catch (IOException exception) {
            source.sendErrorMessage(
                    new StringTextComponent(
                            "Failed to export DISK quarantine payload for "
                                    + uuid + ": " + exception.getMessage()));
            return 0;
        }
    }

    private static int exportGlobal(CommandSource source) {
        DiskStorageData data = DiskStorageService.getCurrent();
        if (data == null) {
            source.sendFeedback(
                    new StringTextComponent("ExpansionAE DISK storage is not available."),
                    false);
            return 0;
        }

        DiskStorageData.QuarantineSnapshot global = null;
        for (DiskStorageData.QuarantineSnapshot snapshot
                : data.getQuarantineSnapshots()) {
            if (snapshot.getReason()
                    == DiskStorageData.QuarantineReason.INVALID_ROOT_DISKS) {
                global = snapshot;
                break;
            }
        }

        if (global == null) {
            source.sendFeedback(
                    new StringTextComponent(
                            "No global DISK root quarantine payload is present."),
                    false);
            return 0;
        }

        try {
            Path path = DiskQuarantineExport.exportSnapshot(
                    Paths.get("expansionae-quarantine"),
                    global,
                    "global-disks-root");
            source.sendFeedback(
                    new StringTextComponent(
                            "Exported global DISK quarantine payload: "
                                    + path.toAbsolutePath().normalize()),
                    false);
            return 1;
        } catch (IOException exception) {
            source.sendErrorMessage(
                    new StringTextComponent(
                            "Failed to export global DISK quarantine payload: "
                                    + exception.getMessage()));
            return 0;
        }
    }


    private static List<DiskStorageData.QuarantineSnapshot> collectSnapshots(
            DiskStorageData data) {
        List<DiskStorageData.QuarantineSnapshot> snapshots =
                new ArrayList<>(data.getQuarantineSnapshots());
        snapshots.addAll(DiskAliasNotifier.snapshotRuntimeDiagnostics());
        snapshots.sort(Comparator.comparing(
                DiskStorageData.QuarantineSnapshot::sortKey));
        return snapshots;
    }

    private static List<DiskStorageData.QuarantineSnapshot> collectSnapshots(
            DiskStorageData data,
            UUID uuid) {
        List<DiskStorageData.QuarantineSnapshot> snapshots =
                new ArrayList<>(data.getQuarantineSnapshots(uuid));
        snapshots.addAll(DiskAliasNotifier.snapshotRuntimeDiagnostics(uuid));
        snapshots.sort(Comparator.comparing(
                DiskStorageData.QuarantineSnapshot::sortKey));
        return snapshots;
    }

    private static String formatSnapshot(DiskStorageData.QuarantineSnapshot snapshot) {
        String id = snapshot.getUuid() == null ? "<global/anonymous>" : snapshot.getUuid().toString();
        return id
                + " | " + snapshot.getReason().name()
                + " | capacity=" + value(snapshot.getStoredCapacity())
                + " | expectedCapacity=" + value(snapshot.getExpectedCapacity())
                + " | itemCount=" + value(snapshot.getItemCount())
                + " | typeCount=" + value(snapshot.getTypeCount())
                + " | duplicateCount=" + snapshot.getDuplicateCount();
    }

    private static String value(Object value) {
        return value == null ? "?" : String.valueOf(value);
    }
}
