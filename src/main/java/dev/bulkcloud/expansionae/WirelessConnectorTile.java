package dev.bulkcloud.expansionae;

import java.util.EnumSet;

import appeng.api.networking.IGridNode;
import appeng.api.util.AECableType;
import appeng.api.util.AEPartLocation;
import appeng.tile.grid.AENetworkTileEntity;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * 1.16.5 semantic backport of ExtendedAE's Wireless Connector.
 */
public final class WirelessConnectorTile extends AENetworkTileEntity
        implements ITickableTileEntity, WirelessLinkEndpoint {
    public static final double MAX_RANGE = WirelessLink.MAX_RANGE;

    private final WirelessLink link = new WirelessLink(this);

    public WirelessConnectorTile(TileEntityType<?> type) {
        super(type);
        getProxy().setValidSides(EnumSet.allOf(Direction.class));
        getProxy().setIdlePowerUsage(1.0D);
    }

    public long getFrequency() {
        return link.getFrequency();
    }

    public void setFrequency(long frequency) {
        link.setFrequency(frequency);
        markDirty();
        markForUpdate();
    }

    public boolean isWirelessConnected() {
        return link.isConnected();
    }

    public double getWirelessDistance() {
        return link.getDistance();
    }

    @Override
    public void tick() {
        link.tick();
    }

    @Override
    public void read(BlockState state, CompoundNBT data) {
        super.read(state, data);
        link.setFrequency(data.getLong("wirelessFrequency"));
    }

    @Override
    public CompoundNBT write(CompoundNBT data) {
        super.write(data);
        data.putLong("wirelessFrequency", link.getFrequency());
        return data;
    }

    @Override
    public void onReady() {
        super.onReady();
        link.onReady();
    }

    @Override
    public void onChunkUnloaded() {
        link.close();
        super.onChunkUnloaded();
    }

    @Override
    public void remove() {
        link.close();
        super.remove();
    }

    @Override
    public AECableType getCableConnectionType(AEPartLocation dir) {
        return AECableType.DENSE_SMART;
    }

    @Override
    public World wirelessWorld() {
        return world;
    }

    @Override
    public BlockPos wirelessPos() {
        return pos;
    }

    @Override
    public IGridNode wirelessNode() {
        return getProxy().getNode();
    }

    @Override
    public boolean wirelessActive() {
        return getProxy().isActive();
    }

    @Override
    public boolean wirelessRemoved() {
        return isRemoved();
    }

    @Override
    public Object wirelessOwner() {
        return this;
    }

    @Override
    public void wirelessChanged() {
        double distance = link.getDistance();
        double power = 1.0D;
        if (link.isConnected()) {
            double d = Math.max(distance, Math.E);
            power = Math.max(1.0D, d * Math.log(d));
        }
        getProxy().setIdlePowerUsage(power);
        markForUpdate();
    }
}
