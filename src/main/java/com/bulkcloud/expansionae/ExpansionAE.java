package com.bulkcloud.expansionae;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;

import com.bulkcloud.expansionae.core.registry.ExpansionAEBlocks;
import com.bulkcloud.expansionae.core.registry.ExpansionAEItems;
import com.bulkcloud.expansionae.ae2.ExpansionAEApi;
import com.bulkcloud.expansionae.feature.disk.DiskGridRuntimeValidator;
import com.bulkcloud.expansionae.feature.disk.DiskRuntimeValidator;
import com.bulkcloud.expansionae.feature.disk.DiskStorageService;
import com.bulkcloud.expansionae.feature.extendedbus.ExtendedBusRuntimeValidator;
import com.bulkcloud.expansionae.feature.extendedbus.ExtendedBusTransferRuntimeValidator;

@Mod(ExpansionAE.MOD_ID)
public final class ExpansionAE {
    public static final String MOD_ID = "expansionae";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    private static final String DEV_RUNTIME_VALIDATION_PROPERTY =
            "expansionae.validateDevRuntime";
    private static final boolean DEV_RUNTIME_VALIDATION =
            Boolean.getBoolean(DEV_RUNTIME_VALIDATION_PROPERTY)
                    && !FMLEnvironment.production;

    public ExpansionAE() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ExpansionAEApi.registerPartModelsEarly();
        ExpansionAEBlocks.register(modBus);
        ExpansionAEItems.register(modBus);
        modBus.addListener(this::onCommonSetup);

        MinecraftForge.EVENT_BUS.register(this);

        if (Boolean.getBoolean(DEV_RUNTIME_VALIDATION_PROPERTY)
                && FMLEnvironment.production) {
            LOGGER.warn(
                    "Ignoring {} in a production Forge environment; "
                            + "destructive DISK runtime validators are userdev/CI-only",
                    DEV_RUNTIME_VALIDATION_PROPERTY);
        }

        LOGGER.info("ExpansionAE bootstrap initialized");
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ExpansionAEItems.registerAE2Upgrades();
            if (DEV_RUNTIME_VALIDATION) {
                ExpansionAEItems.validateDiskWorkbenchContract();
            }
        });
    }

    @SubscribeEvent
    public void onServerStarted(FMLServerStartedEvent event) {
        if (DEV_RUNTIME_VALIDATION) {
            ExtendedBusRuntimeValidator.validate();
            DiskRuntimeValidator.validate();
            ExtendedBusTransferRuntimeValidator.begin();
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END
                && DEV_RUNTIME_VALIDATION) {
            DiskGridRuntimeValidator.tick();
            ExtendedBusTransferRuntimeValidator.tick();
        }
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
