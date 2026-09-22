package com.bulkcloud.expansionae.client;

import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.bulkcloud.expansionae.ExpansionAE;
import com.bulkcloud.expansionae.ae2.ExpansionAEApi;
import com.bulkcloud.expansionae.core.registry.ExpansionAEItems;

import appeng.api.client.ICellModelRegistry;

@Mod.EventBusSubscriber(
        modid = ExpansionAE.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public final class ExpansionAEClient {
    private ExpansionAEClient() {
    }

    @SubscribeEvent
    public static void onModelRegistry(ModelRegistryEvent event) {
        ICellModelRegistry cells = ExpansionAEApi.get().client().cells();

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
