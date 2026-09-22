package com.bulkcloud.expansionae.feature.growth;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockReader;
import net.minecraftforge.common.ToolType;

import com.bulkcloud.expansionae.core.registry.ExpansionAETileEntities;

public final class CrankedGrowthAcceleratorBlock extends Block {
    public static final BooleanProperty POWERED = BooleanProperty.create("powered");

    public CrankedGrowthAcceleratorBlock() {
        super(Properties.create(Material.ROCK)
                .hardnessAndResistance(3.0F, 6.0F)
                .sound(SoundType.METAL)
                .harvestTool(ToolType.PICKAXE)
                .harvestLevel(0));
        setDefaultState(getStateContainer().getBaseState().with(POWERED, false));
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return ExpansionAETileEntities.CRANKED_GROWTH_ACCELERATOR.get().create();
    }
}
