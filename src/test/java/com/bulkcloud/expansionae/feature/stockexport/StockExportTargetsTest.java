package com.bulkcloud.expansionae.feature.stockexport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.minecraft.nbt.CompoundNBT;
import org.junit.jupiter.api.Test;

final class StockExportTargetsTest {
    @Test
    void defaultsEverySlotToSixtyFour() {
        StockExportTargets targets = new StockExportTargets();

        for (int slot = 0; slot < StockExportTargets.SLOT_COUNT; slot++) {
            assertEquals(64, targets.get(slot));
        }
    }

    @Test
    void calculatesOnlyTheMissingAmountWithinBudget() {
        StockExportTargets targets = new StockExportTargets();
        targets.set(0, 64);

        assertEquals(64, targets.missingAmount(0, 0, 768));
        assertEquals(24, targets.missingAmount(0, 40, 768));
        assertEquals(0, targets.missingAmount(0, 64, 768));
        assertEquals(0, targets.missingAmount(0, 80, 768));
        assertEquals(8, targets.missingAmount(0, 0, 8));
    }

    @Test
    void persistsTargetsAndFallsBackSafelyForInvalidEntries() {
        StockExportTargets original = new StockExportTargets();
        original.set(0, 1);
        original.set(1, 4096);

        CompoundNBT tag = new CompoundNBT();
        original.writeToNBT(tag);

        StockExportTargets restored = new StockExportTargets();
        restored.readFromNBT(tag);

        assertEquals(1, restored.get(0));
        assertEquals(4096, restored.get(1));
        assertEquals(64, restored.get(8));

        CompoundNBT malformed = new CompoundNBT();
        malformed.putIntArray("stock_targets", new int[] { 0, -10, 32 });

        restored.readFromNBT(malformed);
        assertEquals(64, restored.get(0));
        assertEquals(64, restored.get(1));
        assertEquals(32, restored.get(2));
        assertEquals(64, restored.get(8));
    }

    @Test
    void rejectsInvalidInputs() {
        StockExportTargets targets = new StockExportTargets();

        assertThrows(IllegalArgumentException.class, () -> targets.set(0, 0));
        assertThrows(IllegalArgumentException.class, () -> targets.missingAmount(0, -1, 1));
        assertThrows(IllegalArgumentException.class, () -> targets.missingAmount(0, 1, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> targets.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> targets.get(9));
    }
}
