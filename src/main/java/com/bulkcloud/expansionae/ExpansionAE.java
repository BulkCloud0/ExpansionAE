package com.bulkcloud.expansionae;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bulkcloud.expansionae.client.ClientBootstrap;
import com.bulkcloud.expansionae.config.ExpansionAEConfig;
import com.bulkcloud.expansionae.registry.ExpansionAEItems;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(ExpansionAE.MOD_ID)
public final class ExpansionAE {

    public static final String MOD_ID = "expansionae";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public ExpansionAE() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ExpansionAEItems.ITEMS.register(modEventBus);

        ModLoadingContext.get().registerConfig(
                ModConfig.Type.COMMON,
                ExpansionAEConfig.COMMON_SPEC,
                MOD_ID + "-common.toml");

        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> ClientBootstrap::register);

        LOGGER.info("Initializing ExpansionAE for Minecraft 1.16.5");
    }
}
