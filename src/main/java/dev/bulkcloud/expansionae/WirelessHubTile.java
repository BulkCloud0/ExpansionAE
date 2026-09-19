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
 * Eight-port semantic backport of ExtendedAE's Wireless Hub.
 *
 * Every configured port participates as an independent one-to-one wireless
 * edge while all eight edges terminate on this tile's single AE2 grid node.
 */
public final class WirelessHubTile extends AENetworkTileEntity implements ITickableTileEntity {
    public static final int MAX_PORTS = 8;

    private final WirelessLink[] links = new WirelessLink[MAX_PORTS];
    private final PortEndpoint[] endpoints = new PortEndpoint[MAX_PORTS];

    public WirelessHubTile(TileEntityType<?> type) {
        super(type);
        getProxy().setValidSides(EnumSet.allOf(Direction.class));
        getProxy().setIdlePowerUsage(1.0D);
        for (int i = 0; i < MAX_PORTS; i++) {
            endpoints[i] = new PortEndpoint(i);
            links[i] = new WirelessLink(endpoints[i]);
        }
    }

    public int allocatePort() {
        for (int i = 0; i < MAX_PORTS; i++) {
            if (links[i].getFrequency() == 0L) {
                return i;
            }
        }
        return -1;
    }

    public void setFrequency(int port, long frequency) {
        if (port < 0 || port >= MAX_PORTS) {
            return;
        }
        links[port].setFrequency(frequency);
        markDirty();
        markForUpdate();
    }

    public void clearPort(int port) {
        setFrequency(port, 0L);
    }

    public long getFrequency(int port) {
        return port >= 0 && port < MAX_PORTS ? links[port].getFrequency() : 0L;
    }

    public boolean isPortConnected(int port) {
        return port >= 0 && port < MAX_PORTS && links[port].isConnected();
    }

    public int getConfiguredPortCount() {
        int count = 0;
        for (WirelessLink link : links) {
            if (link.getFrequency() != 0L) {
                count++;
            }
        }
        return count;
    }

    public int getConnectedPortCount() {
        int count = 0;
        for (WirelessLink link : links) {
            if (link.isConnected()) {
                count++;
            }
        }
        return count;
    }

    @Override
    public void tick() {
        for (WirelessLink link : links) {
            link.tick();
        }
    }

    private void updatePowerUsage() {
        double total = 0.0D;
        boolean connected = false;
        for (WirelessLink link : links) {
            if (link.isConnected()) {
                double d = Math.max(link.getDistance(), Math.E);
                total += Math.max(1.0D, d * Math.log(d));
                connected = true;
            }
        }
        getProxy().setIdlePowerUsage(connected ? Math.max(1.0D, total) : 1.0D);
        markForUpdate();
    }

    @Override
    public void read(BlockState state, CompoundNBT data) {
        super.read(state, data);
        for (int i = 0; i < MAX_PORTS; i++) {
            links[i].setFrequency(data.getLong("wirelessFrequency" + i));
        }
    }

    @Override
    public CompoundNBT write(CompoundNBT data) {
        super.write(data);
        for (int i = 0; i < MAX_PORTS; i++) {
            data.putLong("wirelessFrequency" + i, links[i].getFrequency());
        }
        return data;
    }

    @Override
    public void onReady() {
        super.onReady();
        for (WirelessLink link : links) {
            link.onReady();
        }
    }

    private void closeLinks() {
        for (WirelessLink link : links) {
            link.close();
        }
    }

    @Override
    public void onChunkUnloaded() {
        closeLinks();
        super.onChunkUnloaded();
    }

    @Override
    public void remove() {
        closeLinks();
        super.remove();
    }

    @Override
    public AECableType getCableConnectionType(AEPartLocation dir) {
        return AECableType.DENSE_SMART;
    }

    private final class PortEndpoint implements WirelessLinkEndpoint {
        private final int port;

        private PortEndpoint(int port) {
            this.port = port;
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
            return WirelessHubTile.this.isRemoved();
        }

        @Override
        public Object wirelessOwner() {
            return WirelessHubTile.this;
        }

        @Override
        public void wirelessChanged() {
            updatePowerUsage();
            if (links[port].getFrequency() != 0L) {
                markDirty();
            }
        }
    }
}
