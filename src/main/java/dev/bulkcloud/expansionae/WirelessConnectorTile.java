package dev.bulkcloud.expansionae;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import appeng.api.exceptions.FailedConnectionException;
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

/**
 * 1.16.5 semantic backport of ExtendedAE's Wireless Connector.
 *
 * Two connectors sharing the same frequency create an internal AE2 grid
 * connection while both chunks are loaded, both proxies are active, they are
 * in the same dimension and they are within the upstream default 1000 block
 * range. The physical AE cable connections remain exposed on every side.
 */
public final class WirelessConnectorTile extends AENetworkTileEntity implements ITickableTileEntity {
    public static final double MAX_RANGE = 1000.0D;

    private static final Map<Long, List<WeakReference<WirelessConnectorTile>>> ACTIVE =
            new ConcurrentHashMap<Long, List<WeakReference<WirelessConnectorTile>>>();

    private long frequency;
    private int retryTicks;
    private IGridConnection wirelessConnection;
    private WirelessConnectorTile peer;

    public WirelessConnectorTile(TileEntityType<?> type) {
        super(type);
        getProxy().setValidSides(EnumSet.allOf(Direction.class));
        getProxy().setIdlePowerUsage(1.0D);
    }

    public long getFrequency() {
        return frequency;
    }

    public boolean isWirelessConnected() {
        return wirelessConnection != null && peer != null && !peer.isRemoved();
    }

    public double getWirelessDistance() {
        if (peer == null) {
            return 0.0D;
        }
        return Math.sqrt(pos.distanceSq(peer.pos));
    }

    public void setFrequency(long newFrequency) {
        if (frequency == newFrequency) {
            return;
        }
        unregister();
        disconnectWireless();
        frequency = newFrequency;
        retryTicks = 0;
        register();
        markDirty();
        markForUpdate();
    }

    @Override
    public void tick() {
        if (world == null || world.isRemote || frequency == 0L) {
            return;
        }

        register();

        if (peer != null) {
            if (peer.isRemoved() || peer.world != world || peer.frequency != frequency
                    || pos.distanceSq(peer.pos) > MAX_RANGE * MAX_RANGE) {
                disconnectWireless();
            } else {
                return;
            }
        }

        if (retryTicks-- > 0) {
            return;
        }
        retryTicks = 20;

        WirelessConnectorTile candidate = findPeer();
        if (candidate == null || candidate.getProxy().getNode() == null || getProxy().getNode() == null) {
            return;
        }
        if (!candidate.getProxy().isActive() || !getProxy().isActive()) {
            return;
        }

        try {
            IGridConnection connection = GridConnection.create(
                    getProxy().getNode(), candidate.getProxy().getNode(), AEPartLocation.INTERNAL);
            wirelessConnection = connection;
            peer = candidate;
            candidate.wirelessConnection = connection;
            candidate.peer = this;
            updatePowerUsage();
            candidate.updatePowerUsage();
            markForUpdate();
            candidate.markForUpdate();
        } catch (FailedConnectionException | IllegalStateException ignored) {
            // The nodes may already share a grid path or be transitioning state.
            // Retrying one second later is safer than forcing a duplicate edge.
        }
    }

    private void updatePowerUsage() {
        double distance = Math.max(getWirelessDistance(), Math.E);
        getProxy().setIdlePowerUsage(Math.max(1.0D, distance * Math.log(distance)));
    }

    private WirelessConnectorTile findPeer() {
        List<WeakReference<WirelessConnectorTile>> entries = ACTIVE.get(frequency);
        if (entries == null) {
            return null;
        }

        synchronized (entries) {
            Iterator<WeakReference<WirelessConnectorTile>> it = entries.iterator();
            while (it.hasNext()) {
                WirelessConnectorTile other = it.next().get();
                if (other == null || other.isRemoved() || other.frequency != frequency) {
                    it.remove();
                    continue;
                }
                if (other != this && other.world == world
                        && other.pos.distanceSq(pos) <= MAX_RANGE * MAX_RANGE) {
                    return other;
                }
            }
        }
        return null;
    }

    private void register() {
        if (frequency == 0L || world == null || world.isRemote) {
            return;
        }

        List<WeakReference<WirelessConnectorTile>> entries = ACTIVE.get(frequency);
        if (entries == null) {
            List<WeakReference<WirelessConnectorTile>> created =
                    new ArrayList<WeakReference<WirelessConnectorTile>>();
            List<WeakReference<WirelessConnectorTile>> previous = ACTIVE.putIfAbsent(frequency, created);
            entries = previous == null ? created : previous;
        }

        synchronized (entries) {
            Iterator<WeakReference<WirelessConnectorTile>> it = entries.iterator();
            while (it.hasNext()) {
                WirelessConnectorTile existing = it.next().get();
                if (existing == null || existing.isRemoved()) {
                    it.remove();
                } else if (existing == this) {
                    return;
                }
            }
            entries.add(new WeakReference<WirelessConnectorTile>(this));
        }
    }

    private void unregister() {
        if (frequency == 0L) {
            return;
        }

        List<WeakReference<WirelessConnectorTile>> entries = ACTIVE.get(frequency);
        if (entries == null) {
            return;
        }
        synchronized (entries) {
            Iterator<WeakReference<WirelessConnectorTile>> it = entries.iterator();
            while (it.hasNext()) {
                WirelessConnectorTile existing = it.next().get();
                if (existing == null || existing == this) {
                    it.remove();
                }
            }
            if (entries.isEmpty()) {
                ACTIVE.remove(frequency);
            }
        }
    }

    private void disconnectWireless() {
        IGridConnection old = wirelessConnection;
        WirelessConnectorTile oldPeer = peer;
        wirelessConnection = null;
        peer = null;
        getProxy().setIdlePowerUsage(1.0D);

        if (oldPeer != null && oldPeer.peer == this) {
            oldPeer.wirelessConnection = null;
            oldPeer.peer = null;
            oldPeer.getProxy().setIdlePowerUsage(1.0D);
            oldPeer.markForUpdate();
        }

        if (old != null) {
            try {
                old.destroy();
            } catch (RuntimeException ignored) {
            }
        }
    }

    @Override
    public void read(BlockState state, CompoundNBT data) {
        super.read(state, data);
        frequency = data.getLong("wirelessFrequency");
        retryTicks = 0;
    }

    @Override
    public CompoundNBT write(CompoundNBT data) {
        super.write(data);
        data.putLong("wirelessFrequency", frequency);
        return data;
    }

    @Override
    public void onReady() {
        super.onReady();
        register();
        retryTicks = 0;
    }

    @Override
    public void onChunkUnloaded() {
        unregister();
        disconnectWireless();
        super.onChunkUnloaded();
    }

    @Override
    public void remove() {
        unregister();
        disconnectWireless();
        super.remove();
    }

    @Override
    public AECableType getCableConnectionType(AEPartLocation dir) {
        return AECableType.DENSE_SMART;
    }
}
