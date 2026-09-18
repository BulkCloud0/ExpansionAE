package dev.bulkcloud.expansionae;

import appeng.parts.automation.ImportBusPart;
import net.minecraft.item.ItemStack;

public final class FastImportBus extends ImportBusPart {
    public FastImportBus(ItemStack stack) { super(stack); }
    @Override protected int calculateItemsToSend() { return 8 * super.calculateItemsToSend(); }
}
