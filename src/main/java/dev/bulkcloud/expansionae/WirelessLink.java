package dev.bulkcloud.expansionae;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import appeng.api.exceptions.FailedConnectionException;
import appeng.api.networking.IGridConnection;
import appeng.api.networking.IGridNode;
import appeng.api.util.AEPartLocation;
import appeng.me.GridConnection;

/**
 * Reusable one-to-one wireless edge used by connectors and individual hub ports.
 */
final class WirelessLink {
    static final double MAX_RANGE = 1000.0D;

    private static final Map<Long, List<WeakReference<WirelessLink>>> ACTIVE =
            new ConcurrentHashMap<Long, List<WeakReference<WirelessLink>>>();

    private final WirelessLinkEndpoint endpoint;
    private long frequency;
    private int retryTicks;
    private IGridConnection connection;
    private WirelessLink peer;

    WirelessLink(WirelessLinkEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    long getFrequency() {
        return frequency;
    }

    void setFrequency(long value) {
        if (frequency == value) {
            return;
        }
        unregister();
        disconnect();
        frequency = value;
        retryTicks = 0;
        register();
        endpoint.wirelessChanged();
    }

    boolean isConnected() {
        return connection != null && peer != null && !peer.endpoint.wirelessRemoved();
    }

    double getDistance() {
        return peer == null ? 0.0D
                : Math.sqrt(endpoint.wirelessPos().distanceSq(peer.endpoint.wirelessPos()));
    }

    void tick() {
        if (frequency == 0L || endpoint.wirelessWorld() == null
                || endpoint.wirelessWorld().isRemote || endpoint.wirelessRemoved()) {
            return;
        }

        register();

        if (peer != null) {
            if (!canRemainConnected(peer)) {
                disconnect();
            } else {
                return;
            }
        }

        if (retryTicks-- > 0) {
            return;
        }
        retryTicks = 20;

        WirelessLink candidate = findPeer();
        if (candidate == null) {
            return;
        }

        IGridNode a = endpoint.wirelessNode();
        IGridNode b = candidate.endpoint.wirelessNode();
        if (a == null || b == null || !endpoint.wirelessActive() || !candidate.endpoint.wirelessActive()) {
            return;
        }

        try {
            IGridConnection edge = GridConnection.create(a, b, AEPartLocation.INTERNAL);
            connection = edge;
            peer = candidate;
            candidate.connection = edge;
            candidate.peer = this;
            endpoint.wirelessChanged();
            candidate.endpoint.wirelessChanged();
        } catch (FailedConnectionException | IllegalStateException ignored) {
            // AE2 may be rebuilding either grid. Retry after the normal backoff.
        }
    }

    void onReady() {
        register();
        retryTicks = 0;
    }

    void close() {
        unregister();
        disconnect();
    }

    private boolean canRemainConnected(WirelessLink other) {
        return other != null
                && !other.endpoint.wirelessRemoved()
                && other.frequency == frequency
                && other.endpoint.wirelessWorld() == endpoint.wirelessWorld()
                && other.endpoint.wirelessOwner() != endpoint.wirelessOwner()
                && endpoint.wirelessPos().distanceSq(other.endpoint.wirelessPos()) <= MAX_RANGE * MAX_RANGE
                && endpoint.wirelessActive()
                && other.endpoint.wirelessActive();
    }

    private WirelessLink findPeer() {
        List<WeakReference<WirelessLink>> entries = ACTIVE.get(frequency);
        if (entries == null) {
            return null;
        }
        synchronized (entries) {
            Iterator<WeakReference<WirelessLink>> it = entries.iterator();
            while (it.hasNext()) {
                WirelessLink other = it.next().get();
                if (other == null || other.endpoint.wirelessRemoved() || other.frequency != frequency) {
                    it.remove();
                    continue;
                }
                if (other != this && canRemainConnected(other) && other.peer == null) {
                    return other;
                }
            }
        }
        return null;
    }

    private void register() {
        if (frequency == 0L || endpoint.wirelessWorld() == null
                || endpoint.wirelessWorld().isRemote || endpoint.wirelessRemoved()) {
            return;
        }

        List<WeakReference<WirelessLink>> entries = ACTIVE.get(frequency);
        if (entries == null) {
            List<WeakReference<WirelessLink>> created = new ArrayList<WeakReference<WirelessLink>>();
            List<WeakReference<WirelessLink>> previous = ACTIVE.putIfAbsent(frequency, created);
            entries = previous == null ? created : previous;
        }

        synchronized (entries) {
            Iterator<WeakReference<WirelessLink>> it = entries.iterator();
            while (it.hasNext()) {
                WirelessLink existing = it.next().get();
                if (existing == null || existing.endpoint.wirelessRemoved()) {
                    it.remove();
                } else if (existing == this) {
                    return;
                }
            }
            entries.add(new WeakReference<WirelessLink>(this));
        }
    }

    private void unregister() {
        if (frequency == 0L) {
            return;
        }
        List<WeakReference<WirelessLink>> entries = ACTIVE.get(frequency);
        if (entries == null) {
            return;
        }
        synchronized (entries) {
            Iterator<WeakReference<WirelessLink>> it = entries.iterator();
            while (it.hasNext()) {
                WirelessLink existing = it.next().get();
                if (existing == null || existing == this) {
                    it.remove();
                }
            }
            if (entries.isEmpty()) {
                ACTIVE.remove(frequency);
            }
        }
    }

    private void disconnect() {
        IGridConnection old = connection;
        WirelessLink oldPeer = peer;
        connection = null;
        peer = null;

        if (oldPeer != null && oldPeer.peer == this) {
            oldPeer.connection = null;
            oldPeer.peer = null;
            oldPeer.endpoint.wirelessChanged();
        }

        if (old != null) {
            try {
                old.destroy();
            } catch (RuntimeException ignored) {
            }
        }
        endpoint.wirelessChanged();
    }
}
