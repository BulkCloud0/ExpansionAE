package com.bulkcloud.expansionae.feature.disk;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

public final class DiskStorageData extends WorldSavedData {

    private static final String DATA_NAME = "expansionae_disk_storage";
    private static final String TAG_DISKS = "disks";
    private static final String TAG_DISK_ID = "id";
    private static final String TAG_STACKS = "stacks";

    private final Map<UUID, ListNBT> disks = new HashMap<>();

    public DiskStorageData() {
        super(DATA_NAME);
    }

    @Override
    public void load(CompoundNBT root) {
        this.disks.clear();

        ListNBT diskList = root.getList(TAG_DISKS, Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < diskList.size(); i++) {
            CompoundNBT diskTag = diskList.getCompound(i);
            if (!diskTag.hasUUID(TAG_DISK_ID)) {
                continue;
            }

            UUID id = diskTag.getUUID(TAG_DISK_ID);
            ListNBT stacks = diskTag.getList(TAG_STACKS, Constants.NBT.TAG_COMPOUND);
            this.disks.put(id, stacks);
        }
    }

    @Override
    public CompoundNBT save(CompoundNBT root) {
        ListNBT diskList = new ListNBT();

        for (Map.Entry<UUID, ListNBT> entry : this.disks.entrySet()) {
            CompoundNBT diskTag = new CompoundNBT();
            diskTag.putUUID(TAG_DISK_ID, entry.getKey());
            diskTag.put(TAG_STACKS, entry.getValue());
            diskList.add(diskTag);
        }

        root.put(TAG_DISKS, diskList);
        return root;
    }

    public ListNBT getContents(UUID id) {
        ListNBT stacks = this.disks.get(id);
        return stacks == null ? new ListNBT() : stacks;
    }

    public void putContents(UUID id, ListNBT stacks) {
        this.disks.put(id, stacks);
        this.setDirty();
    }

    public void remove(UUID id) {
        if (this.disks.remove(id) != null) {
            this.setDirty();
        }
    }

    @Nullable
    public static DiskStorageData get() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return null;
        }

        ServerWorld overworld = server.overworld();
        if (overworld == null) {
            return null;
        }

        return overworld.getDataStorage().computeIfAbsent(DiskStorageData::new, DATA_NAME);
    }
}
