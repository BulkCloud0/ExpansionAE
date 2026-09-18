package dev.bulkcloud.expansionae;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import appeng.api.exceptions.FailedConnectionException;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridConnection;
import appeng.api.util.AECableType;
import appeng.api.util.AEPartLocation;
import appeng.me.GridConnection;
import appeng.tile.grid.AENetworkTileEntity;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.world.World;

/**
 * Point-to-point wireless ME link for AE2 8.4.
 * Frequencies are signed: one endpoint owns +f and the other -f.
 */
public final class WirelessConnectorTile extends AENetworkTileEntity implements ITickableTileEntity {
    public static final double MAX_RANGE = 256.0;
    private static final Map<World, Map<Long, WirelessConnectorTile>> REGISTRY = new WeakHashMap<>();

    private long frequency;
    private IGridConnection connection;
    private int ticker;

    public WirelessConnectorTile(TileEntityType<?> type) {
        super(type);
        getProxy().setValidSides(EnumSet.allOf(Direction.class));
        getProxy().setFlags(GridFlags.DENSE_CAPACITY);
        getProxy().setIdlePowerUsage(1.0);
    }

    @Override public boolean canBeRotated() { return false; }

    @Override
    public AECableType getCableConnectionType(AEPartLocation dir) {
        return AECableType.DENSE_SMART;
    }

    public long getFrequency() { return frequency; }

    public void setFrequency(long newFrequency) {
        if (frequency == newFrequency) return;
        unregister();
        destroyConnection();
        frequency = newFrequency;
        register();
        saveChanges();
        markForUpdate();
    }

    public boolean isWirelessConnected() { return connection != null; }

    @Override
    public void onReady() {
        super.onReady();
        register();
        refreshLink();
    }

    @Override
    public void tick() {
        if (world == null || world.isRemote) return;
        if (++ticker >= 20) {
            ticker = 0;
            register();
            refreshLink();
        }
    }

    private void register() {
        if (world == null || world.isRemote || frequency == 0 || isRemoved()) return;
        REGISTRY.computeIfAbsent(world, w -> new HashMap<>()).put(frequency, this);
    }

    private void unregister() {
        if (world == null || frequency == 0) return;
        Map<Long, WirelessConnectorTile> byFreq = REGISTRY.get(world);
        if (byFreq != null && byFreq.get(frequency) == this) {
            byFreq.remove(frequency);
            if (byFreq.isEmpty()) REGISTRY.remove(world);
        }
    }

    private void refreshLink() {
        if (world == null || world.isRemote || frequency == 0 || isRemoved()) {
            destroyConnection();
            return;
        }
        Map<Long, WirelessConnectorTile> byFreq = REGISTRY.get(world);
        WirelessConnectorTile other = byFreq == null ? null : byFreq.get(-frequency);
        if (other == null || other == this || other.isRemoved() || other.world != world
                || !world.isBlockLoaded(other.pos)) {
            destroyConnection();
            getProxy().setIdlePowerUsage(1.0);
            return;
        }

        double distance = Math.sqrt(pos.distanceSq(other.pos));
        if (distance > MAX_RANGE) {
            destroyConnection();
            getProxy().setIdlePowerUsage(1.0);
            return;
        }

        double d = Math.max(Math.E, distance);
        double cost = Math.max(1.0, d * Math.log(d));
        getProxy().setIdlePowerUsage(cost);
        other.getProxy().setIdlePowerUsage(cost);

        if (connection != null || other.connection != null) return;
        if (getProxy().getNode() == null || other.getProxy().getNode() == null) return;

        try {
            IGridConnection made = GridConnection.create(getProxy().getNode(), other.getProxy().getNode(),
                    AEPartLocation.INTERNAL);
            connection = made;
            other.connection = made;
            markForUpdate();
            other.markForUpdate();
        } catch (FailedConnectionException | IllegalStateException ignored) {
        }
    }

    private void destroyConnection() {
        IGridConnection old = connection;
        if (old == null) return;
        connection = null;
        try {
            appeng.api.networking.IGridNode otherNode = old.getOtherSide(getProxy().getNode());
            if (otherNode != null && otherNode.getMachine() instanceof WirelessConnectorTile) {
                WirelessConnectorTile other = (WirelessConnectorTile) otherNode.getMachine();
                if (other.connection == old) other.connection = null;
            }
        } catch (RuntimeException ignored) {
        }
        try {
            old.destroy();
        } catch (RuntimeException ignored) {
        }
    }

    @Override
    public void onChunkUnloaded() {
        unregister();
        destroyConnection();
        super.onChunkUnloaded();
    }

    @Override
    public void remove() {
        unregister();
        destroyConnection();
        super.remove();
    }

    @Override
    public void read(BlockState state, CompoundNBT data) {
        super.read(state, data);
        frequency = data.getLong("wirelessFrequency");
    }

    @Override
    public CompoundNBT write(CompoundNBT data) {
        super.write(data);
        data.putLong("wirelessFrequency", frequency);
        return data;
    }
}
