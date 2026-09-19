package dev.bulkcloud.expansionae;

import appeng.me.cluster.MBCalculator;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class AssemblerMatrixCalculator extends MBCalculator<AssemblerMatrixTile, AssemblerMatrixCluster> {
    private static final int MAX_SPAN = 9;

    public AssemblerMatrixCalculator(AssemblerMatrixTile target) { super(target); }

    @Override
    public boolean checkMultiblockScale(BlockPos min, BlockPos max) {
        int dx = max.getX() - min.getX();
        int dy = max.getY() - min.getY();
        int dz = max.getZ() - min.getZ();
        return dx >= 2 && dy >= 2 && dz >= 2 && dx <= MAX_SPAN && dy <= MAX_SPAN && dz <= MAX_SPAN;
    }

    @Override
    public AssemblerMatrixCluster createCluster(World world, BlockPos min, BlockPos max) {
        return new AssemblerMatrixCluster(min, max);
    }

    @Override
    public boolean verifyInternalStructure(World world, BlockPos min, BlockPos max) {
        boolean pattern = false;
        boolean crafter = false;
        for (BlockPos pos : BlockPos.getAllInBoxMutable(min, max)) {
            TileEntity raw = world.getTileEntity(pos);
            if (!(raw instanceof AssemblerMatrixTile)) return false;
            AssemblerMatrixTile tile = (AssemblerMatrixTile) raw;
            AssemblerMatrixBlock.Kind kind = tile.getKind();
            if (isInternal(pos, min, max)) {
                if (kind != AssemblerMatrixBlock.Kind.PATTERN
                        && kind != AssemblerMatrixBlock.Kind.CRAFTER
                        && kind != AssemblerMatrixBlock.Kind.SPEED) return false;
                pattern |= kind == AssemblerMatrixBlock.Kind.PATTERN;
                crafter |= kind == AssemblerMatrixBlock.Kind.CRAFTER;
            } else if (isEdge(pos, min, max)) {
                if (kind != AssemblerMatrixBlock.Kind.FRAME) return false;
            } else if (kind != AssemblerMatrixBlock.Kind.WALL && kind != AssemblerMatrixBlock.Kind.GLASS) {
                return false;
            }
        }
        return pattern && crafter;
    }

    @Override
    public void updateTiles(AssemblerMatrixCluster cluster, World world, BlockPos min, BlockPos max) {
        for (BlockPos pos : BlockPos.getAllInBoxMutable(min, max)) {
            TileEntity raw = world.getTileEntity(pos);
            if (raw instanceof AssemblerMatrixTile) {
                AssemblerMatrixTile tile = (AssemblerMatrixTile) raw;
                tile.updateStatus(cluster);
                cluster.add(tile);
            }
        }
        cluster.updateStatus(true);
    }

    @Override public boolean isValidTile(TileEntity tile) { return tile instanceof AssemblerMatrixTile; }

    private static boolean isInternal(BlockPos p, BlockPos min, BlockPos max) {
        return p.getX() > min.getX() && p.getX() < max.getX()
                && p.getY() > min.getY() && p.getY() < max.getY()
                && p.getZ() > min.getZ() && p.getZ() < max.getZ();
    }

    private static boolean isEdge(BlockPos p, BlockPos min, BlockPos max) {
        int boundary = 0;
        if (p.getX() == min.getX() || p.getX() == max.getX()) boundary++;
        if (p.getY() == min.getY() || p.getY() == max.getY()) boundary++;
        if (p.getZ() == min.getZ() || p.getZ() == max.getZ()) boundary++;
        return boundary >= 2;
    }
}
