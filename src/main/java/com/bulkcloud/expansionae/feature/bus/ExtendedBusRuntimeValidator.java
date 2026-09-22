package com.bulkcloud.expansionae.feature.bus;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import com.bulkcloud.expansionae.ExpansionAE;
import com.bulkcloud.expansionae.ae2.ExpansionAEApi;
import com.bulkcloud.expansionae.core.registry.ExpansionAEItems;

import appeng.api.config.Upgrades;
import appeng.api.parts.IPart;
import appeng.items.parts.PartItem;
import appeng.parts.automation.UpgradeInventory;

public final class ExtendedBusRuntimeValidator {
    private ExtendedBusRuntimeValidator() {
    }

    public static void validate() {
        validateExportBus();
        validateImportBus();

        ExpansionAE.LOGGER.info(
                "Extended IO bus contract validated "
                        + "(native AE2 GUI, 4 upgrade slots, 8x throughput, 0-4 Speed Cards)");
    }

    private static void validateExportBus() {
        IPart created = createPart(ExpansionAEItems.EXTENDED_EXPORT_BUS.get());
        if (!(created instanceof ExtendedExportBusPart)) {
            throw new IllegalStateException("Extended Export Bus item did not create ExtendedExportBusPart");
        }

        ExtendedExportBusPart part = (ExtendedExportBusPart) created;
        validateUpgradeContract(part.getInventoryByName("upgrades"), true, "Extended Export Bus");
        validateSpeedProgression(part, "Extended Export Bus");
    }

    private static void validateImportBus() {
        IPart created = createPart(ExpansionAEItems.EXTENDED_IMPORT_BUS.get());
        if (!(created instanceof ExtendedImportBusPart)) {
            throw new IllegalStateException("Extended Import Bus item did not create ExtendedImportBusPart");
        }

        ExtendedImportBusPart part = (ExtendedImportBusPart) created;
        validateUpgradeContract(part.getInventoryByName("upgrades"), false, "Extended Import Bus");
        validateSpeedProgression(part, "Extended Import Bus");
    }

    private static IPart createPart(Item item) {
        if (!(item instanceof PartItem)) {
            throw new IllegalStateException(item.getRegistryName() + " is not an AE2 PartItem");
        }

        @SuppressWarnings("rawtypes")
        PartItem partItem = (PartItem) item;
        return partItem.createPart(new ItemStack(item));
    }

    private static void validateUpgradeContract(
            IItemHandler rawUpgrades,
            boolean craftingExpected,
            String name) {
        if (!(rawUpgrades instanceof UpgradeInventory)) {
            throw new IllegalStateException(name + " does not expose AE2 UpgradeInventory");
        }

        UpgradeInventory upgrades = (UpgradeInventory) rawUpgrades;
        if (upgrades.getSlots() != 4
                || upgrades.getMaxInstalled(Upgrades.CAPACITY) != 2
                || upgrades.getMaxInstalled(Upgrades.SPEED) != 4
                || upgrades.getMaxInstalled(Upgrades.REDSTONE) != 1
                || upgrades.getMaxInstalled(Upgrades.FUZZY) != 1
                || upgrades.getMaxInstalled(Upgrades.CRAFTING) != (craftingExpected ? 1 : 0)
                || upgrades.getMaxInstalled(Upgrades.INVERTER) != 0) {
            throw new IllegalStateException(name + " upgrade contract does not match the AE2 8.4.7 bus contract");
        }
    }

    private static void validateSpeedProgression(ExtendedExportBusPart part, String name) {
        int[] expected = { 8, 64, 256, 512, 768 };
        validateSpeedProgression(
                part.getInventoryByName("upgrades"),
                part::calculatedItemsToSendForValidation,
                expected,
                name);
    }

    private static void validateSpeedProgression(ExtendedImportBusPart part, String name) {
        int[] expected = { 8, 64, 256, 512, 768 };
        validateSpeedProgression(
                part.getInventoryByName("upgrades"),
                part::calculatedItemsToSendForValidation,
                expected,
                name);
    }

    private static void validateSpeedProgression(
            IItemHandler upgrades,
            ThroughputReader reader,
            int[] expected,
            String name) {
        assertThroughput(name, reader.read(), expected[0], 0);

        for (int speedCards = 1; speedCards < expected.length; speedCards++) {
            ItemStack remainder = upgrades.insertItem(
                    speedCards - 1,
                    ExpansionAEApi.get().definitions().materials().cardSpeed().stack(1),
                    false);
            if (!remainder.isEmpty()) {
                throw new IllegalStateException(name + " rejected Speed Card #" + speedCards);
            }
            assertThroughput(name, reader.read(), expected[speedCards], speedCards);
        }
    }

    private static void assertThroughput(
            String name,
            int actual,
            int expected,
            int speedCards) {
        if (actual != expected) {
            throw new IllegalStateException(
                    name + " throughput mismatch with " + speedCards + " Speed Cards: "
                            + actual + " != " + expected);
        }
    }

    @FunctionalInterface
    private interface ThroughputReader {
        int read();
    }
}
