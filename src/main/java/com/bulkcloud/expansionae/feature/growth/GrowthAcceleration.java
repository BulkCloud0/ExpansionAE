package com.bulkcloud.expansionae.feature.growth;

public final class GrowthAcceleration {
    public static final int NATIVE_SINGLE_ACCELERATOR_PROGRESS = 40;

    private GrowthAcceleration() {
    }

    public static int addWeightedProgress(int nativeProgress, int extraWeight) {
        if (nativeProgress < 0) {
            throw new IllegalArgumentException("nativeProgress must be >= 0");
        }
        if (extraWeight <= 0) {
            return nativeProgress;
        }

        long result = (long) nativeProgress
                + (long) extraWeight * NATIVE_SINGLE_ACCELERATOR_PROGRESS;
        return result >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) result;
    }
}
