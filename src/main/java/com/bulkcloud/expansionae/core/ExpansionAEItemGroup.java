package com.bulkcloud.expansionae.core;

import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;

import com.bulkcloud.expansionae.ExpansionAE;
import com.bulkcloud.expansionae.core.registry.ExpansionAEItems;

public final class ExpansionAEItemGroup {
    public static final ItemGroup MAIN = new ItemGroup(ExpansionAE.MOD_ID) {
        @Override
        public ItemStack createIcon() {
            return new ItemStack(ExpansionAEItems.DISK_1K.get());
        }
    };

    private ExpansionAEItemGroup() {
    }
}
