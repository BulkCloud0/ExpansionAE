package dev.bulkcloud.expansionae;

public enum CanerMode {
    FILL,
    EMPTY;

    public CanerMode next() {
        return this == FILL ? EMPTY : FILL;
    }
}
