package com.bulkcloud.expansionae.core.registry;

import java.util.Arrays;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import com.bulkcloud.expansionae.ExpansionAE;
import com.bulkcloud.expansionae.core.ExpansionAEItemGroup;
import com.bulkcloud.expansionae.feature.disk.DiskStorageCellItem;

import appeng.api.config.FuzzyMode;
import appeng.api.config.Upgrades;
import appeng.items.contents.CellUpgrades;

public final class ExpansionAEItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ExpansionAE.MOD_ID);

    public static final RegistryObject<Item> DISK_1K =
            registerDisk("1k_disk", 1_000, 0.5);
    public static final RegistryObject<Item> DISK_4K =
            registerDisk("4k_disk", 4_000, 1.0);
    public static final RegistryObject<Item> DISK_16K =
            registerDisk("16k_disk", 16_000, 1.5);
    public static final RegistryObject<Item> DISK_64K =
            registerDisk("64k_disk", 64_000, 2.0);

    private static final List<RegistryObject<Item>> DISKS = Arrays.asList(
            DISK_1K,
            DISK_4K,
            DISK_16K,
            DISK_64K);

    private ExpansionAEItems() {
    }

    private static RegistryObject<Item> registerDisk(
            String registryName,
            int capacity,
            double idleDrain) {
        return ITEMS.register(
                registryName,
                () -> new DiskStorageCellItem(
                        new Item.Properties().group(ExpansionAEItemGroup.MAIN),
                        capacity,
                        idleDrain));
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    public static void registerAE2Upgrades() {
        for (RegistryObject<Item> disk : DISKS) {
            Item item = disk.get();
            Upgrades.FUZZY.registerItem(item, 1);
            Upgrades.INVERTER.registerItem(item, 1);
        }
    }

    public static void validateDiskWorkbenchContract() {
        validateDiskWorkbenchContract(DISK_1K.get(), 1_000, 0.5, "1k");
        validateDiskWorkbenchContract(DISK_4K.get(), 4_000, 1.0, "4k");
        validateDiskWorkbenchContract(DISK_16K.get(), 16_000, 1.5, "16k");
        validateDiskWorkbenchContract(DISK_64K.get(), 64_000, 2.0, "64k");

        ExpansionAE.LOGGER.info(
                "DISK workbench contract validated for 1k/4k/16k/64k "
                        + "(63 config slots, 2 upgrade slots, FUZZY + INVERTER)");
    }

    private static void validateDiskWorkbenchContract(
            Item registered,
            int expectedCapacity,
            double expectedIdleDrain,
            String tier) {
        if (!(registered instanceof DiskStorageCellItem)) {
            throw new IllegalStateException(tier + " DISK is not a DiskStorageCellItem");
        }

        DiskStorageCellItem disk = (DiskStorageCellItem) registered;
        if (disk.getCapacity() != expectedCapacity) {
            throw new IllegalStateException(
                    tier + " DISK capacity mismatch: " + disk.getCapacity());
        }
        if (Double.compare(disk.getIdleDrain(), expectedIdleDrain) != 0) {
            throw new IllegalStateException(
                    tier + " DISK idle drain mismatch: " + disk.getIdleDrain());
        }

        ItemStack stack = new ItemStack(disk);

        if (!disk.isEditable(stack)) {
            throw new IllegalStateException(tier + " DISK is not editable in the Cell Workbench");
        }

        IItemHandler config = disk.getConfigInventory(stack);
        if (config.getSlots() != 63) {
            throw new IllegalStateException(
                    tier + " DISK config inventory must expose exactly 63 slots");
        }

        IItemHandler rawUpgrades = disk.getUpgradesInventory(stack);
        if (!(rawUpgrades instanceof CellUpgrades)) {
            throw new IllegalStateException(
                    tier + " DISK upgrade inventory is not an AE2 CellUpgrades inventory");
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
            throw new IllegalStateException(
                    tier + " DISK fuzzy mode did not round-trip through ItemStack NBT");
        }
    }
}
