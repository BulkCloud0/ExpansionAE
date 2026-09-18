package dev.bulkcloud.expansionae;

import com.google.common.collect.ImmutableSet;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.config.RedstoneMode;
import appeng.api.config.SchedulingMode;
import appeng.api.config.Settings;
import appeng.api.config.Upgrades;
import appeng.api.config.YesNo;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.container.ContainerLocator;
import appeng.container.ContainerOpener;
import appeng.core.AELog;
import appeng.core.Api;
import appeng.helpers.MultiCraftingTracker;
import appeng.me.GridAccessException;
import appeng.me.helpers.MachineSource;
import appeng.parts.automation.ExportBusPart;
import appeng.util.InventoryAdaptor;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Hand;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.items.IItemHandler;

/**
 * ExtendedAE precise export bus backport.
 *
 * The configured stack count is the batch size. Exact mode only transfers a
 * complete batch. Multiple mode transfers the largest complete multiple that
 * fits in the current transfer budget and destination.
 */
public final class PreciseExportBus extends ExportBusPart {
    private final IActionSource source = new MachineSource(this);
    private final MultiCraftingTracker craftingTracker = new MultiCraftingTracker(this, 9);
    private boolean multipleMode;
    private int nextSlot;

    public PreciseExportBus(ItemStack stack) {
        super(stack);
    }

    public boolean isMultipleMode() {
        return multipleMode;
    }

    public void setMultipleMode(boolean multipleMode) {
        if (this.multipleMode != multipleMode) {
            this.multipleMode = multipleMode;
            getHost().markForSave();
        }
    }

    @Override
    public void readFromNBT(CompoundNBT extra) {
        super.readFromNBT(extra);
        craftingTracker.readFromNBT(extra);
        multipleMode = extra.getBoolean("preciseMultiple");
        nextSlot = extra.getInt("preciseNextSlot");
    }

    @Override
    public void writeToNBT(CompoundNBT extra) {
        super.writeToNBT(extra);
        craftingTracker.writeToNBT(extra);
        extra.putBoolean("preciseMultiple", multipleMode);
        extra.putInt("preciseNextSlot", nextSlot);
    }

    @Override
    protected int calculateItemsToSend() {
        return 8 * super.calculateItemsToSend();
    }

    @Override
    protected TickRateModulation doBusWork() {
        if (!getProxy().isActive() || !canDoBusWork()) {
            return TickRateModulation.IDLE;
        }

        InventoryAdaptor destination = getHandler();
        if (destination == null) {
            return TickRateModulation.SLEEP;
        }

        try {
            IItemStorageChannel channel = Api.instance().storage().getStorageChannel(IItemStorageChannel.class);
            IMEMonitor<IAEItemStack> network = getProxy().getStorage().getInventory(channel);
            IEnergyGrid energy = getProxy().getEnergy();
            ICraftingGrid crafting = getProxy().getCrafting();
            IItemHandler config = getInventoryByName("config");
            SchedulingMode scheduling = (SchedulingMode) getConfigManager().getSetting(Settings.SCHEDULING_MODE);
            int budget = calculateItemsToSend();
            int visited = 0;
            boolean worked = false;

            for (int x = 0; x < availableSlots() && budget > 0; x++) {
                int slot = startingSlot(scheduling, x);
                visited = x + 1;
                ItemStack configured = config.getStackInSlot(slot);
                if (configured.isEmpty()) {
                    continue;
                }

                int batch = Math.max(1, configured.getCount());
                boolean exact = useExactAmount();
                if (exact && budget < batch) {
                    continue;
                }

                IAEItemStack key = channel.createStack(configured);
                IAEItemStack stored = network.getStorageList().findPrecise(key);
                long available = stored == null ? 0 : stored.getStackSize();

                int target;
                if (exact) {
                    target = batch;
                } else {
                    long max = Math.min((long) budget, available);
                    target = (int) (max / batch * batch);
                }

                if (isCraftOnly()) {
                    int craftAmount = exact ? batch : Math.max(batch, budget / batch * batch);
                    worked |= requestCraft(slot, craftAmount, key, destination, crafting);
                    budget = Math.max(0, budget - Math.min(budget, craftAmount));
                    continue;
                }

                if (target <= 0 || available < (exact ? batch : target)) {
                    if (isCraftingEnabled()) {
                        int craftAmount = exact ? batch : Math.max(batch, budget / batch * batch);
                        worked |= requestCraft(slot, craftAmount, key, destination, crafting);
                    }
                    continue;
                }

                ItemStack probe = configured.copy();
                probe.setCount(Math.min(target, probe.getMaxStackSize()));
                ItemStack leftover = destination.simulateAdd(probe);
                int canFit = probe.getCount() - (leftover.isEmpty() ? 0 : leftover.getCount());
                canFit = canFit / batch * batch;

                if (exact && canFit < batch) {
                    continue;
                }
                if (canFit <= 0) {
                    continue;
                }

                IAEItemStack request = stored.copy();
                request.setStackSize(canFit);
                IAEItemStack extracted = Platform.poweredExtraction(energy, network, request, source);
                if (extracted == null || extracted.getStackSize() < canFit) {
                    if (extracted != null && extracted.getStackSize() > 0) {
                        network.injectItems(extracted, Actionable.MODULATE, source);
                    }
                    continue;
                }

                ItemStack failed = destination.addItems(extracted.createItemStack());
                int failedCount = failed.isEmpty() ? 0 : failed.getCount();
                if (failedCount > 0) {
                    IAEItemStack restore = extracted.copy();
                    restore.setStackSize(failedCount);
                    network.injectItems(restore, Actionable.MODULATE, source);
                }

                int moved = (int) extracted.getStackSize() - failedCount;
                int validMoved = moved / batch * batch;
                if (validMoved != moved) {
                    // Destination changed after simulation. Restore the partial tail
                    // so only complete batches are committed.
                    int tail = moved - validMoved;
                    if (tail > 0) {
                        ItemStack remove = configured.copy();
                        remove.setCount(tail);
                        ItemStack takenBack = destination.removeItems(tail, remove, null);
                        if (!takenBack.isEmpty()) {
                            network.injectItems(channel.createStack(takenBack), Actionable.MODULATE, source);
                        }
                    }
                    moved = validMoved;
                }

                if (moved > 0) {
                    budget -= moved;
                    worked = true;
                } else if (isCraftingEnabled()) {
                    worked |= requestCraft(slot, batch, key, destination, crafting);
                }
            }

            if (worked && scheduling == SchedulingMode.ROUNDROBIN && availableSlots() > 0) {
                nextSlot = (nextSlot + visited) % availableSlots();
            }

            return worked ? TickRateModulation.FASTER : TickRateModulation.SLOWER;
        } catch (GridAccessException e) {
            return TickRateModulation.IDLE;
        }
    }

