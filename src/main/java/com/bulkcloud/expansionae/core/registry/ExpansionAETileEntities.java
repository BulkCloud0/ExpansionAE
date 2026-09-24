package com.bulkcloud.expansionae.core.registry;

import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import com.bulkcloud.expansionae.ExpansionAE;
import com.bulkcloud.expansionae.feature.growth.BoostedGrowthAcceleratorTileEntity;
import com.bulkcloud.expansionae.feature.growth.CrankedGrowthAcceleratorTileEntity;
import com.bulkcloud.expansionae.feature.extendedprovider.PatternProvider36TileEntity;

public final class ExpansionAETileEntities {
    public static final DeferredRegister<TileEntityType<?>> TILE_ENTITIES =
            DeferredRegister.create(ForgeRegistries.TILE_ENTITIES, ExpansionAE.MOD_ID);

    public static final RegistryObject<TileEntityType<BoostedGrowthAcceleratorTileEntity>>
            BOOSTED_GROWTH_ACCELERATOR =
            TILE_ENTITIES.register(
                    "boosted_growth_accelerator",
                    () -> TileEntityType.Builder.create(
                            BoostedGrowthAcceleratorTileEntity::new,
                            ExpansionAEBlocks.BOOSTED_GROWTH_ACCELERATOR.get())
                            .build(null));

    public static final RegistryObject<TileEntityType<CrankedGrowthAcceleratorTileEntity>>
            CRANKED_GROWTH_ACCELERATOR =
            TILE_ENTITIES.register(
                    "cranked_growth_accelerator",
                    () -> TileEntityType.Builder.create(
                            CrankedGrowthAcceleratorTileEntity::new,
                            ExpansionAEBlocks.CRANKED_GROWTH_ACCELERATOR.get())
                            .build(null));

    public static final RegistryObject<TileEntityType<PatternProvider36TileEntity>>
            PATTERN_PROVIDER_36 =
            TILE_ENTITIES.register(
                    "pattern_provider_36",
                    () -> TileEntityType.Builder.create(
                            PatternProvider36TileEntity::new,
                            ExpansionAEBlocks.PATTERN_PROVIDER_36.get())
                            .build(null));

    private ExpansionAETileEntities() {
    }

    public static void register(IEventBus modBus) {
        TILE_ENTITIES.register(modBus);
    }
}
