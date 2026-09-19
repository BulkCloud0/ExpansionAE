package dev.bulkcloud.expansionae;

import java.util.EnumSet;

import javax.annotation.Nonnull;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraftforge.items.IItemHandler;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.definitions.IItemDefinition;
import appeng.api.implementations.items.IGrowableCrystal;
import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.util.AECableType;
import appeng.api.util.AEPartLocation;
import appeng.api.util.DimensionalCoord;
import appeng.core.Api;
import appeng.me.GridAccessException;
import appeng.tile.grid.AENetworkInvTileEntity;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.util.Platform;
import appeng.util.inv.InvOperation;
import appeng.util.inv.filter.IAEItemFilter;

/**
 * 1.16.5 semantic backport of ExtendedAE's Crystal Fixer.
 *
 * Modern AE2 repairs degraded budding-certus blocks, but AE2 8.4 predates that
 * mechanic and grows quartz from Crystal Seeds. This machine therefore repairs
 * the old progression directly: a growable crystal seed in slot 0 is advanced
 * using ME power and charged Certus from slot 1.
 */
public final class CrystalFixerTile extends AENetworkInvTileEntity implements IGridTickable {
    public static final int TARGET_SLOT = 0;
    public static final int FUEL_SLOT = 1;

    private static final int CATALYST_STEPS = 100;
    private static final double AE_PER_GROWTH_STEP = 50.0;

    private final AppEngInternalInventory inv =
            new AppEngInternalInventory(this, 2, 64, new FixerFilter());
    private int catalystSteps;

    public CrystalFixerTile(TileEntityType<?> type) {
        super(type);
        getProxy().setFlags();
        getProxy().setIdlePowerUsage(0);
        getProxy().setValidSides(EnumSet.allOf(Direction.class));
    }

    @Override
    public AECableType getCableConnectionType(AEPartLocation dir) {
        return AECableType.COVERED;
    }

    @Override
    public DimensionalCoord getLocation() {
        return new DimensionalCoord(this);
    }

    @Nonnull
    @Override
    public IItemHandler getInternalInventory() {
        return inv;
    }

    @Override
    public void onChangeInventory(IItemHandler inventory, int slot, InvOperation operation,
            ItemStack removed, ItemStack added) {
        try {
            getProxy().getTick().wakeDevice(getProxy().getNode());
        } catch (GridAccessException ignored) {
        }
        markForUpdate();
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(2, 10, false, false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        int steps = Math.min(20, Math.max(1, ticksSinceLastCall));
        boolean worked = false;
        for (int i = 0; i < steps; i++) {
            if (!growOnce()) {
                break;
            }
            worked = true;
        }
        return worked ? TickRateModulation.FASTER : TickRateModulation.SLOWER;
    }

    private boolean growOnce() {
        ItemStack target = inv.getStackInSlot(TARGET_SLOT);
        if (target.isEmpty() || !(target.getItem() instanceof IGrowableCrystal)) {
            return false;
        }

        double available;
        try {
            available = getProxy().getEnergy().extractAEPower(
                    AE_PER_GROWTH_STEP, Actionable.SIMULATE, PowerMultiplier.ONE);
        } catch (GridAccessException ignored) {
            return false;
        }
        if (available + 0.0001 < AE_PER_GROWTH_STEP) {
            return false;
        }

        if (catalystSteps <= 0) {
            ItemStack fuel = inv.getStackInSlot(FUEL_SLOT);
            if (!isChargedCertus(fuel)) {
                return false;
            }
            inv.extractItem(FUEL_SLOT, 1, false);
            catalystSteps = CATALYST_STEPS;
        }

        try {
            getProxy().getEnergy().extractAEPower(
                    AE_PER_GROWTH_STEP, Actionable.MODULATE, PowerMultiplier.ONE);
        } catch (GridAccessException ignored) {
            return false;
        }

        IGrowableCrystal growable = (IGrowableCrystal) target.getItem();
        ItemStack grown = growable.triggerGrowth(target);
        catalystSteps--;

        if (grown == null || grown.isEmpty()) {
            inv.setStackInSlot(TARGET_SLOT, ItemStack.EMPTY);
        } else if (grown != target || grown.getItem() != target.getItem()) {
            inv.setStackInSlot(TARGET_SLOT, grown);
        } else {
            markDirty();
        }

        markForUpdate();
        return true;
    }

    public void activate(PlayerEntity player) {
        if (!Platform.hasPermissions(new DimensionalCoord(this), player)) {
            return;
        }

        ItemStack held = player.inventory.getCurrentItem();
        if (!held.isEmpty()) {
            int slot = held.getItem() instanceof IGrowableCrystal ? TARGET_SLOT
                    : isChargedCertus(held) ? FUEL_SLOT : -1;
            if (slot >= 0) {
                ItemStack remainder = inv.insertItem(slot, held, false);
                player.inventory.setInventorySlotContents(player.inventory.currentItem, remainder);
            }
            return;
        }

        ItemStack extracted = inv.extractItem(TARGET_SLOT, Integer.MAX_VALUE, false);
        if (extracted.isEmpty()) {
            extracted = inv.extractItem(FUEL_SLOT, Integer.MAX_VALUE, false);
        }
        if (!extracted.isEmpty() && !player.inventory.addItemStackToInventory(extracted)) {
            Platform.spawnDrops(world, pos.offset(getForward()), java.util.Collections.singletonList(extracted));
        }
    }

    public int getCatalystSteps() {
        return catalystSteps;
    }

    private static boolean isChargedCertus(ItemStack stack) {
        IItemDefinition charged = Api.instance().definitions().materials().certusQuartzCrystalCharged();
        return charged.isSameAs(stack);
    }

    @Override
    public void read(net.minecraft.block.BlockState state, CompoundNBT data) {
        super.read(state, data);
        catalystSteps = Math.max(0, data.getInt("catalystSteps"));
    }

    @Override
    public CompoundNBT write(CompoundNBT data) {
        super.write(data);
        data.putInt("catalystSteps", catalystSteps);
        return data;
    }

    private final class FixerFilter implements IAEItemFilter {
        @Override
        public boolean allowInsert(IItemHandler handler, int slot, ItemStack stack) {
            if (slot == TARGET_SLOT) {
                return stack.getItem() instanceof IGrowableCrystal;
            }
            return slot == FUEL_SLOT && isChargedCertus(stack);
        }

        @Override
        public boolean allowExtract(IItemHandler handler, int slot, int amount) {
            return true;
        }
    }
}
