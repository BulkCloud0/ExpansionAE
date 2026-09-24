package com.bulkcloud.expansionae.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.client.renderer.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

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
                new ResourceLocation(ExpansionAE.MOD_ID, "block/drive/cells/1k_disk"));
        registerDiskModel(
                cells,
                ExpansionAEItems.DISK_4K.get(),
                new ResourceLocation(ExpansionAE.MOD_ID, "block/drive/cells/4k_disk"));
        registerDiskModel(
                cells,
                ExpansionAEItems.DISK_16K.get(),
                new ResourceLocation(ExpansionAE.MOD_ID, "block/drive/cells/16k_disk"));
        registerDiskModel(
                cells,
                ExpansionAEItems.DISK_64K.get(),
                new ResourceLocation(ExpansionAE.MOD_ID, "block/drive/cells/64k_disk"));

        ExpansionAE.LOGGER.info(
                "Registered and queued 1k/4k/16k/64k DISK drive models with the AE2 client cell registry");
    }

    @SubscribeEvent
    public static void onModelBake(ModelBakeEvent event) {
        if (FMLEnvironment.production) {
            return;
        }

        ICellModelRegistry cells = Api.instance().client().cells();

        validateBakedDiskModels(event, cells, ExpansionAEItems.DISK_1K.get());
        validateBakedDiskModels(event, cells, ExpansionAEItems.DISK_4K.get());
        validateBakedDiskModels(event, cells, ExpansionAEItems.DISK_16K.get());
        validateBakedDiskModels(event, cells, ExpansionAEItems.DISK_64K.get());

        ModelBounds oneKBounds = driveModelBounds(event, cells, ExpansionAEItems.DISK_1K.get());
        ModelBounds fourKBounds = driveModelBounds(event, cells, ExpansionAEItems.DISK_4K.get());
        ModelBounds sixteenKBounds = driveModelBounds(event, cells, ExpansionAEItems.DISK_16K.get());
        ModelBounds sixtyFourKBounds = driveModelBounds(event, cells, ExpansionAEItems.DISK_64K.get());

        requireSameBounds(oneKBounds, fourKBounds, "1k", "4k");
        requireSameBounds(oneKBounds, sixteenKBounds, "1k", "16k");
        requireSameBounds(oneKBounds, sixtyFourKBounds, "1k", "64k");

        validateDiskTooltips();

        ExpansionAE.LOGGER.info(
                "DISK client model bake validation passed (item inventory + ME Drive models)");
        ExpansionAE.LOGGER.info(
                "DISK client visual geometry validation passed "
                        + "(all Drive tiers share identical in-bounds geometry)");
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
        String resolved = component.getString();
        if (resolved == null
                || resolved.trim().isEmpty()
                || resolved.contains(expectedKey)
                || resolved.length() > 120) {
            throw new IllegalStateException(
                    "DISK tooltip did not resolve to sane readable text for "
                            + disk.getRegistryName()
                            + " / "
                            + expectedKey
                            + ": "
                            + resolved);
        }
    }

    private static ModelBounds driveModelBounds(
            ModelBakeEvent event,
            ICellModelRegistry cells,
            Item item) {
        ResourceLocation driveModel = cells.model(item);
        if (driveModel == null) {
            throw new IllegalStateException(
                    "Cannot inspect Drive geometry without registered model for "
                            + item.getRegistryName());
        }

        IBakedModel model = event.getModelRegistry().get(driveModel);
        if (model == null) {
            throw new IllegalStateException(
                    "Cannot inspect missing Drive geometry for " + item.getRegistryName());
        }

        ModelBounds bounds = new ModelBounds();
        collectBounds(model.getQuads(null, null, new Random(0L)), bounds);
        for (Direction direction : Direction.values()) {
            collectBounds(model.getQuads(null, direction, new Random(0L)), bounds);
        }

        if (bounds.vertices == 0) {
            throw new IllegalStateException(
                    "Drive model contains no baked vertices for " + item.getRegistryName());
        }

        bounds.requireInsideUnitCube(item);
        return bounds;
    }

    private static void collectBounds(List<BakedQuad> quads, ModelBounds bounds) {
        for (BakedQuad quad : quads) {
            int[] data = quad.getVertexData();
            if (data.length == 0 || data.length % 4 != 0) {
                throw new IllegalStateException(
                        "Unexpected baked quad vertex-data length: " + data.length);
            }

            int stride = data.length / 4;
            if (stride < 3) {
                throw new IllegalStateException(
                        "Baked quad vertex stride is too small: " + stride);
            }

            for (int vertex = 0; vertex < 4; vertex++) {
                int offset = vertex * stride;
                bounds.include(
                        Float.intBitsToFloat(data[offset]),
                        Float.intBitsToFloat(data[offset + 1]),
                        Float.intBitsToFloat(data[offset + 2]));
            }
        }
    }

    private static void requireSameBounds(
            ModelBounds expected,
            ModelBounds actual,
            String expectedTier,
            String actualTier) {
        if (!expected.sameAs(actual, 0.00001f)) {
            throw new IllegalStateException(
                    "Drive model alignment differs between "
                            + expectedTier
                            + " and "
                            + actualTier
                            + ": "
                            + expected
                            + " vs "
                            + actual);
        }
    }

    private static final class ModelBounds {
        private float minX = Float.POSITIVE_INFINITY;
        private float minY = Float.POSITIVE_INFINITY;
        private float minZ = Float.POSITIVE_INFINITY;
        private float maxX = Float.NEGATIVE_INFINITY;
        private float maxY = Float.NEGATIVE_INFINITY;
        private float maxZ = Float.NEGATIVE_INFINITY;
        private int vertices;

        private void include(float x, float y, float z) {
            if (Float.isNaN(x) || Float.isInfinite(x)
                    || Float.isNaN(y) || Float.isInfinite(y)
                    || Float.isNaN(z) || Float.isInfinite(z)) {
                throw new IllegalStateException(
                        "Drive model contains non-finite vertex coordinates");
            }

            this.minX = Math.min(this.minX, x);
            this.minY = Math.min(this.minY, y);
            this.minZ = Math.min(this.minZ, z);
            this.maxX = Math.max(this.maxX, x);
            this.maxY = Math.max(this.maxY, y);
            this.maxZ = Math.max(this.maxZ, z);
            this.vertices++;
        }

        private void requireInsideUnitCube(Item item) {
            float epsilon = 0.001f;
            if (this.minX < -epsilon
                    || this.minY < -epsilon
                    || this.minZ < -epsilon
                    || this.maxX > 1.0f + epsilon
                    || this.maxY > 1.0f + epsilon
                    || this.maxZ > 1.0f + epsilon) {
                throw new IllegalStateException(
                        "Drive model geometry exceeds block bounds for "
                                + item.getRegistryName()
                                + ": "
                                + this);
            }
        }

        private boolean sameAs(ModelBounds other, float epsilon) {
            return close(this.minX, other.minX, epsilon)
                    && close(this.minY, other.minY, epsilon)
                    && close(this.minZ, other.minZ, epsilon)
                    && close(this.maxX, other.maxX, epsilon)
                    && close(this.maxY, other.maxY, epsilon)
                    && close(this.maxZ, other.maxZ, epsilon);
        }

        private static boolean close(float left, float right, float epsilon) {
            return Math.abs(left - right) <= epsilon;
        }

        @Override
        public String toString() {
            return "["
                    + this.minX + "," + this.minY + "," + this.minZ
                    + " -> "
                    + this.maxX + "," + this.maxY + "," + this.maxZ
                    + "; vertices=" + this.vertices + "]";
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

        if (!FMLEnvironment.production && !model.equals(cells.model(item))) {
            throw new IllegalStateException(
                    "AE2 client cell model registry did not retain model " + model
                            + " for " + item.getRegistryName());
        }
    }
}
