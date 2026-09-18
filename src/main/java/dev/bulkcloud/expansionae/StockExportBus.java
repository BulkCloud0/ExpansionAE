package dev.bulkcloud.expansionae;

import java.util.Collection;

import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.config.SchedulingMode;
import appeng.api.config.Settings;
import appeng.api.config.Upgrades;
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
import appeng.util.inv.ItemSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Hand;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.items.IItemHandler;

/**
 * Backport of AdvancedAE's Stock Export Bus for item storage.
 *
 * <p>The configured stack count is the desired amount in the adjacent
 * inventory. Because AE2 8.4 represents configuration through ItemStack, the
 * practical target range in this version is 1..64 from the GUI.</p>
 */
public class StockExportBus extends ExportBusPart {
    private final IActionSource source = new MachineSource(this);
    private int stockNextSlot;

    public StockExportBus(ItemStack stack) {
        super(stack);
    }

    @Override
    protected int getUpgradeSlots() {
        return 6;
    }

    @Override
    public void readFromNBT(CompoundNBT extra) {
        super.readFromNBT(extra);
        stockNextSlot = extra.getInt("stockNextSlot");
    }

    @Override
    public void writeToNBT(CompoundNBT extra) {
        super.writeToNBT(extra);
        extra.putInt("stockNextSlot", stockNextSlot);
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
            FuzzyMode fuzzyMode = (FuzzyMode) getConfigManager().getSetting(Settings.FUZZY_MODE);
            SchedulingMode scheduling =
                    (SchedulingMode) getConfigManager().getSetting(Settings.SCHEDULING_MODE);
            boolean fuzzy = getInstalledUpgrades(Upgrades.FUZZY) > 0;
            int budget = calculateItemsToSend();
            int slotsVisited = 0;
            boolean worked = false;

            for (int x = 0; x < availableSlots() && budget > 0; x++) {
                int configSlot = startingSlot(scheduling, x);
                slotsVisited = x + 1;
                ItemStack wanted = config.getStackInSlot(configSlot);
                if (wanted.isEmpty()) continue;

                int targetAmount = Math.max(1, wanted.getCount());
                long current = currentStock(destination, wanted, fuzzy, fuzzyMode);
                long deficit = targetAmount - current;
                if (deficit <= 0) continue;

                IAEItemStack filter = channel.createStack(wanted);
                Collection<IAEItemStack> candidates = fuzzy
                        ? network.getStorageList().findFuzzy(filter, fuzzyMode)
                        : java.util.Collections.singletonList(network.getStorageList().findPrecise(filter));

                for (IAEItemStack candidate : candidates) {
                    if (candidate == null || candidate.getStackSize() <= 0 || budget <= 0 || deficit <= 0) continue;
                    int moved = transfer(destination, network, energy, candidate,
                            (int) Math.min(Math.min(deficit, budget), Integer.MAX_VALUE));
                    if (moved > 0) {
                        budget -= moved;
                        deficit -= moved;
                        worked = true;
                    }
                }
            }

            if (worked && scheduling == SchedulingMode.ROUNDROBIN && availableSlots() > 0) {
                stockNextSlot = (stockNextSlot + slotsVisited) % availableSlots();
            }

            return worked ? TickRateModulation.FASTER : TickRateModulation.SLOWER;
        } catch (GridAccessException e) {
            return TickRateModulation.IDLE;
        }
    }

    protected long currentStock(InventoryAdaptor destination, ItemStack wanted,
            boolean fuzzy, FuzzyMode fuzzyMode) {
        long count = 0;
        for (ItemSlot slot : destination) {
            ItemStack stack = slot.getItemStack();
            if (stack.isEmpty()) continue;
            boolean match = fuzzy
                    ? Platform.itemComparisons().isFuzzyEqualItem(stack, wanted, fuzzyMode)
                    : Platform.itemComparisons().isSameItem(stack, wanted);
            if (match) count += stack.getCount();
        }
        return count;
    }

    protected int transfer(InventoryAdaptor destination, IMEMonitor<IAEItemStack> network,
            IEnergyGrid energy, IAEItemStack candidate, int requested) {
        if (requested <= 0) return 0;

        ItemStack simulatedStack = candidate.createItemStack();
        simulatedStack.setCount(Math.min(requested, simulatedStack.getMaxStackSize()));
        ItemStack leftover = destination.simulateAdd(simulatedStack);
        int canFit = simulatedStack.getCount() - (leftover.isEmpty() ? 0 : leftover.getCount());
        if (canFit <= 0) return 0;

        IAEItemStack extraction = candidate.copy();
        extraction.setStackSize(canFit);
        IAEItemStack extracted = Platform.poweredExtraction(energy, network, extraction, source);
        if (extracted == null || extracted.getStackSize() <= 0) return 0;

        ItemStack failed = destination.addItems(extracted.createItemStack());
        int failedCount = failed.isEmpty() ? 0 : failed.getCount();
        if (failedCount > 0) {
            IAEItemStack returnStack = candidate.copy();
            returnStack.setStackSize(failedCount);
            network.injectItems(returnStack, Actionable.MODULATE, source);
        }
        return (int) extracted.getStackSize() - failedCount;
    }

    private int startingSlot(SchedulingMode mode, int offset) {
        if (mode == SchedulingMode.RANDOM) {
            return Platform.getRandom().nextInt(availableSlots());
        }
        if (mode == SchedulingMode.ROUNDROBIN) {
            return (stockNextSlot + offset) % availableSlots();
        }
        return offset;
    }

    @Override
    public boolean onPartActivate(PlayerEntity player, Hand hand, Vector3d pos) {
        if (!isRemote()) {
            ContainerOpener.openContainer(StockExportBusContainer.TYPE, player, ContainerLocator.forPart(this));
        }
        return true;
    }
}
