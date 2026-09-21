package dev.bulkcloud.expansionae;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

/** Persists the wireless Expanded Crafting Terminal grids in the item NBT. */
final class WirelessCraftingInventoryHost implements ExpandedCraftingInventoryHost {
    private static final String TAG_MODE = "exCraftingMode";
    private static final String TAG_CRAFTING = "exCraftingGrid";
    private static final String TAG_SMITHING = "exSmithingGrid";
    private static final String TAG_STONECUTTING = "exStonecuttingGrid";
    private static final String TAG_ANVIL = "exAnvilGrid";

    private final ItemStack stack;
    private final PersistentInventory crafting = new PersistentInventory(9);
    private final PersistentInventory smithing = new PersistentInventory(2);
    private final PersistentInventory stonecutting = new PersistentInventory(1);
    private final PersistentInventory anvil = new PersistentInventory(2);
    private ExpandedCraftingMode mode = ExpandedCraftingMode.CRAFTING;
    private boolean loading;

    WirelessCraftingInventoryHost(ItemStack stack) {
        this.stack = stack;
        load();
    }

    private void load() {
        CompoundNBT tag = stack.getOrCreateTag();
        loading = true;
        try {
            if (tag.contains(TAG_CRAFTING, 10)) crafting.deserializeNBT(tag.getCompound(TAG_CRAFTING));
            if (tag.contains(TAG_SMITHING, 10)) smithing.deserializeNBT(tag.getCompound(TAG_SMITHING));
            if (tag.contains(TAG_STONECUTTING, 10)) stonecutting.deserializeNBT(tag.getCompound(TAG_STONECUTTING));
            if (tag.contains(TAG_ANVIL, 10)) anvil.deserializeNBT(tag.getCompound(TAG_ANVIL));
            mode = ExpandedCraftingMode.byOrdinal(tag.getInt(TAG_MODE));
        } finally {
            loading = false;
        }
    }

    private void save() {
        if (loading) return;
        CompoundNBT tag = stack.getOrCreateTag();
        tag.putInt(TAG_MODE, mode.ordinal());
        tag.put(TAG_CRAFTING, crafting.serializeNBT());
        tag.put(TAG_SMITHING, smithing.serializeNBT());
        tag.put(TAG_STONECUTTING, stonecutting.serializeNBT());
        tag.put(TAG_ANVIL, anvil.serializeNBT());
    }

    @Override public ExpandedCraftingMode getCraftingMode() { return mode; }

    @Override
    public void setCraftingMode(ExpandedCraftingMode mode) {
        ExpandedCraftingMode next = mode == null ? ExpandedCraftingMode.CRAFTING : mode;
        if (this.mode != next) {
            this.mode = next;
            save();
        }
    }

    @Override
    public IItemHandler getModeInventory(ExpandedCraftingMode requestedMode) {
        switch (requestedMode == null ? ExpandedCraftingMode.CRAFTING : requestedMode) {
            case SMITHING: return smithing;
            case STONECUTTING: return stonecutting;
            case ANVIL: return anvil;
            case CRAFTING:
            default: return crafting;
        }
    }

    @Override
    public IItemHandler getInventoryByName(String name) {
        if ("crafting".equals(name)) return crafting;
        if ("smithing".equals(name)) return smithing;
        if ("stonecutting".equals(name)) return stonecutting;
        if ("anvil".equals(name)) return anvil;
        return null;
    }

    private final class PersistentInventory extends ItemStackHandler {
        PersistentInventory(int size) { super(size); }
        @Override protected void onContentsChanged(int slot) { save(); }
    }
}
