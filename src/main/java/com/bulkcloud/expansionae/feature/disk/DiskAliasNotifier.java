package com.bulkcloud.expansionae.feature.disk;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
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
        if (!(inventory.getSaveProviderForAliasSync() instanceof IActionHost)) {
            return;
        }

        cleanupCollected();
        TRACKED.add(new WeakReference<>(inventory));
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
