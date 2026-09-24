package com.bulkcloud.expansionae.feature.disk;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class DiskQuarantinePermissionsTest {
    @Test
    void readDiagnosticsRequireOperatorLevelTwo() {
        assertFalse(DiskQuarantinePermissions.permitsReadLevel(0));
        assertFalse(DiskQuarantinePermissions.permitsReadLevel(1));
        assertTrue(DiskQuarantinePermissions.permitsReadLevel(2));
        assertTrue(DiskQuarantinePermissions.permitsReadLevel(3));
        assertTrue(DiskQuarantinePermissions.permitsReadLevel(4));
    }

    @Test
    void rawExportRequiresOperatorLevelThree() {
        assertFalse(DiskQuarantinePermissions.permitsExportLevel(0));
        assertFalse(DiskQuarantinePermissions.permitsExportLevel(1));
        assertFalse(DiskQuarantinePermissions.permitsExportLevel(2));
        assertTrue(DiskQuarantinePermissions.permitsExportLevel(3));
        assertTrue(DiskQuarantinePermissions.permitsExportLevel(4));
    }

    @Test
    void futureMutationIsReservedForLevelFour() {
        assertFalse(DiskQuarantinePermissions.permitsMutationLevel(0));
        assertFalse(DiskQuarantinePermissions.permitsMutationLevel(1));
        assertFalse(DiskQuarantinePermissions.permitsMutationLevel(2));
        assertFalse(DiskQuarantinePermissions.permitsMutationLevel(3));
        assertTrue(DiskQuarantinePermissions.permitsMutationLevel(4));
    }
}
