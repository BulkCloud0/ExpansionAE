package com.bulkcloud.expansionae.feature.disk;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;

public final class DiskItem extends Item {

    static final String TAG_DISK_ID = "ExpansionAEDiskId";
    static final String TAG_STORED_COUNT = "ExpansionAEStoredCount";

    private final long capacity;
    private final double idleDrain;

    public DiskItem(long capacity, double idleDrain, Properties properties) {
        super(properties);
        this.capacity = capacity;
        this.idleDrain = idleDrain;
    }

    public long getCapacity() {
        return this.capacity;
    }

    public double getIdleDrain() {
        return this.idleDrain;
    }

    public static boolean hasDiskId(ItemStack stack) {
        return stack.hasTag() && stack.getTag().hasUUID(TAG_DISK_ID);
    }

    @Nullable
    public static UUID getDiskId(ItemStack stack) {
        return hasDiskId(stack) ? stack.getTag().getUUID(TAG_DISK_ID) : null;
    }

    public static UUID getOrCreateDiskId(ItemStack stack) {
        UUID id = getDiskId(stack);
        if (id == null) {
            id = UUID.randomUUID();
            stack.getOrCreateTag().putUUID(TAG_DISK_ID, id);
        }
        return id;
    }

    public static void clearDiskId(ItemStack stack) {
        CompoundNBT tag = stack.getTag();
        if (tag != null) {
            tag.remove(TAG_DISK_ID);
        }
    }

    public static long getStoredCount(ItemStack stack) {
        CompoundNBT tag = stack.getTag();
        return tag == null ? 0L : Math.max(0L, tag.getLong(TAG_STORED_COUNT));
    }

    public static void setStoredCount(ItemStack stack, long count) {
        if (count <= 0L) {
            CompoundNBT tag = stack.getTag();
            if (tag != null) {
                tag.remove(TAG_STORED_COUNT);
            }
        } else {
            stack.getOrCreateTag().putLong(TAG_STORED_COUNT, count);
        }
    }
}
