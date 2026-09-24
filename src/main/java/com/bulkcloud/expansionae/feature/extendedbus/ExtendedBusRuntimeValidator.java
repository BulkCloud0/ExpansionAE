package com.bulkcloud.expansionae.feature.extendedbus;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import com.bulkcloud.expansionae.ExpansionAE;
import com.bulkcloud.expansionae.core.registry.ExpansionAEItems;
import com.bulkcloud.expansionae.feature.stockexport.StockExportBusPart;

import appeng.api.config.Upgrades;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartItem;

public final class ExtendedBusRuntimeValidator {
    private static final int[] NATIVE_BUDGETS = { 1, 8, 32, 64, 96 };
    private static final int[] EXPECTED_BUDGETS = { 8, 64, 256, 512, 768 };

    private ExtendedBusRuntimeValidator() {
    }

    public static void validate() {
        if (FMLEnvironment.production) {
            return;
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            throw new IllegalStateException(
                    "Dedicated server is not available for 8x item bus validation");
        }

        Item importBus = ExpansionAEItems.EXTENDED_IMPORT_BUS.get();
        Item exportBus = ExpansionAEItems.EXTENDED_EXPORT_BUS.get();
        Item stockExportBus = ExpansionAEItems.STOCK_EXPORT_BUS.get();

        requirePart(importBus, ExpansionImportBusPart.class, "import");
        requirePart(exportBus, ExpansionExportBusPart.class, "export");
        requirePart(stockExportBus, StockExportBusPart.class, "stock export");

        if (ExpansionImportBusPart.UPGRADE_SLOTS != 4
                || ExpansionExportBusPart.UPGRADE_SLOTS != 4) {
            throw new IllegalStateException("8x item buses must expose exactly four upgrade slots");
        }

        requireUpgrade(importBus, Upgrades.FUZZY, 1, "import");
        requireUpgrade(importBus, Upgrades.REDSTONE, 1, "import");
        requireUpgrade(importBus, Upgrades.CAPACITY, 2, "import");
        requireUpgrade(importBus, Upgrades.SPEED, 4, "import");
        requireUpgrade(importBus, Upgrades.CRAFTING, 0, "import");

        requireUpgrade(exportBus, Upgrades.FUZZY, 1, "export");
        requireUpgrade(exportBus, Upgrades.REDSTONE, 1, "export");
        requireUpgrade(exportBus, Upgrades.CAPACITY, 2, "export");
        requireUpgrade(exportBus, Upgrades.SPEED, 4, "export");
        requireUpgrade(exportBus, Upgrades.CRAFTING, 1, "export");

        requireUpgrade(stockExportBus, Upgrades.FUZZY, 1, "stock export");
        requireUpgrade(stockExportBus, Upgrades.REDSTONE, 1, "stock export");
        requireUpgrade(stockExportBus, Upgrades.CAPACITY, 2, "stock export");
        requireUpgrade(stockExportBus, Upgrades.SPEED, 4, "stock export");
        requireUpgrade(stockExportBus, Upgrades.CRAFTING, 0, "stock export");

        for (int i = 0; i < NATIVE_BUDGETS.length; i++) {
            int actual = ExtendedBusThroughput.scaleBudget(NATIVE_BUDGETS[i]);
            if (actual != EXPECTED_BUDGETS[i]) {
                throw new IllegalStateException(
                        "8x item bus budget mismatch at SPEED=" + i
                                + ": expected " + EXPECTED_BUDGETS[i]
                                + " but got " + actual);
            }
        }

        validateRecipe(server, "extended_import_bus", importBus);
        validateRecipe(server, "extended_export_bus", exportBus);
        validateRecipe(server, "stock_export_bus", stockExportBus);

        ExpansionAE.LOGGER.info(
                "8x item bus runtime contract validated "
                        + "(part factories, 4 upgrade slots, native upgrade matrix, "
                        + "8/64/256/512/768 budgets, crafting recipes)");
        ExpansionAE.LOGGER.info(
                "Stock Export Bus runtime contract validated "
                        + "(part factory, FUZZY/REDSTONE/CAPACITY/SPEED upgrades, "
                        + "CRAFTING disabled for MVP, crafting recipe)");
    }

    private static void requirePart(
            Item item,
            Class<? extends IPart> expectedType,
            String label) {
        if (!(item instanceof IPartItem)) {
            throw new IllegalStateException("8x " + label + " bus item is not an AE2 IPartItem");
        }

        IPart part = ((IPartItem<?>) item).createPart(new ItemStack(item));
        if (!expectedType.isInstance(part)) {
            throw new IllegalStateException(
                    "8x " + label + " bus PartItem factory returned "
                            + (part == null ? "null" : part.getClass().getName()));
        }
    }

    private static void requireUpgrade(
            Item item,
            Upgrades upgrade,
            int expected,
            String label) {
        int actual = 0;
        for (Upgrades.Supported supported : upgrade.getSupported()) {
            if (supported.isSupported(item)) {
                actual = Math.max(actual, supported.getMaxCount());
            }
        }

        if (actual != expected) {
            throw new IllegalStateException(
                    "8x " + label + " bus upgrade contract mismatch for "
                            + upgrade + ": expected " + expected + " but got " + actual);
        }
    }

    private static void validateRecipe(
            MinecraftServer server,
            String recipePath,
            Item expectedOutput) {
        ResourceLocation id = new ResourceLocation(ExpansionAE.MOD_ID, recipePath);
        IRecipe<?> recipe = server.getRecipeManager()
                .getRecipe(id)
                .orElseThrow(() -> new IllegalStateException("Missing 8x item bus recipe " + id));

        if (recipe.getType() != IRecipeType.CRAFTING) {
            throw new IllegalStateException("8x item bus recipe " + id + " is not a crafting recipe");
        }

        ItemStack output = recipe.getRecipeOutput();
        if (output.isEmpty() || output.getItem() != expectedOutput || output.getCount() != 1) {
            throw new IllegalStateException(
                    "8x item bus recipe " + id + " has unexpected output " + output);
        }
    }
}
