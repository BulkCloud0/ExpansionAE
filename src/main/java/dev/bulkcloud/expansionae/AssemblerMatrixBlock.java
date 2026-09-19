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

public final class AssemblerMatrixBlock extends AEBaseTileBlock<AssemblerMatrixTile> {
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    public static final BooleanProperty POWERED = BooleanProperty.create("powered");

    public enum Kind { FRAME, WALL, GLASS, PATTERN, CRAFTER, SPEED }

    private final Kind kind;

    public AssemblerMatrixBlock(Kind kind) {
        super(defaultProps(kind == Kind.GLASS ? Material.GLASS : Material.IRON)
                .hardnessAndResistance(kind == Kind.GLASS ? 1.5F : 3.5F, kind == Kind.GLASS ? 3.0F : 8.0F)
                .notSolid());
        this.kind = kind;
        setDefaultState(getDefaultState().with(FORMED, false).with(POWERED, false));
    }

    public Kind getKind() { return kind; }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        super.fillStateContainer(builder);
        builder.add(FORMED, POWERED);
    }

    @Override
    protected BlockState updateBlockStateFromTileEntity(BlockState state, AssemblerMatrixTile tile) {
        return state.with(FORMED, tile.isFormed()).with(POWERED, tile.isActive());
    }

    @Override
    public void neighborChanged(BlockState state, World world, BlockPos pos, Block block, BlockPos fromPos,
            boolean isMoving) {
        super.neighborChanged(state, world, pos, block, fromPos, isMoving);
        AssemblerMatrixTile tile = getTileEntity(world, pos);
        if (tile != null) tile.updateMultiBlock(fromPos);
    }

    @Override
    public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player,
            Hand hand, BlockRayTraceResult hit) {
        AssemblerMatrixTile tile = getTileEntity(world, pos);
        if (tile != null && kind == Kind.PATTERN && !InteractionUtil.isInAlternateUseMode(player)) {
            if (!world.isRemote()) {
                ContainerOpener.openContainer(AssemblerMatrixPatternContainer.TYPE, player,
                        ContainerLocator.forTileEntitySide(tile, hit.getFace()));
            }
            return ActionResultType.func_233537_a_(world.isRemote());
        }
        return super.onBlockActivated(state, world, pos, player, hand, hit);
    }
}
