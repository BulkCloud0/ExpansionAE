package com.bulkcloud.expansionae.feature.disk;

import net.minecraft.util.math.BlockPos;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DiskAliasExposureTest {

    @Test
    void sameCoordinatesAndSlotInDifferentDimensionsRemainDistinct() {
        BlockPos pos = new BlockPos(12, 64, -7);

        DiskAliasExposure.Candidate overworld =
                new DiskAliasExposure.Candidate("minecraft:overworld", pos, 2);
        DiskAliasExposure.Candidate nether =
                new DiskAliasExposure.Candidate("minecraft:the_nether", pos, 2);

        assertNotEquals(overworld, nether);
        assertNotEquals(0, overworld.compareTo(nether));
        assertNotEquals(0, nether.compareTo(overworld));
        assertEquals(
                Integer.signum(overworld.compareTo(nether)),
                -Integer.signum(nether.compareTo(overworld)));
    }

    @Test
    void candidateOrderingStillUsesPositionAndSlotWithinDimension() {
        DiskAliasExposure.Candidate first =
                new DiskAliasExposure.Candidate(
                        "minecraft:overworld",
                        new BlockPos(1, 64, 1),
                        0);
        DiskAliasExposure.Candidate laterPosition =
                new DiskAliasExposure.Candidate(
                        "minecraft:overworld",
                        new BlockPos(2, 64, 1),
                        0);
        DiskAliasExposure.Candidate laterSlot =
                new DiskAliasExposure.Candidate(
                        "minecraft:overworld",
                        new BlockPos(1, 64, 1),
                        1);
        DiskAliasExposure.Candidate equal =
                new DiskAliasExposure.Candidate(
                        "minecraft:overworld",
                        new BlockPos(1, 64, 1),
                        0);

        assertTrue(first.compareTo(laterPosition) < 0);
        assertTrue(first.compareTo(laterSlot) < 0);
        assertEquals(first, equal);
        assertEquals(first.hashCode(), equal.hashCode());
    }
}
