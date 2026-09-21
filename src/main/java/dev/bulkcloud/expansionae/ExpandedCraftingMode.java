package dev.bulkcloud.expansionae;

public enum ExpandedCraftingMode {
    CRAFTING,
    SMITHING,
    STONECUTTING,
    ANVIL;

    public static ExpandedCraftingMode byOrdinal(int ordinal) {
        ExpandedCraftingMode[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : CRAFTING;
    }

    public ExpandedCraftingMode next() {
        ExpandedCraftingMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
