package com.bulkcloud.expansionae.feature.extendedbus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class ExtendedBusThroughputTest {
    @Test
    void scalesNativeAe2BudgetsByEight() {
        int[] nativeBudgets = { 1, 8, 32, 64, 96 };
        int[] expected = { 8, 64, 256, 512, 768 };

        for (int i = 0; i < nativeBudgets.length; i++) {
            assertEquals(expected[i], ExtendedBusThroughput.scaleBudget(nativeBudgets[i]));
        }
    }

    @Test
    void saturatesInsteadOfOverflowing() {
        assertEquals(Integer.MAX_VALUE, ExtendedBusThroughput.scaleBudget(Integer.MAX_VALUE));
    }

    @Test
    void rejectsNegativeNativeBudget() {
        assertThrows(IllegalArgumentException.class, () -> ExtendedBusThroughput.scaleBudget(-1));
    }
}
