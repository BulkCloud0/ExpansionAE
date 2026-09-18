package dev.bulkcloud.expansionae;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.WeakHashMap;

import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.tile.crafting.CraftingTileEntity;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

/**
 * Quantum Computer crafting tile for AE2 8.4.
 *
 * The modern AdvancedAE CPU adds configurable aggregate multipliers that the
 * AE2 8.4 public cluster API does not expose. The job engine remains AE2's;
 * after validating the original quantum multiblock rules, this tile scales the
 * native aggregate storage/thread counters to AdvancedAE's default values.
 */
public final class QuantumCraftingTile extends CraftingTileEntity {
    private static final int MAX_DIMENSION = 7;
    private static final int ACCELERATOR_THREADS = 8;
    private static final int MULTI_THREADER_MULTIPLIER = 4;
    private static final int DATA_ENTANGLER_MULTIPLIER = 4;

    private static final Field CPU_ACCELERATOR = findField("accelerator");
    private static final Field CPU_STORAGE = findField("availableStorage");
    private static final Set<CraftingCPUCluster> SCALED_CLUSTERS =
            Collections.newSetFromMap(new WeakHashMap<CraftingCPUCluster, Boolean>());

    public QuantumCraftingTile(TileEntityType<?> type) {
        super(type);
    }

    @Override
    public boolean isStorage() {
        return getStorageBytes() > 0;
    }

    @Override
    public int getStorageBytes() {
        switch (getKind()) {
            case CORE:
            case STORAGE_256M:
                return 256 * 1024 * 1024;
            case STORAGE_128M:
                return 128 * 1024 * 1024;
            default:
                return 0;
        }
    }

    @Override
    public boolean isAccelerator() {
        QuantumCraftingBlock.Kind kind = getKind();
        return kind == QuantumCraftingBlock.Kind.CORE
                || kind == QuantumCraftingBlock.Kind.ACCELERATOR;
    }

