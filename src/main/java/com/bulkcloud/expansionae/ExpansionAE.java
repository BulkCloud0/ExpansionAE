package com.bulkcloud.expansionae;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import com.bulkcloud.expansionae.core.registry.ExpansionAEBlocks;
import com.bulkcloud.expansionae.core.registry.ExpansionAEItems;

@Mod(ExpansionAE.MOD_ID)
public final class ExpansionAE {
    public static final String MOD_ID = "expansionae";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public ExpansionAE() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ExpansionAEBlocks.register(modBus);
        ExpansionAEItems.register(modBus);

        MinecraftForge.EVENT_BUS.register(this);
        LOGGER.info("ExpansionAE bootstrap initialized");
    }
}
