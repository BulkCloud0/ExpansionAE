package com.bulkcloud.expansionae.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.client.renderer.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.bulkcloud.expansionae.ExpansionAE;
import com.bulkcloud.expansionae.core.registry.ExpansionAEItems;
import com.bulkcloud.expansionae.feature.disk.DiskStorageCellItem;

import appeng.api.client.ICellModelRegistry;
import appeng.core.Api;

@Mod.EventBusSubscriber(
        modid = ExpansionAE.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public final class ExpansionAEClient {
    private ExpansionAEClient() {
    }

    @SubscribeEvent
    public static void onModelRegistry(ModelRegistryEvent event) {
        // AE2 8.4.x fires Forge model registration before AddonLoader announces
        // IAppEngApi through @AEAddon. Api.instance() explicitly documents this
        // as a supported exceptional case for early API access.
        ICellModelRegistry cells = Api.instance().client().cells();

        registerDiskModel(
                cells,
                ExpansionAEItems.DISK_1K.get(),
                new ResourceLocation("appliedenergistics2:block/drive/cells/1k_item_cell"));
        registerDiskModel(
                cells,
                ExpansionAEItems.DISK_4K.get(),
                new ResourceLocation("appliedenergistics2:block/drive/cells/4k_item_cell"));
        registerDiskModel(
                cells,
                ExpansionAEItems.DISK_16K.get(),
                new ResourceLocation("appliedenergistics2:block/drive/cells/16k_item_cell"));
        registerDiskModel(
                cells,
                ExpansionAEItems.DISK_64K.get(),
                new ResourceLocation("appliedenergistics2:block/drive/cells/64k_item_cell"));

        ExpansionAE.LOGGER.info(
                "Registered and queued 1k/4k/16k/64k DISK drive models with the AE2 client cell registry");
    }

    @SubscribeEvent
    public static void onModelBake(ModelBakeEvent event) {
        ICellModelRegistry cells = Api.instance().client().cells();

        validateBakedDiskModels(event, cells, ExpansionAEItems.DISK_1K.get());
        validateBakedDiskModels(event, cells, ExpansionAEItems.DISK_4K.get());
        validateBakedDiskModels(event, cells, ExpansionAEItems.DISK_16K.get());
        validateBakedDiskModels(event, cells, ExpansionAEItems.DISK_64K.get());
        validateDiskTooltips();

        ExpansionAE.LOGGER.info(
                "DISK client model bake validation passed (item inventory + ME Drive models)");
    }

    private static void validateDiskTooltips() {
        validateDiskTooltip((DiskStorageCellItem) ExpansionAEItems.DISK_1K.get());
        validateDiskTooltip((DiskStorageCellItem) ExpansionAEItems.DISK_4K.get());
        validateDiskTooltip((DiskStorageCellItem) ExpansionAEItems.DISK_16K.get());
        validateDiskTooltip((DiskStorageCellItem) ExpansionAEItems.DISK_64K.get());

        ExpansionAE.LOGGER.info(
                "DISK client tooltip validation passed (cached item/type counts + tier capacities)");
    }

    private static void validateDiskTooltip(DiskStorageCellItem disk) {
        ItemStack stack = new ItemStack(disk);
        stack.getOrCreateTag().putLong("expansionae_disk_item_count", 321L);
        stack.getOrCreateTag().putLong("expansionae_disk_type_count", 7L);

        List<ITextComponent> tooltip = new ArrayList<>();
        disk.addInformation(stack, null, tooltip, ITooltipFlag.TooltipFlags.NORMAL);

        if (tooltip.size() != 3) {
            throw new IllegalStateException(
                    "DISK tooltip must expose exactly 3 lines for " + disk.getRegistryName());
        }

        requireTranslation(
                tooltip.get(0),
                "tooltip.expansionae.disk.items",
                new long[] { 321L, disk.getCapacity() },
                disk);
        requireTranslation(
                tooltip.get(1),
                "tooltip.expansionae.disk.types",
                new long[] { 7L },
                disk);
        requireTranslation(
                tooltip.get(2),
                "tooltip.expansionae.disk.no_type_limit",
                new long[0],
                disk);
    }

    private static void requireTranslation(
            ITextComponent component,
            String expectedKey,
            long[] expectedNumericArgs,
            DiskStorageCellItem disk) {
        if (!(component instanceof TranslationTextComponent)) {
            throw new IllegalStateException(
                    "DISK tooltip line is not translatable for " + disk.getRegistryName());
        }

        TranslationTextComponent translated = (TranslationTextComponent) component;
        if (!expectedKey.equals(translated.getKey())) {
            throw new IllegalStateException(
                    "DISK tooltip key mismatch for "
                            + disk.getRegistryName()
                            + ": expected "
                            + expectedKey
                            + " but got "
                            + translated.getKey());
        }

        Object[] args = translated.getFormatArgs();
        if (args.length != expectedNumericArgs.length) {
            throw new IllegalStateException(
                    "DISK tooltip argument count mismatch for "
                            + disk.getRegistryName()
                            + " / "
                            + expectedKey);
        }

        for (int i = 0; i < args.length; i++) {
            if (!(args[i] instanceof Number)
                    || ((Number) args[i]).longValue() != expectedNumericArgs[i]) {
                throw new IllegalStateException(
                        "DISK tooltip argument mismatch for "
                                + disk.getRegistryName()
                                + " / "
                                + expectedKey
                                + " at index "
                                + i);
            }
        }
    }

    private static void validateBakedDiskModels(
            ModelBakeEvent event,
            ICellModelRegistry cells,
            Item item) {
        IBakedModel missing = event.getModelManager().getModel(
                new ResourceLocation(ExpansionAE.MOD_ID, "__missing_model_probe__"));

        ResourceLocation driveModel = cells.model(item);
        if (driveModel == null) {
            throw new IllegalStateException(
                    "No AE2 ME Drive model is registered for " + item.getRegistryName());
        }

        IBakedModel bakedDrive = event.getModelRegistry().get(driveModel);
        if (bakedDrive == null || bakedDrive == missing) {
            throw new IllegalStateException(
                    "AE2 ME Drive model was not baked for "
                            + item.getRegistryName() + ": " + driveModel);
        }

        ModelResourceLocation itemModel =
                new ModelResourceLocation(item.getRegistryName(), "inventory");
        IBakedModel bakedItem = event.getModelRegistry().get(itemModel);
        if (bakedItem == null || bakedItem == missing) {
            throw new IllegalStateException(
                    "Inventory model was not baked for "
                            + item.getRegistryName() + ": " + itemModel);
        }
    }

    private static void registerDiskModel(
            ICellModelRegistry cells,
            Item item,
            ResourceLocation model) {
        // AE2 8.4.x explicitly requires addon cell models to be queued for loading.
        // Registering only the item -> model mapping is not sufficient by API contract.
        ModelLoader.addSpecialModel(model);
        cells.registerModel(item, model);

        if (!model.equals(cells.model(item))) {
            throw new IllegalStateException(
                    "AE2 client cell model registry did not retain model " + model
                            + " for " + item.getRegistryName());
        }
    }
}