    @Override
    protected ItemStack getItemFromTile(Object obj) {
        if (world != null) {
            Block block = world.getBlockState(pos).getBlock();
            if (block instanceof QuantumCraftingBlock) {
                return new ItemStack(block);
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isValid() {
        StructureStats stats = inspectStructure();
        return stats != null && stats.valid;
    }

    @Override
    public void updateStatus(CraftingCPUCluster cluster) {
        super.updateStatus(cluster);
        if (cluster == null || CPU_ACCELERATOR == null || CPU_STORAGE == null) {
            return;
        }
        synchronized (SCALED_CLUSTERS) {
            if (SCALED_CLUSTERS.contains(cluster)) {
                return;
            }
            StructureStats stats = inspectStructure();
            if (stats == null || !stats.valid) {
                return;
            }
            try {
                int desiredThreads = stats.acceleratorTiles * ACCELERATOR_THREADS;
                if (stats.multiThreaders > 0) {
                    desiredThreads *= MULTI_THREADER_MULTIPLIER;
                }
                CPU_ACCELERATOR.setInt(cluster, CPU_ACCELERATOR.getInt(cluster)
                        + Math.max(0, desiredThreads - stats.acceleratorTiles));

                long desiredStorage = stats.baseStorage;
                if (stats.dataEntanglers > 0) {
                    desiredStorage *= DATA_ENTANGLER_MULTIPLIER;
                }
                CPU_STORAGE.setLong(cluster, CPU_STORAGE.getLong(cluster)
                        + Math.max(0L, desiredStorage - stats.baseStorage));
                SCALED_CLUSTERS.add(cluster);
            } catch (IllegalAccessException ignored) {
                // Native AE2 values remain a safe fallback.
            }
        }
    }

    private QuantumCraftingBlock.Kind getKind() {
        if (world != null) {
            Block block = world.getBlockState(pos).getBlock();
            if (block instanceof QuantumCraftingBlock) {
                return ((QuantumCraftingBlock) block).getQuantumKind();
            }
        }
        return QuantumCraftingBlock.Kind.UNIT;
    }

    private StructureStats inspectStructure() {
        if (world == null || isRemoved()) {
            return null;
        }

        Set<BlockPos> component = new HashSet<BlockPos>();
        Queue<BlockPos> open = new ArrayDeque<BlockPos>();
        BlockPos origin = pos.toImmutable();
        component.add(origin);
        open.add(origin);

        int minX = origin.getX();
        int minY = origin.getY();
        int minZ = origin.getZ();
        int maxX = minX;
        int maxY = minY;
        int maxZ = minZ;

        while (!open.isEmpty()) {
            BlockPos current = open.remove();
            minX = Math.min(minX, current.getX());
            minY = Math.min(minY, current.getY());
            minZ = Math.min(minZ, current.getZ());
            maxX = Math.max(maxX, current.getX());
            maxY = Math.max(maxY, current.getY());
            maxZ = Math.max(maxZ, current.getZ());

            for (Direction direction : Direction.values()) {
                BlockPos adjacent = current.offset(direction);
                TileEntity tile = world.getTileEntity(adjacent);
                if (tile instanceof CraftingTileEntity && !(tile instanceof QuantumCraftingTile)) {
                    return StructureStats.invalid();
                }
                if (tile instanceof QuantumCraftingTile) {
                    BlockPos immutable = adjacent.toImmutable();
                    if (component.add(immutable)) {
                        open.add(immutable);
                    }
                }
            }
        }

        if (maxX - minX + 1 > MAX_DIMENSION
                || maxY - minY + 1 > MAX_DIMENSION
                || maxZ - minZ + 1 > MAX_DIMENSION) {
            return StructureStats.invalid();
        }

        if (component.size() == 1) {
            return getKind() == QuantumCraftingBlock.Kind.CORE
                    ? StructureStats.singleCore(getStorageBytes())
                    : StructureStats.invalid();
        }

        int cores = 0;
        int dataEntanglers = 0;
        int multiThreaders = 0;
        int acceleratorTiles = 0;
        long storage = 0L;

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
                    if (!(tile instanceof QuantumCraftingTile)) {
                        return StructureStats.invalid();
                    }
                    QuantumCraftingTile quantumTile = (QuantumCraftingTile) tile;
                    QuantumCraftingBlock.Kind kind = quantumTile.getKind();
                    boolean boundary = x == minX || x == maxX
                            || y == minY || y == maxY
                            || z == minZ || z == maxZ;

                    switch (kind) {
                        case CORE:
                            if (boundary || cores > 0) return StructureStats.invalid();
                            cores++;
                            break;
                        case STRUCTURE:
                            if (!boundary) return StructureStats.invalid();
                            break;
                        case DATA_ENTANGLER:
                            if (boundary || dataEntanglers >= 1) return StructureStats.invalid();
                            dataEntanglers++;
                            break;
                        case MULTI_THREADER:
                            if (boundary || multiThreaders >= 1) return StructureStats.invalid();
                            multiThreaders++;
                            break;
                        default:
                            if (boundary) return StructureStats.invalid();
                            break;
                    }

                    int bytes = quantumTile.getStorageBytes();
                    if (bytes > 0) storage += bytes;
                    if (kind == QuantumCraftingBlock.Kind.CORE
                            || kind == QuantumCraftingBlock.Kind.ACCELERATOR) {
                        acceleratorTiles++;
                    }
                }
            }
        }

        return new StructureStats(cores == 1 && storage > 0L, storage,
                acceleratorTiles, dataEntanglers, multiThreaders);
    }

    private static Field findField(String name) {
        try {
            Field field = CraftingCPUCluster.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static final class StructureStats {
        private final boolean valid;
        private final long baseStorage;
        private final int acceleratorTiles;
        private final int dataEntanglers;
        private final int multiThreaders;

        private StructureStats(boolean valid, long baseStorage, int acceleratorTiles,
                int dataEntanglers, int multiThreaders) {
            this.valid = valid;
            this.baseStorage = baseStorage;
            this.acceleratorTiles = acceleratorTiles;
            this.dataEntanglers = dataEntanglers;
            this.multiThreaders = multiThreaders;
        }

        private static StructureStats invalid() {
            return new StructureStats(false, 0L, 0, 0, 0);
        }

        private static StructureStats singleCore(long storage) {
            return new StructureStats(true, storage, 1, 0, 0);
        }
    }
}
