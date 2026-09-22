package com.bulkcloud.expansionae.core.registry;

import net.minecraft.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.fml.RegistryObject;

import com.bulkcloud.expansionae.ExpansionAE;
import com.bulkcloud.expansionae.feature.growth.BoostedGrowthAcceleratorBlock;
import com.bulkcloud.expansionae.feature.growth.CrankedGrowthAcceleratorBlock;

public final class ExpansionAEBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, ExpansionAE.MOD_ID);

    public static final RegistryObject<Block> CRANKED_GROWTH_ACCELERATOR =
            BLOCKS.register("cranked_growth_accelerator", CrankedGrowthAcceleratorBlock::new);

    public static final RegistryObject<Block> BOOSTED_GROWTH_ACCELERATOR =
            BLOCKS.register("boosted_growth_accelerator", BoostedGrowthAcceleratorBlock::new);

    private ExpansionAEBlocks() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }
}
