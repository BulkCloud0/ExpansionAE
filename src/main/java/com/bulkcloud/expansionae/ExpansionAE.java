package com.bulkcloud.expansionae;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bulkcloud.expansionae.config.ExpansionAEConfig;

import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod(ExpansionAE.MOD_ID)
public final class ExpansionAE {

    public static final String MOD_ID = "expansionae";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public ExpansionAE() {
        ModLoadingContext.get().registerConfig(
                ModConfig.Type.COMMON,
                ExpansionAEConfig.COMMON_SPEC,
                MOD_ID + "-common.toml");

        LOGGER.info("Initializing ExpansionAE for Minecraft 1.16.5");
    }
}
