package dev.bulkcloud.expansionae;

import appeng.parts.automation.ExportBusPart;
import net.minecraft.item.ItemStack;

public final class FastExportBus extends ExportBusPart {
    public FastExportBus(ItemStack stack) { super(stack); }
    @Override protected int calculateItemsToSend() { return 8 * super.calculateItemsToSend(); }
}
