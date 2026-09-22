package com.bulkcloud.expansionae.feature.bus;

import net.minecraft.item.ItemStack;

import appeng.parts.automation.ImportBusPart;

public final class ExtendedImportBusPart extends ImportBusPart {
    public static final int THROUGHPUT_MULTIPLIER = 8;

    public ExtendedImportBusPart(ItemStack stack) {
        super(stack);
    }

    @Override
    protected int calculateItemsToSend() {
        return Math.multiplyExact(super.calculateItemsToSend(), THROUGHPUT_MULTIPLIER);
    }

    int calculatedItemsToSendForValidation() {
        return calculateItemsToSend();
    }
}
