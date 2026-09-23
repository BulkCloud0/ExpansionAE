package com.bulkcloud.expansionae.feature.disk;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.storage.cells.ISaveProvider;
import appeng.api.util.DimensionalCoord;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.data.IAEItemStack;
import appeng.me.helpers.BaseActionSource;

final class DiskAliasNotifier {
    private static final List<WeakReference<DiskCellInventory>> TRACKED = new ArrayList<>();

    private DiskAliasNotifier() {
    }

    static synchronized void track(DiskCellInventory inventory) {
        cleanupCollected();

        // Metadata aliases are relevant even outside an active AE2 host (for example,
        // a copied DISK opened from an inventory or workbench). Grid notifications
        // remain restricted by activeGrid(), but every live inventory view should
        // keep its cached tooltip counts aligned with the authoritative backing record.
        inventory.refreshCachedMetadataFromBacking();
        TRACKED.add(new WeakReference<>(inventory));
    }

    static synchronized void clear() {
        TRACKED.clear();
    }

    static void notifyOtherGrids(
            DiskCellInventory origin,
            IAEItemStack change,
            IActionSource source) {
        UUID uuid = origin.getUuidForAliasSync();
        if (uuid == null || change == null || change.getStackSize() == 0) {
            return;
        }

        IGrid originGrid = activeGrid(origin);
        Set<IGrid> targets = Collections.newSetFromMap(new IdentityHashMap<>());

        synchronized (DiskAliasNotifier.class) {
            Iterator<WeakReference<DiskCellInventory>> it = TRACKED.iterator();
            while (it.hasNext()) {
                DiskCellInventory inventory = it.next().get();
                if (inventory == null) {
                    it.remove();
                    continue;
                }

                if (!uuid.equals(inventory.getUuidForAliasSync())) {
                    continue;
                }

                if (inventory != origin) {
                    inventory.refreshCachedMetadataFromBacking();
                }

                IGrid grid = activeGrid(inventory);
                if (grid != null && grid != originGrid) {
                    targets.add(grid);
                }
            }
        }

        if (targets.isEmpty()) {
            return;
        }

        IActionSource eventSource = source != null ? source : new BaseActionSource();

        for (IGrid grid : targets) {
            IStorageGrid storage = grid.getCache(IStorageGrid.class);
            IAEItemStack delta = change.copy();
            storage.postAlterationOfStoredItems(
                    origin.getChannel(),
                    Collections.singletonList(delta),
                    eventSource);
        }
    }


    static synchronized List<AliasSnapshot> snapshotLoadedAliases(UUID uuid) {
        if (uuid == null) {
            return Collections.emptyList();
        }

        cleanupCollected();
        List<AliasSnapshot> snapshots = new ArrayList<>();
        Set<Object> seenHostedProviders =
                Collections.newSetFromMap(new IdentityHashMap<>());

        for (WeakReference<DiskCellInventory> reference : TRACKED) {
            DiskCellInventory inventory = reference.get();
            if (inventory == null || !uuid.equals(inventory.getUuidForAliasSync())) {
                continue;
            }

            ISaveProvider saveProvider = inventory.getSaveProviderForAliasSync();
            if (saveProvider != null && !seenHostedProviders.add(saveProvider)) {
                continue;
            }

            snapshots.add(describeAlias(saveProvider));
        }

        snapshots.sort(Comparator.comparing(AliasSnapshot::sortKey));
        return Collections.unmodifiableList(snapshots);
    }

    private static AliasSnapshot describeAlias(ISaveProvider saveProvider) {
        String hostType = saveProvider == null
                ? "<unhosted>"
                : saveProvider.getClass().getName();

        if (!(saveProvider instanceof IActionHost)) {
            return new AliasSnapshot(hostType, null, null, null, null, null);
        }

        IGridNode node = ((IActionHost) saveProvider).getActionableNode();
        if (node == null) {
            return new AliasSnapshot(hostType, Boolean.FALSE, null, null, null, null);
        }

        Boolean active = node.isActive();
        try {
            DimensionalCoord location = node.getGridBlock().getLocation();
            if (location == null || location.getWorld() == null) {
                return new AliasSnapshot(hostType, active, null, null, null, null);
            }

            String dimension = location.getWorld()
                    .getDimensionKey()
                    .getLocation()
                    .toString();
            return new AliasSnapshot(
                    hostType,
                    active,
                    dimension,
                    location.x,
                    location.y,
                    location.z);
        } catch (RuntimeException unavailableLocation) {
            return new AliasSnapshot(hostType, active, null, null, null, null);
        }
    }

    static final class AliasSnapshot {
        private final String hostType;
        private final Boolean active;
        private final String dimension;
        private final Integer x;
        private final Integer y;
        private final Integer z;

        private AliasSnapshot(
                String hostType,
                Boolean active,
                String dimension,
                Integer x,
                Integer y,
                Integer z) {
            this.hostType = hostType;
            this.active = active;
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        String getHostType() {
            return hostType;
        }

        Boolean isActive() {
            return active;
        }

        String getDimension() {
            return dimension;
        }

        Integer getX() {
            return x;
        }

        Integer getY() {
            return y;
        }

        Integer getZ() {
            return z;
        }

        boolean hasLocation() {
            return dimension != null && x != null && y != null && z != null;
        }

        private String sortKey() {
            return (dimension == null ? "~" : dimension)
                    + "|" + (x == null ? Integer.MAX_VALUE : x)
                    + "|" + (y == null ? Integer.MAX_VALUE : y)
                    + "|" + (z == null ? Integer.MAX_VALUE : z)
                    + "|" + hostType;
        }
    }

    private static IGrid activeGrid(DiskCellInventory inventory) {
        if (!(inventory.getSaveProviderForAliasSync() instanceof IActionHost)) {
            return null;
        }

        IGridNode node = ((IActionHost) inventory.getSaveProviderForAliasSync()).getActionableNode();
        if (node == null || !node.isActive()) {
            return null;
        }

        return node.getGrid();
    }

    private static void cleanupCollected() {
        Iterator<WeakReference<DiskCellInventory>> it = TRACKED.iterator();
        while (it.hasNext()) {
            if (it.next().get() == null) {
                it.remove();
            }
        }
    }
}
