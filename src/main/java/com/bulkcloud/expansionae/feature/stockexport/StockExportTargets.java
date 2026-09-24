package com.bulkcloud.expansionae.feature.stockexport;

import net.minecraft.nbt.CompoundNBT;

public final class StockExportTargets {
    public static final int SLOT_COUNT = 9;
    public static final int DEFAULT_TARGET = 64;

    private static final String NBT_KEY = "stock_targets";

    private final int[] targets = new int[SLOT_COUNT];

    public StockExportTargets() {
        resetDefaults();
    }

    public int get(int slot) {
        checkSlot(slot);
        return this.targets[slot];
    }

    public void set(int slot, int target) {
        checkSlot(slot);
        if (target < 1) {
            throw new IllegalArgumentException("Stock target must be at least 1");
        }
        this.targets[slot] = target;
    }

    public long missingAmount(int slot, long currentAmount, long cycleBudget) {
        checkSlot(slot);
        if (currentAmount < 0) {
            throw new IllegalArgumentException("Current amount must not be negative");
        }
        if (cycleBudget < 0) {
            throw new IllegalArgumentException("Cycle budget must not be negative");
        }

        long missing = Math.max(0L, (long) this.targets[slot] - currentAmount);
        return Math.min(missing, cycleBudget);
    }

    public void writeToNBT(CompoundNBT tag) {
        tag.putIntArray(NBT_KEY, this.targets);
    }

    public void readFromNBT(CompoundNBT tag) {
        resetDefaults();

        if (!tag.contains(NBT_KEY)) {
            return;
        }

        int[] persisted = tag.getIntArray(NBT_KEY);
        int copyLength = Math.min(persisted.length, SLOT_COUNT);
        for (int i = 0; i < copyLength; i++) {
            if (persisted[i] > 0) {
                this.targets[i] = persisted[i];
            }
        }
    }

    private void resetDefaults() {
        for (int i = 0; i < SLOT_COUNT; i++) {
            this.targets[i] = DEFAULT_TARGET;
        }
    }

    private static void checkSlot(int slot) {
        if (slot < 0 || slot >= SLOT_COUNT) {
            throw new IndexOutOfBoundsException("Stock target slot out of range: " + slot);
        }
    }
}
