package com.bulkcloud.expansionae;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import com.bulkcloud.expansionae.core.registry.ExpansionAEBlocks;
import com.bulkcloud.expansionae.core.registry.ExpansionAEItems;
import com.bulkcloud.expansionae.feature.disk.DiskStorageService;

@Mod(ExpansionAE.MOD_ID)
public final class ExpansionAE {
    public static final String MOD_ID = "expansionae";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public ExpansionAE() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ExpansionAEBlocks.register(modBus);
        ExpansionAEItems.register(modBus);
        modBus.addListener(this::onCommonSetup);

        MinecraftForge.EVENT_BUS.register(this);
        LOGGER.info("ExpansionAE bootstrap initialized");
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ExpansionAEItems::registerAE2Upgrades);
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        DiskStorageService.onWorldLoad(event);
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        DiskStorageService.onWorldUnload(event);
    }
}
