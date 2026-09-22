package com.bulkcloud.expansionae.feature.growth;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;

import com.bulkcloud.expansionae.core.registry.ExpansionAETileEntities;

import appeng.api.implementations.items.IGrowableCrystal;
import appeng.entity.GrowingCrystalEntity;
import appeng.tile.misc.QuartzGrowthAcceleratorTileEntity;

public final class BoostedGrowthAcceleratorTileEntity
        extends QuartzGrowthAcceleratorTileEntity
        implements net.minecraft.tileentity.ITickableTileEntity {

    public static final int POWER_PER_TICK = 24;
    public static final int VANILLA_SINGLE_ACCELERATOR_PROGRESS = 40;
    public static final int SPEED_MULTIPLIER = 8;
    public static final int EXTRA_PROGRESS_PER_TICK =
            VANILLA_SINGLE_ACCELERATOR_PROGRESS * (SPEED_MULTIPLIER - 1);

    private static final String TAG_EXTRA_PROGRESS =
            "expansionae_boosted_growth_progress_1000";

    public BoostedGrowthAcceleratorTileEntity() {
        super(ExpansionAETileEntities.BOOSTED_GROWTH_ACCELERATOR.get());
        getProxy().setIdlePowerUsage(POWER_PER_TICK);
    }

    @Override
    public void tick() {
        if (world == null || world.isRemote || !isPowered()) {
            return;
        }

        Set<Integer> processed = new HashSet<>();
        for (Direction direction : Direction.values()) {
            BlockPos growthPos = pos.offset(direction);
            AxisAlignedBB box = new AxisAlignedBB(growthPos);

            for (GrowingCrystalEntity entity :
                    world.getEntitiesWithinAABB(GrowingCrystalEntity.class, box)) {
                if (processed.add(entity.getEntityId())) {
                    applyExtraGrowth(entity, growthPos);
                }
            }
        }
    }

    static void applyExtraGrowth(GrowingCrystalEntity entity, BlockPos growthPos) {
        if (!(entity.world instanceof ServerWorld)) {
            return;
        }

        ItemStack current = entity.getItem();
        Item item = current.getItem();
        if (!(item instanceof IGrowableCrystal)) {
            return;
        }

        IGrowableCrystal growable = (IGrowableCrystal) item;
        BlockState state = entity.world.getBlockState(growthPos);
        float materialMultiplier = growable.getMultiplier(state, entity.world, growthPos);
        if (materialMultiplier <= 0) {
            entity.getPersistentData().remove(TAG_EXTRA_PROGRESS);
            return;
        }

        int addedProgress = Math.max(
                1,
                (int) (EXTRA_PROGRESS_PER_TICK * materialMultiplier));

        int accumulated = entity.getPersistentData().getInt(TAG_EXTRA_PROGRESS);
        accumulated = Math.min(Integer.MAX_VALUE - addedProgress, accumulated) + addedProgress;

        while (accumulated >= 1000) {
            ItemStack before = entity.getItem();
            Item beforeItem = before.getItem();

            if (!(beforeItem instanceof IGrowableCrystal)) {
                accumulated = 0;
                break;
            }

            ItemStack next = ((IGrowableCrystal) beforeItem).triggerGrowth(before.copy());
            if (next == null) {
                accumulated = 0;
                break;
            }

            entity.setItem(next);
            accumulated -= 1000;

            if (next.getItem() != beforeItem) {
                accumulated = 0;
                break;
            }
        }

        if (accumulated > 0) {
            entity.getPersistentData().putInt(TAG_EXTRA_PROGRESS, accumulated);
        } else {
            entity.getPersistentData().remove(TAG_EXTRA_PROGRESS);
        }
    }

    public static int getExtraProgressPerTick() {
        return EXTRA_PROGRESS_PER_TICK;
    }
}
