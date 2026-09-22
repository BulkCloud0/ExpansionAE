package com.bulkcloud.expansionae.feature.disk;

import javax.annotation.Nullable;

import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.world.WorldEvent;

public final class DiskStorageService {
    @Nullable
    private static DiskStorageData current;

    private DiskStorageService() {
    }

    public static void onWorldLoad(WorldEvent.Load event) {
        if (!(event.getWorld() instanceof ServerWorld)) {
            return;
        }

        ServerWorld world = (ServerWorld) event.getWorld();
        if (world.getDimensionKey() == World.OVERWORLD) {
            current = DiskStorageData.get(world);
        }
    }

    public static void onWorldUnload(WorldEvent.Unload event) {
        if (!(event.getWorld() instanceof ServerWorld)) {
            return;
        }

        ServerWorld world = (ServerWorld) event.getWorld();
        if (world.getDimensionKey() == World.OVERWORLD) {
            current = null;
        }
    }

    @Nullable
    public static DiskStorageData getCurrent() {
        return current;
    }
}
