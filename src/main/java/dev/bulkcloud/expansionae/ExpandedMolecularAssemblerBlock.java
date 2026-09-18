package dev.bulkcloud.expansionae;

import appeng.block.AEBaseTileBlock;
import appeng.container.ContainerLocator;
import appeng.container.ContainerOpener;
import appeng.util.InteractionUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.World;

public final class ExpandedMolecularAssemblerBlock
        extends AEBaseTileBlock<ExpandedMolecularAssemblerTile> {
    public static final BooleanProperty POWERED = BooleanProperty.create("powered");

    public ExpandedMolecularAssemblerBlock() {
        super(defaultProps(Material.IRON).notSolid());
        setDefaultState(getDefaultState().with(POWERED, false));
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        super.fillStateContainer(builder);
        builder.add(POWERED);
    }

    @Override
    protected BlockState updateBlockStateFromTileEntity(BlockState state,
            ExpandedMolecularAssemblerTile tile) {
        return state.with(POWERED, tile.isPowered());
    }

    @Override
    public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos,
            PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
        ExpandedMolecularAssemblerTile tile = getTileEntity(world, pos);
        if (tile != null && !InteractionUtil.isInAlternateUseMode(player)) {
            if (!world.isRemote()) {
                ContainerOpener.openContainer(ExpandedMolecularAssemblerContainer.TYPE, player,
                        ContainerLocator.forTileEntitySide(tile, hit.getFace()));
            }
            return ActionResultType.func_233537_a_(world.isRemote());
        }
        return super.onBlockActivated(state, world, pos, player, hand, hit);
    }
}
