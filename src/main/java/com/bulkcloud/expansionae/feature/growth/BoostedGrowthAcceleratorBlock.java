package com.bulkcloud.expansionae.feature.growth;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;

import appeng.api.util.IOrientableBlock;
import appeng.block.AEBaseTileBlock;

public final class BoostedGrowthAcceleratorBlock
        extends AEBaseTileBlock<BoostedGrowthAcceleratorTileEntity>
        implements IOrientableBlock {
    private static final BooleanProperty POWERED = BooleanProperty.create("powered");

    public BoostedGrowthAcceleratorBlock() {
        super(defaultProps(Material.ROCK).sound(SoundType.METAL));
        this.setDefaultState(this.getDefaultState().with(POWERED, false));
        this.setTileEntity(
                BoostedGrowthAcceleratorTileEntity.class,
                BoostedGrowthAcceleratorTileEntity::new);
    }

    @Override
    protected BlockState updateBlockStateFromTileEntity(
            BlockState currentState,
            BoostedGrowthAcceleratorTileEntity tile) {
        return currentState.with(POWERED, tile.isPowered());
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        super.fillStateContainer(builder);
        builder.add(POWERED);
    }
}
