package dev.bulkcloud.expansionae;

import appeng.api.networking.IGridNode;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

interface WirelessLinkEndpoint {
    World wirelessWorld();
    BlockPos wirelessPos();
    IGridNode wirelessNode();
    boolean wirelessActive();
    boolean wirelessRemoved();
    Object wirelessOwner();
    void wirelessChanged();
}
