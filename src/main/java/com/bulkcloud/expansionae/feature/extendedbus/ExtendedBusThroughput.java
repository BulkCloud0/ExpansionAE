package com.bulkcloud.expansionae.feature.extendedbus;

public final class ExtendedBusThroughput {
    public static final int MULTIPLIER = 8;

    private ExtendedBusThroughput() {
    }

    public static int scaleBudget(int nativeBudget) {
        if (nativeBudget < 0) {
            throw new IllegalArgumentException("Native bus budget must not be negative");
        }

        long scaled = (long) nativeBudget * MULTIPLIER;
        return scaled > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) scaled;
    }
}
