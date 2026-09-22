package com.bulkcloud.expansionae.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ExpansionAEConfig {

    public static final ForgeConfigSpec COMMON_SPEC;
    public static final ForgeConfigSpec.BooleanValue DEBUG_LOGGING;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("general");
        DEBUG_LOGGING = builder
                .comment("Enables additional diagnostic logging for ExpansionAE integrations.")
                .define("debugLogging", false);
        builder.pop();

        COMMON_SPEC = builder.build();
    }

    private ExpansionAEConfig() {
    }
}
