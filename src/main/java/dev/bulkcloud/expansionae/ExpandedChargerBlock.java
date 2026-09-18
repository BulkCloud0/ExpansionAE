package dev.bulkcloud.expansionae;

import javax.annotation.Nullable;

import appeng.api.util.AEAxisAlignedBB;
import appeng.block.AEBaseTileBlock;
import appeng.util.InteractionUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

/** Block wrapper for the four-slot Extended Charger. */
public final class ExpandedChargerBlock extends AEBaseTileBlock<ExpandedChargerTile> {
    public ExpandedChargerBlock() {
        super(defaultProps(Material.IRON).notSolid());
    }

    @Override
    public int getOpacity(BlockState state, IBlockReader world, BlockPos pos) {
        return 2;
    }

    @Override
    public ActionResultType onActivated(World world, BlockPos pos, PlayerEntity player, Hand hand,
            @Nullable ItemStack heldItem, BlockRayTraceResult hit) {
        if (InteractionUtil.isInAlternateUseMode(player)) {
            return ActionResultType.PASS;
        }
        if (!world.isRemote()) {
            ExpandedChargerTile tile = getTileEntity(world, pos);
            if (tile != null) {
                tile.activate(player);
            }
        }
        return ActionResultType.func_233537_a_(world.isRemote());
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext context) {
        ExpandedChargerTile tile = getTileEntity(world, pos);
        if (tile != null) {
            double twoPixels = 2.0 / 16.0;
            Direction forward = tile.getForward();
            AEAxisAlignedBB bb = new AEAxisAlignedBB(twoPixels, twoPixels, twoPixels,
                    1.0 - twoPixels, 1.0 - twoPixels, 1.0 - twoPixels);
            switch (forward) {
                case DOWN: bb.maxY = 1; break;
                case UP: bb.minY = 0; break;
                case NORTH: bb.maxZ = 1; break;
                case SOUTH: bb.minZ = 0; break;
                case EAST: bb.minX = 0; break;
                case WEST: bb.maxX = 1; break;
                default: break;
            }
            return VoxelShapes.create(bb.getBoundingBox());
        }
        return VoxelShapes.create(new AxisAlignedBB(0, 0, 0, 1, 1, 1));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, IBlockReader world, BlockPos pos,
            ISelectionContext context) {
        return VoxelShapes.create(new AxisAlignedBB(0, 0, 0, 1, 1, 1));
    }
}
