package dev.bulkcloud.expansionae;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.me.cluster.IAECluster;
import appeng.me.cluster.MBCalculator;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.util.math.BlockPos;

public final class AssemblerMatrixCluster implements IAECluster {
    private final BlockPos min;
    private final BlockPos max;
    private final List<AssemblerMatrixTile> tiles = new ArrayList<>();
    private final List<AssemblerMatrixTile> crafters = new ArrayList<>();
    private boolean destroyed;
    private int speedCores;

    public AssemblerMatrixCluster(BlockPos min, BlockPos max) {
        this.min = min.toImmutable();
        this.max = max.toImmutable();
    }

    public void add(AssemblerMatrixTile tile) {
        tiles.add(tile);
        if (tile.getKind() == AssemblerMatrixBlock.Kind.CRAFTER) crafters.add(tile);
        if (tile.getKind() == AssemblerMatrixBlock.Kind.SPEED && speedCores < 5) speedCores++;
    }

    public int getSpeedCores() { return speedCores; }

    public boolean pushPattern(ICraftingPatternDetails details, CraftingInventory table) {
        for (AssemblerMatrixTile crafter : crafters) {
            if (crafter.acceptMatrixJob(details, table)) return true;
        }
        return false;
    }

    public boolean isBusy() {
        if (crafters.isEmpty()) return true;
        for (AssemblerMatrixTile crafter : crafters) if (crafter.hasFreeMatrixLane()) return false;
        return true;
    }

    @Override public BlockPos getBoundsMin() { return min; }
    @Override public BlockPos getBoundsMax() { return max; }

    @Override
    public void updateStatus(boolean updateGrid) {
        for (AssemblerMatrixTile tile : tiles) tile.refreshMatrixState();
    }

    @Override
    public void destroy() {
        if (destroyed) return;
        destroyed = true;
        boolean owns = !MBCalculator.isModificationInProgress();
        if (owns) MBCalculator.setModificationInProgress(this);
        try {
            for (AssemblerMatrixTile tile : tiles) tile.updateStatus(null);
        } finally {
            if (owns) MBCalculator.setModificationInProgress(null);
        }
    }

    @Override public boolean isDestroyed() { return destroyed; }
    @Override public Iterator<AssemblerMatrixTile> getBlockEntities() { return tiles.iterator(); }
}
