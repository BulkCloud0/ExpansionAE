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
    public static final RegistryObject<Item> DISK_4K = ITEMS.register(
            "4k_disk",
            () -> new DiskStorageCellItem(new Item.Properties(), 4000, 1.0));
    public static final RegistryObject<Item> DISK_16K = ITEMS.register(
            "16k_disk",
            () -> new DiskStorageCellItem(new Item.Properties(), 16000, 1.5));
    public static final RegistryObject<Item> DISK_64K = ITEMS.register(
            "64k_disk",
            () -> new DiskStorageCellItem(new Item.Properties(), 64000, 2.0));

    private ExpansionAEItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    public static void registerAE2Upgrades() {
        registerDiskUpgrades(DISK_1K.get());
        registerDiskUpgrades(DISK_4K.get());
        registerDiskUpgrades(DISK_16K.get());
        registerDiskUpgrades(DISK_64K.get());
    }

    private static void registerDiskUpgrades(Item disk) {
        Upgrades.FUZZY.registerItem(disk, 1);
        Upgrades.INVERTER.registerItem(disk, 1);
    }

    public static void validateDiskWorkbenchContract() {
        validateDiskTier("1k", DISK_1K.get(), 1000, 0.5);
        validateDiskTier("4k", DISK_4K.get(), 4000, 1.0);
        validateDiskTier("16k", DISK_16K.get(), 16000, 1.5);
        validateDiskTier("64k", DISK_64K.get(), 64000, 2.0);

        ExpansionAE.LOGGER.info(
                "DISK workbench contract validated (tiers 1k/4k/16k/64k, 63 config slots, 2 upgrade slots, FUZZY + INVERTER)");
    }

    private static void validateDiskTier(
            String tier,
            Item registered,
            int expectedCapacity,
            double expectedIdleDrain) {
        if (!(registered instanceof DiskStorageCellItem)) {
            throw new IllegalStateException(tier + " DISK is not a DiskStorageCellItem");
        }

        DiskStorageCellItem disk = (DiskStorageCellItem) registered;
        if (disk.getCapacity() != expectedCapacity
                || Double.compare(disk.getIdleDrain(), expectedIdleDrain) != 0) {
            throw new IllegalStateException(tier + " DISK capacity/idle drain contract is incorrect");
        }

        ItemStack stack = new ItemStack(disk);

        if (!disk.isEditable(stack)) {
            throw new IllegalStateException(tier + " DISK is not editable in the Cell Workbench");
        }

        IItemHandler config = disk.getConfigInventory(stack);
        if (config.getSlots() != 63) {
            throw new IllegalStateException(tier + " DISK config inventory must expose exactly 63 slots");
        }

        IItemHandler rawUpgrades = disk.getUpgradesInventory(stack);
        if (!(rawUpgrades instanceof CellUpgrades)) {
            throw new IllegalStateException(tier + " DISK upgrade inventory is not an AE2 CellUpgrades inventory");
        }

        CellUpgrades upgrades = (CellUpgrades) rawUpgrades;
        if (upgrades.getSlots() != 2
                || upgrades.getMaxInstalled(Upgrades.FUZZY) != 1
                || upgrades.getMaxInstalled(Upgrades.INVERTER) != 1
                || upgrades.getMaxInstalled(Upgrades.CAPACITY) != 0) {
            throw new IllegalStateException(
                    tier + " DISK upgrade support does not match the intended FUZZY/INVERTER contract");
        }

        disk.setFuzzyMode(stack, FuzzyMode.PERCENT_50);
        if (disk.getFuzzyMode(stack) != FuzzyMode.PERCENT_50) {
            throw new IllegalStateException(tier + " DISK fuzzy mode did not round-trip through ItemStack NBT");
        }
    }
}
