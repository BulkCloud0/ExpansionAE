package dev.bulkcloud.expansionae;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraftforge.items.IItemHandler;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.config.PowerUnits;
import appeng.api.definitions.IItemDefinition;
import appeng.api.definitions.IMaterials;
import appeng.api.implementations.items.IAEItemPowerStorage;
import appeng.api.implementations.tiles.ICrankable;
import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.util.AECableType;
import appeng.api.util.AEPartLocation;
import appeng.api.util.DimensionalCoord;
import appeng.core.Api;
import appeng.core.settings.TickRates;
import appeng.me.GridAccessException;
import appeng.tile.grid.AENetworkPowerTileEntity;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.util.Platform;
import appeng.util.inv.InvOperation;
import appeng.util.inv.filter.IAEItemFilter;
import appeng.util.item.AEItemStack;

/**
 * Four-slot ExtendedAE charger backport.
 *
 * Each slot is processed independently while sharing the internal power buffer
 * and grid connection. This mirrors the multi-threaded 1.20 charger behavior
 * without introducing background threads into the 1.16 server tick.
 */
public final class ExpandedChargerTile extends AENetworkPowerTileEntity implements ICrankable, IGridTickable {
    public static final int SLOT_COUNT = 4;
    private static final int POWER_MAXIMUM_AMOUNT = 3200;
    private static final int POWER_THRESHOLD = POWER_MAXIMUM_AMOUNT - 1;
    private static final int POWER_PER_CRANK_TURN = 160;

    private final AppEngInternalInventory inv =
            new AppEngInternalInventory(this, SLOT_COUNT, 1, new ChargerInvFilter());
    private boolean working;

    public ExpandedChargerTile(TileEntityType<?> type) {
        super(type);
        this.getProxy().setValidSides(EnumSet.noneOf(Direction.class));
        this.getProxy().setFlags();
        this.setInternalMaxPower(POWER_MAXIMUM_AMOUNT);
        this.getProxy().setIdlePowerUsage(0);
    }

    public boolean isWorking() {
        return working;
    }

    @Override
    public AECableType getCableConnectionType(AEPartLocation dir) {
        return AECableType.COVERED;
    }

