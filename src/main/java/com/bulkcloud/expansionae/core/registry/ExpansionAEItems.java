package com.bulkcloud.expansionae.core.registry;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.fml.RegistryObject;

import com.bulkcloud.expansionae.ExpansionAE;
import com.bulkcloud.expansionae.feature.disk.DiskStorageCellItem;

import appeng.api.config.FuzzyMode;
import appeng.api.config.Upgrades;
import appeng.items.contents.CellUpgrades;

public final class ExpansionAEItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ExpansionAE.MOD_ID);

    public static final RegistryObject<Item> DISK_1K = ITEMS.register(
            "1k_disk",
            () -> new DiskStorageCellItem(new Item.Properties(), 1000, 0.5));

    private ExpansionAEItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    public static void registerAE2Upgrades() {
        Item disk = DISK_1K.get();
        Upgrades.FUZZY.registerItem(disk, 1);
        Upgrades.INVERTER.registerItem(disk, 1);
    }

    public static void validateDiskWorkbenchContract() {
        Item registered = DISK_1K.get();
        if (!(registered instanceof DiskStorageCellItem)) {
            throw new IllegalStateException("1k DISK is not a DiskStorageCellItem");
        }

        DiskStorageCellItem disk = (DiskStorageCellItem) registered;
        ItemStack stack = new ItemStack(disk);

        if (!disk.isEditable(stack)) {
            throw new IllegalStateException("1k DISK is not editable in the Cell Workbench");
        }

        IItemHandler config = disk.getConfigInventory(stack);
        if (config.getSlots() != 63) {
            throw new IllegalStateException("1k DISK config inventory must expose exactly 63 slots");
        }

        IItemHandler rawUpgrades = disk.getUpgradesInventory(stack);
        if (!(rawUpgrades instanceof CellUpgrades)) {
            throw new IllegalStateException("1k DISK upgrade inventory is not an AE2 CellUpgrades inventory");
        }

        CellUpgrades upgrades = (CellUpgrades) rawUpgrades;
        if (upgrades.getSlots() != 2
                || upgrades.getMaxInstalled(Upgrades.FUZZY) != 1
                || upgrades.getMaxInstalled(Upgrades.INVERTER) != 1
                || upgrades.getMaxInstalled(Upgrades.CAPACITY) != 0) {
            throw new IllegalStateException("1k DISK upgrade support does not match the intended FUZZY/INVERTER contract");
        }

        disk.setFuzzyMode(stack, FuzzyMode.PERCENT_50);
        if (disk.getFuzzyMode(stack) != FuzzyMode.PERCENT_50) {
            throw new IllegalStateException("1k DISK fuzzy mode did not round-trip through ItemStack NBT");
        }

        ExpansionAE.LOGGER.info(
                "DISK workbench contract validated (63 config slots, 2 upgrade slots, FUZZY + INVERTER)");
    }
}
