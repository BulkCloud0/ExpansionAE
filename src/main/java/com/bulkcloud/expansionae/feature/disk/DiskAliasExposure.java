package com.bulkcloud.expansionae.feature.disk;

import java.util.UUID;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.items.IItemHandler;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.api.storage.cells.ISaveProvider;
import appeng.tile.storage.ChestTileEntity;
import appeng.tile.storage.DriveTileEntity;

final class DiskAliasExposure {
    private DiskAliasExposure() {
    }

    static boolean shouldExposeToGrid(ItemStack cellStack, ISaveProvider saveProvider) {
        UUID uuid = getUuid(cellStack);
        if (uuid == null
                || !(saveProvider instanceof IActionHost)
                || !(saveProvider instanceof TileEntity)) {
            return true;
        }

        IGridNode currentNode = ((IActionHost) saveProvider).getActionableNode();
        if (currentNode == null || !currentNode.isActive()) {
            // Local/direct access outside an active grid must continue to see the cell.
            return true;
        }

        Candidate current = findCurrentCandidate(cellStack, saveProvider, uuid);
        if (current == null) {
            // Unknown/custom host: do not hide contents without a provable peer.
            return true;
        }

        IGrid grid = currentNode.getGrid();
        Candidate winner = current;

        for (IGridNode node : grid.getNodes()) {
            if (!node.isActive()) {
                continue;
            }

            IGridHost machine = node.getMachine();
            Candidate candidate = findBestCandidate(machine, uuid);
            if (candidate != null && candidate.compareTo(winner) < 0) {
                winner = candidate;
            }
        }

        return current.equals(winner);
    }

    private static Candidate findCurrentCandidate(
            ItemStack cellStack,
            ISaveProvider host,
            UUID uuid) {
        if (!(host instanceof TileEntity)) {
            return null;
        }

        BlockPos pos = ((TileEntity) host).getPos();

        if (host instanceof DriveTileEntity) {
            IItemHandler inventory = ((DriveTileEntity) host).getInternalInventory();

            // The handler is normally constructed from the exact ItemStack object held by
            // the Drive. Identity lets two same-UUID aliases in different slots of one
            // Drive elect a single deterministic owner.
            for (int slot = 0; slot < inventory.getSlots(); slot++) {
                if (inventory.getStackInSlot(slot) == cellStack) {
                    return new Candidate(pos, slot);
                }
            }

            // Defensive fallback for hosts that return a copy rather than the exact stack.
            for (int slot = 0; slot < inventory.getSlots(); slot++) {
                if (uuid.equals(getUuid(inventory.getStackInSlot(slot)))) {
                    return new Candidate(pos, slot);
                }
            }
            return null;
        }

        if (host instanceof ChestTileEntity) {
            ItemStack cell = ((ChestTileEntity) host).getCell();
            if (cell == cellStack || uuid.equals(getUuid(cell))) {
                return new Candidate(pos, 0);
            }
        }

        return null;
    }

    private static Candidate findBestCandidate(IGridHost host, UUID uuid) {
        if (!(host instanceof TileEntity)) {
            return null;
        }

        BlockPos pos = ((TileEntity) host).getPos();
        Candidate best = null;

        if (host instanceof DriveTileEntity) {
            IItemHandler inventory = ((DriveTileEntity) host).getInternalInventory();
            for (int slot = 0; slot < inventory.getSlots(); slot++) {
                if (uuid.equals(getUuid(inventory.getStackInSlot(slot)))) {
                    Candidate candidate = new Candidate(pos, slot);
                    if (best == null || candidate.compareTo(best) < 0) {
                        best = candidate;
                    }
                }
            }
            return best;
        }

        if (host instanceof ChestTileEntity
                && uuid.equals(getUuid(((ChestTileEntity) host).getCell()))) {
            return new Candidate(pos, 0);
        }

        return null;
    }

    private static UUID getUuid(ItemStack stack) {
        if (stack.isEmpty()
                || !stack.hasTag()
                || !stack.getTag().hasUniqueId(DiskCellInventory.TAG_UUID)) {
            return null;
        }
        return stack.getTag().getUniqueId(DiskCellInventory.TAG_UUID);
    }

    private static final class Candidate implements Comparable<Candidate> {
        private final BlockPos pos;
        private final int slot;

        private Candidate(BlockPos pos, int slot) {
            this.pos = pos.toImmutable();
            this.slot = slot;
        }

        @Override
        public int compareTo(Candidate other) {
            int x = Integer.compare(this.pos.getX(), other.pos.getX());
            if (x != 0) {
                return x;
            }

            int y = Integer.compare(this.pos.getY(), other.pos.getY());
            if (y != 0) {
                return y;
            }

            int z = Integer.compare(this.pos.getZ(), other.pos.getZ());
            if (z != 0) {
                return z;
            }

            return Integer.compare(this.slot, other.slot);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Candidate)) {
                return false;
            }
            Candidate other = (Candidate) obj;
            return this.slot == other.slot && this.pos.equals(other.pos);
        }

        @Override
        public int hashCode() {
            return 31 * this.pos.hashCode() + this.slot;
        }
    }
}
