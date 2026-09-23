package com.bulkcloud.expansionae.feature.disk;

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
                        .requires(source -> source.hasPermissionLevel(2))
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
                                                                UUIDArgument.getUuid(
                                                                        context,
                                                                        "uuid"))))))));
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
                data.getQuarantineSnapshots();
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
                data.getQuarantineSnapshots(uuid);
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

    private static String formatSnapshot(DiskStorageData.QuarantineSnapshot snapshot) {
        String id = snapshot.getUuid() == null ? "<global/anonymous>" : snapshot.getUuid().toString();
        return id
                + " | " + snapshot.getReason().name()
                + " | capacity=" + value(snapshot.getStoredCapacity())
                + " | itemCount=" + value(snapshot.getItemCount())
                + " | typeCount=" + value(snapshot.getTypeCount())
                + " | duplicateCount=" + snapshot.getDuplicateCount();
    }

    private static String value(Object value) {
        return value == null ? "?" : String.valueOf(value);
    }
}
