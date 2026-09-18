package dev.bulkcloud.expansionae;

public enum PreciseStorageMode {
    DEFAULT {
        @Override public boolean test(long value, long threshold) { return true; }
    },
    GREATER_EQUAL {
        @Override public boolean test(long value, long threshold) { return value >= threshold; }
    },
    GREATER {
        @Override public boolean test(long value, long threshold) { return value > threshold; }
    },
    EQUAL {
        @Override public boolean test(long value, long threshold) { return value == threshold; }
    },
    LESS {
        @Override public boolean test(long value, long threshold) { return value < threshold; }
    },
    LESS_EQUAL {
        @Override public boolean test(long value, long threshold) { return value <= threshold; }
    };

    public abstract boolean test(long value, long threshold);

    public PreciseStorageMode next() {
        PreciseStorageMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