    private boolean requestCraft(int slot, int amount, IAEItemStack key,
            InventoryAdaptor destination, ICraftingGrid crafting) {
        if (key == null || amount <= 0) {
            return false;
        }
        try {
            return craftingTracker.handleCrafting(slot, amount, key, destination,
                    getTile().getWorld(), getProxy().getGrid(), crafting, source);
        } catch (GridAccessException e) {
            return false;
        }
    }

    private boolean useExactAmount() {
        if (getInstalledUpgrades(Upgrades.REDSTONE) > 0 && getRSMode() == RedstoneMode.SIGNAL_PULSE) {
            return true;
        }
        return !multipleMode;
    }

    private boolean isCraftingEnabled() {
        return getInstalledUpgrades(Upgrades.CRAFTING) > 0;
    }

    private boolean isCraftOnly() {
        return isCraftingEnabled() && getConfigManager().getSetting(Settings.CRAFT_ONLY) == YesNo.YES;
    }

    private int startingSlot(SchedulingMode mode, int offset) {
        if (mode == SchedulingMode.RANDOM) {
            return Platform.getRandom().nextInt(availableSlots());
        }
        if (mode == SchedulingMode.ROUNDROBIN) {
            return (nextSlot + offset) % availableSlots();
        }
        return offset;
    }

    @Override
    public ImmutableSet<ICraftingLink> getRequestedJobs() {
        return craftingTracker.getRequestedJobs();
    }

    @Override
    public IAEItemStack injectCraftedItems(ICraftingLink link, IAEItemStack items, Actionable mode) {
        InventoryAdaptor destination = getHandler();
        try {
            if (destination != null && getProxy().isActive()) {
                IEnergyGrid energy = getProxy().getEnergy();
                double power = items.getStackSize();
                if (energy.extractAEPower(power, mode, PowerMultiplier.CONFIG) > power - 0.01) {
                    if (mode == Actionable.MODULATE) {
                        return AEItemStack.fromItemStack(destination.addItems(items.createItemStack()));
                    }
                    return AEItemStack.fromItemStack(destination.simulateAdd(items.createItemStack()));
                }
            }
        } catch (GridAccessException e) {
            AELog.debug(e);
        }
        return items;
    }

    @Override
    public void jobStateChange(ICraftingLink link) {
        craftingTracker.jobStateChange(link);
    }

    @Override
    public boolean onPartActivate(PlayerEntity player, Hand hand, Vector3d pos) {
        if (!isRemote()) {
            ContainerOpener.openContainer(PreciseExportBusContainer.TYPE, player, ContainerLocator.forPart(this));
        }
        return true;
    }
}