    @Override
    protected boolean readFromStream(PacketBuffer data) throws IOException {
        boolean changed = super.readFromStream(data);
        boolean previousWorking = working;
        working = data.readBoolean();
        changed |= previousWorking != working;
        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack old = inv.getStackInSlot(i);
            ItemStack next = ItemStack.EMPTY;
            if (data.readBoolean()) {
                IAEItemStack ae = AEItemStack.fromPacket(data);
                if (ae != null) {
                    next = ae.createItemStack();
                }
            }
            if (!ItemStack.areItemStacksEqual(old, next)) {
                inv.setStackInSlot(i, next);
                changed = true;
            }
        }
        return changed;
    }

    @Override
    protected void writeToStream(PacketBuffer data) throws IOException {
        super.writeToStream(data);
        data.writeBoolean(working);
        for (int i = 0; i < SLOT_COUNT; i++) {
            AEItemStack ae = AEItemStack.fromItemStack(inv.getStackInSlot(i));
            data.writeBoolean(ae != null);
            if (ae != null) {
                ae.writeToPacket(data);
            }
        }
    }

    @Override
    public void setOrientation(Direction forward, Direction up) {
        super.setOrientation(forward, up);
        EnumSet<Direction> sides = EnumSet.complementOf(EnumSet.of(this.getForward()));
        this.getProxy().setValidSides(sides);
        this.setPowerSides(sides);
    }

    @Override
    public boolean canTurn() {
        return getInternalCurrentPower() < getInternalMaxPower();
    }

    @Override
    public void applyTurn() {
        injectExternalPower(PowerUnits.AE, POWER_PER_CRANK_TURN, Actionable.MODULATE);
        processQuartzRecipes();
    }

    @Override
    public boolean canCrankAttach(Direction directionToCrank) {
        return directionToCrank != getForward();
    }

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

    public void activate(PlayerEntity player) {
        if (!Platform.hasPermissions(new DimensionalCoord(this), player)) {
            return;
        }

        ItemStack held = player.inventory.getCurrentItem();
        if (!held.isEmpty() && canChargeOrConvert(held)) {
            for (int i = 0; i < SLOT_COUNT; i++) {
                if (inv.getStackInSlot(i).isEmpty()) {
                    ItemStack one = player.inventory.decrStackSize(player.inventory.currentItem, 1);
                    inv.setStackInSlot(i, one);
                    return;
                }
            }
        }

        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack stored = inv.getStackInSlot(i);
            if (!stored.isEmpty()) {
                List<ItemStack> drops = new ArrayList<>();
                drops.add(stored);
                inv.setStackInSlot(i, ItemStack.EMPTY);
                Platform.spawnDrops(world, pos.offset(getForward()), drops);
                return;
            }
        }
    }

    private boolean canChargeOrConvert(ItemStack stack) {
        return Platform.isChargeable(stack)
                || Api.instance().definitions().materials().certusQuartzCrystal().isSameAs(stack);
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(TickRates.Charger.getMin(), TickRates.Charger.getMin(), false, true);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        return doWork() ? TickRateModulation.FASTER : TickRateModulation.SLOWER;
    }

    private boolean doWork() {
        boolean didWork = false;
        boolean nowWorking = false;

        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }

            if (Platform.isChargeable(stack)) {
                IAEItemPowerStorage storage = (IAEItemPowerStorage) stack.getItem();
                double missing = storage.getAEMaxPower(stack) - storage.getAECurrentPower(stack);
                if (missing > 0) {
                    double rate = Math.min(missing,
                            Api.instance().registries().charger().getChargeRate(stack.getItem()));
                    double extracted = extractAEPower(rate, Actionable.MODULATE, PowerMultiplier.CONFIG);
                    double stillNeeded = rate - extracted;
                    if (stillNeeded > 0) {
                        try {
                            extracted += getProxy().getEnergy().extractAEPower(stillNeeded,
                                    Actionable.MODULATE, PowerMultiplier.ONE);
                        } catch (GridAccessException ignored) {
                        }
                    }
                    if (extracted > 0) {
                        double returned = storage.injectAEPower(stack, extracted, Actionable.MODULATE);
                        setInternalCurrentPower(getInternalCurrentPower() + returned);
                        nowWorking = true;
                        didWork = true;
                    }
                }
            }
        }

        if (processQuartzRecipes()) {
            nowWorking = true;
            didWork = true;
        }

        if (getInternalCurrentPower() < POWER_THRESHOLD) {
            try {
                double toExtract = Math.min(800.0, getInternalMaxPower() - getInternalCurrentPower());
                double extracted = getProxy().getEnergy().extractAEPower(
                        toExtract, Actionable.MODULATE, PowerMultiplier.ONE);
                if (extracted > 0) {
                    injectExternalPower(PowerUnits.AE, extracted, Actionable.MODULATE);
                    didWork = true;
                }
            } catch (GridAccessException ignored) {
            }
        }

        if (working != nowWorking) {
            working = nowWorking;
            markForUpdate();
        } else if (didWork) {
            markForUpdate();
        }

        return didWork;
    }

    private boolean processQuartzRecipes() {
        if (getInternalCurrentPower() <= POWER_THRESHOLD) {
            return false;
        }

        IMaterials materials = Api.instance().definitions().materials();
        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (!materials.certusQuartzCrystal().isSameAs(stack)) {
                continue;
            }
            if (Platform.getRandomFloat() <= 0.8f) {
                continue;
            }

            extractAEPower(getInternalMaxPower(), Actionable.MODULATE, PowerMultiplier.CONFIG);
            final int slot = i;
            materials.certusQuartzCrystalCharged().maybeStack(stack.getCount())
                    .ifPresent(charged -> inv.setStackInSlot(slot, charged));
            return true;
        }
        return false;
    }

    private final class ChargerInvFilter implements IAEItemFilter {
        @Override
        public boolean allowInsert(IItemHandler handler, int slot, ItemStack stack) {
            IItemDefinition cert = Api.instance().definitions().materials().certusQuartzCrystal();
            return Platform.isChargeable(stack) || cert.isSameAs(stack);
        }

        @Override
        public boolean allowExtract(IItemHandler handler, int slot, int amount) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (Platform.isChargeable(stack)) {
                IAEItemPowerStorage storage = (IAEItemPowerStorage) stack.getItem();
                if (storage.getAECurrentPower(stack) >= storage.getAEMaxPower(stack)) {
                    return true;
                }
            }
            return Api.instance().definitions().materials().certusQuartzCrystalCharged().isSameAs(stack);
        }
    }
}
