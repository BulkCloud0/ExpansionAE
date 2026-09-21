package dev.bulkcloud.expansionae;

import net.minecraftforge.items.IItemHandler;

/**
 * Provides the mode-specific local inventories used by the Expanded Crafting
 * Terminal independently from the object that exposes the ME network.
 */
public interface ExpandedCraftingInventoryHost {
    ExpandedCraftingMode getCraftingMode();
    void setCraftingMode(ExpandedCraftingMode mode);
    IItemHandler getModeInventory(ExpandedCraftingMode mode);
    IItemHandler getInventoryByName(String name);
}
