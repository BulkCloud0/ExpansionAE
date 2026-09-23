package com.bulkcloud.expansionae.feature.disk;

import net.minecraft.command.CommandSource;

final class DiskQuarantinePermissions {
    static final int READ_LEVEL = 2;
    static final int EXPORT_LEVEL = 3;
    static final int MUTATION_LEVEL = 4;

    private DiskQuarantinePermissions() {
    }

    static boolean canRead(CommandSource source) {
        return source != null && source.hasPermissionLevel(READ_LEVEL);
    }

    static boolean canExport(CommandSource source) {
        return source != null && source.hasPermissionLevel(EXPORT_LEVEL);
    }

    static boolean permitsReadLevel(int level) {
        return level >= READ_LEVEL;
    }

    static boolean permitsExportLevel(int level) {
        return level >= EXPORT_LEVEL;
    }

    static boolean permitsMutationLevel(int level) {
        return level >= MUTATION_LEVEL;
    }
}
