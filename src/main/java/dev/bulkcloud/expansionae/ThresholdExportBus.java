package dev.bulkcloud.expansionae;

import appeng.api.config.Actionable;
import appeng.api.config.SchedulingMode;
import appeng.api.config.Settings;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.container.ContainerLocator;
import appeng.container.ContainerOpener;
import appeng.core.Api;
import appeng.me.GridAccessException;
import appeng.me.helpers.MachineSource;
import appeng.parts.automation.ExportBusPart;
import appeng.util.InventoryAdaptor;
import appeng.util.Platform;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Hand;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.items.IItemHandler;

/**
 * ExtendedAE-style threshold export bus.
 *
 * Each configured stack count is a network threshold. In GREATER mode the item
 * is exported while the ME network stores at least that amount; in LOWER mode
 * it is exported while the network stores at most that amount.
 */
public final class ThresholdExportBus extends ExportBusPart {
    private final IActionSource source = new MachineSource(this);
    private boolean lowerMode;
    private int nextSlot;

    public ThresholdExportBus(ItemStack stack) {
        super(stack);
    }

    public boolean isLowerMode() {
        return lowerMode;
    }

    public void setLowerMode(boolean lowerMode) {
        if (this.lowerMode != lowerMode) {
            this.lowerMode = lowerMode;
            getHost().markForSave();
        }
    }

    @Override
    public void readFromNBT(CompoundNBT extra) {
        super.readFromNBT(extra);
        lowerMode = extra.getBoolean("thresholdLower");
        nextSlot = extra.getInt("thresholdNextSlot");
    }

    @Override
    public void writeToNBT(CompoundNBT extra) {
        super.writeToNBT(extra);
        extra.putBoolean("thresholdLower", lowerMode);
        extra.putInt("thresholdNextSlot", nextSlot);
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

                IAEItemStack key = channel.createStack(configured);
                IAEItemStack storedStack = network.getStorageList().findPrecise(key);
                long stored = storedStack == null ? 0 : storedStack.getStackSize();
                long threshold = Math.max(1, configured.getCount());
                boolean allowed = lowerMode ? stored <= threshold : stored >= threshold;
                if (!allowed || storedStack == null || stored <= 0) {
                    continue;
                }

                int requested = (int) Math.min(Math.min((long) budget, stored), Integer.MAX_VALUE);
                ItemStack simulated = storedStack.createItemStack();
                simulated.setCount(Math.min(requested, simulated.getMaxStackSize()));
                ItemStack leftover = destination.simulateAdd(simulated);
                int canFit = simulated.getCount() - (leftover.isEmpty() ? 0 : leftover.getCount());
                if (canFit <= 0) {
                    continue;
                }

                IAEItemStack request = storedStack.copy();
                request.setStackSize(canFit);
                IAEItemStack extracted = Platform.poweredExtraction(energy, network, request, source);
                if (extracted == null || extracted.getStackSize() <= 0) {
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
                if (moved > 0) {
                    budget -= moved;
                    worked = true;
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
    public boolean onPartActivate(PlayerEntity player, Hand hand, Vector3d pos) {
        if (!isRemote()) {
            ContainerOpener.openContainer(ThresholdExportBusContainer.TYPE, player, ContainerLocator.forPart(this));
        }
        return true;
    }
}
