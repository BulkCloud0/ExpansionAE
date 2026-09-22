package com.bulkcloud.expansionae.core.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import com.bulkcloud.expansionae.feature.disk.DiskStorageCellItem;

import appeng.api.config.FuzzyMode;
import appeng.api.config.Upgrades;
import appeng.items.contents.CellUpgrades;

final class DiskWorkbenchContractTest {

    @Test
    void diskMatchesAe2CellWorkbenchContract() {
        DiskStorageCellItem disk = new DiskStorageCellItem(new Item.Properties(), 1000, 0.5);
        ItemStack stack = new ItemStack(disk);

        assertTrue(disk.isEditable(stack));

        IItemHandler config = disk.getConfigInventory(stack);
        assertEquals(63, config.getSlots());

        CellUpgrades upgrades = (CellUpgrades) disk.getUpgradesInventory(stack);
        assertEquals(2, upgrades.getSlots());

        ExpansionAEItems.registerAE2Upgrades(disk);

        assertEquals(1, upgrades.getMaxInstalled(Upgrades.FUZZY));
        assertEquals(1, upgrades.getMaxInstalled(Upgrades.INVERTER));
        assertEquals(0, upgrades.getMaxInstalled(Upgrades.CAPACITY));

        disk.setFuzzyMode(stack, FuzzyMode.PERCENT_50);
        assertEquals(FuzzyMode.PERCENT_50, disk.getFuzzyMode(stack));
    }
}
