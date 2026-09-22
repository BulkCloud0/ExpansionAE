package com.bulkcloud.expansionae.client;

import com.bulkcloud.expansionae.ae2.AE2Bridge;
import com.bulkcloud.expansionae.registry.ExpansionAEItems;

import appeng.api.client.ICellModelRegistry;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public final class ClientBootstrap {

    private ClientBootstrap() {
    }

    public static void register() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(ClientBootstrap::onClientSetup);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ICellModelRegistry cells = AE2Bridge.api().client().cells();

            cells.registerModel(
                    ExpansionAEItems.DISK_1K.get(),
                    new ResourceLocation("appliedenergistics2", "block/drive/cells/1k_item_cell"));
            cells.registerModel(
                    ExpansionAEItems.DISK_4K.get(),
                    new ResourceLocation("appliedenergistics2", "block/drive/cells/4k_item_cell"));
            cells.registerModel(
                    ExpansionAEItems.DISK_16K.get(),
                    new ResourceLocation("appliedenergistics2", "block/drive/cells/16k_item_cell"));
            cells.registerModel(
                    ExpansionAEItems.DISK_64K.get(),
                    new ResourceLocation("appliedenergistics2", "block/drive/cells/64k_item_cell"));
        });
    }
}
