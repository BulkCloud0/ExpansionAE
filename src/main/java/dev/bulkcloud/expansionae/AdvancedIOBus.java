package dev.bulkcloud.expansionae;

import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
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
import appeng.util.InventoryAdaptor;
import appeng.util.Platform;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Hand;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.items.IItemHandler;

/**
 * AdvancedAE-style Advanced IO Bus for AE2 8.4.
 *
 * <p>Export uses the stock-limited behavior inherited from StockExportBus. The
 * import phase first pulls excess configured stock back into the network when
 * regulation is enabled, then imports according to the export filter (blacklist
 * by default, whitelist with inverter card). Import and export have independent
 * transfer budgets.</p>
 */
public final class AdvancedIOBus extends StockExportBus {
    private final IActionSource importSource = new MachineSource(this);
    private boolean regulateStock = true;

    public AdvancedIOBus(ItemStack stack) {
        super(stack);
    }

    @Override
    protected int getUpgradeSlots() {
        return 8;
    }

    public boolean isRegulateStock() {
        return regulateStock;
    }

    public void setRegulateStock(boolean regulateStock) {
        if (this.regulateStock != regulateStock) {
            this.regulateStock = regulateStock;
            getHost().markForSave();
        }
    }

    @Override
    public void readFromNBT(CompoundNBT extra) {
        super.readFromNBT(extra);
        regulateStock = !extra.contains("regulateStock") || extra.getBoolean("regulateStock");
    }

    @Override
    public void writeToNBT(CompoundNBT extra) {
        super.writeToNBT(extra);
        extra.putBoolean("regulateStock", regulateStock);
    }

    @Override
    protected int calculateItemsToSend() {
        return 8 * super.calculateItemsToSend();
    }

    @Override
    protected TickRateModulation doBusWork() {
        TickRateModulation exportResult = super.doBusWork();

        if (!getProxy().isActive() || !canDoBusWork()) {
            return exportResult;
        }

        InventoryAdaptor external = getHandler();
        if (external == null || !external.containsItems()) {
            return exportResult;
        }

        try {
            IItemStorageChannel channel = Api.instance().storage().getStorageChannel(IItemStorageChannel.class);
            IMEMonitor<IAEItemStack> network = getProxy().getStorage().getInventory(channel);
            IEnergyGrid energy = getProxy().getEnergy();
            IItemHandler config = getInventoryByName("config");
            FuzzyMode fuzzyMode = (FuzzyMode) getConfigManager().getSetting(Settings.FUZZY_MODE);
            boolean fuzzy = getInstalledUpgrades(Upgrades.FUZZY) > 0;
            boolean inverted = getInstalledUpgrades(Upgrades.INVERTER) > 0;
            int budget = calculateItemsToSend();
            boolean imported = false;

            if (regulateStock) {
                for (int slot = 0; slot < availableSlots() && budget > 0; slot++) {
                    ItemStack wanted = config.getStackInSlot(slot);
                    if (wanted.isEmpty()) continue;

                    long current = currentStock(external, wanted, fuzzy, fuzzyMode);
                    long excess = current - Math.max(1, wanted.getCount());
                    if (excess <= 0) continue;

                    int moved = importMatching(external, network, energy, wanted,
                            (int) Math.min(Math.min(excess, budget), Integer.MAX_VALUE), fuzzy, fuzzyMode);
                    if (moved > 0) {
                        budget -= moved;
                        imported = true;
                    }
                }
            }

            if (budget > 0) {
                int moved = importFiltered(external, network, energy, config, budget, fuzzy, fuzzyMode, inverted);
                imported |= moved > 0;
            }

            return imported ? TickRateModulation.FASTER : exportResult;
        } catch (GridAccessException e) {
            return exportResult;
        }
    }

    private int importMatching(InventoryAdaptor external, IMEMonitor<IAEItemStack> network,
            IEnergyGrid energy, ItemStack wanted, int amount, boolean fuzzy, FuzzyMode fuzzyMode) {
        ItemStack simulated = fuzzy
                ? external.simulateSimilarRemove(amount, wanted, fuzzyMode, null)
                : external.simulateRemove(amount, wanted, null);
        if (simulated.isEmpty()) return 0;

        int accepted = networkCapacity(network, simulated);
        if (accepted <= 0) return 0;

        ItemStack extracted = fuzzy
                ? external.removeSimilarItems(Math.min(amount, accepted), wanted, fuzzyMode, null)
                : external.removeItems(Math.min(amount, accepted), wanted, null);
        return insertIntoNetwork(external, network, energy, extracted);
    }

    private int importFiltered(InventoryAdaptor external, IMEMonitor<IAEItemStack> network,
            IEnergyGrid energy, IItemHandler config, int budget, boolean fuzzy,
            FuzzyMode fuzzyMode, boolean inverted) {
        int moved = 0;

        for (appeng.util.inv.ItemSlot slot : external) {
            if (moved >= budget) break;
            ItemStack candidate = slot.getItemStack();
            if (candidate.isEmpty() || !slot.isExtractable()) continue;

            boolean configured = matchesConfig(candidate, config, fuzzy, fuzzyMode);
            boolean shouldImport = inverted ? configured : !configured;
            if (!shouldImport) continue;

            int request = Math.min(budget - moved, candidate.getMaxStackSize());
            ItemStack simulated = external.simulateRemove(request, candidate, null);
            if (simulated.isEmpty()) continue;

            int accepted = networkCapacity(network, simulated);
            if (accepted <= 0) continue;

            ItemStack extracted = external.removeItems(Math.min(request, accepted), candidate, null);
            moved += insertIntoNetwork(external, network, energy, extracted);
        }

        return moved;
    }

    private boolean matchesConfig(ItemStack stack, IItemHandler config, boolean fuzzy, FuzzyMode fuzzyMode) {
        for (int i = 0; i < availableSlots(); i++) {
            ItemStack filter = config.getStackInSlot(i);
            if (filter.isEmpty()) continue;
            boolean match = fuzzy
                    ? Platform.itemComparisons().isFuzzyEqualItem(stack, filter, fuzzyMode)
                    : Platform.itemComparisons().isSameItem(stack, filter);
            if (match) return true;
        }
        return false;
    }

    private int networkCapacity(IMEMonitor<IAEItemStack> network, ItemStack stack) {
        IItemStorageChannel channel = Api.instance().storage().getStorageChannel(IItemStorageChannel.class);
        IAEItemStack ae = channel.createStack(stack);
        IAEItemStack remainder = network.injectItems(ae, Actionable.SIMULATE, importSource);
        return stack.getCount() - (remainder == null ? 0 : (int) Math.min(stack.getCount(), remainder.getStackSize()));
    }

    private int insertIntoNetwork(InventoryAdaptor external, IMEMonitor<IAEItemStack> network,
            IEnergyGrid energy, ItemStack extracted) {
        if (extracted.isEmpty()) return 0;

        IItemStorageChannel channel = Api.instance().storage().getStorageChannel(IItemStorageChannel.class);
        IAEItemStack ae = channel.createStack(extracted);
        IAEItemStack failed = Platform.poweredInsert(energy, network, ae, importSource);
        int failedCount = failed == null ? 0 : (int) Math.min(Integer.MAX_VALUE, failed.getStackSize());

        if (failed != null && failedCount > 0) {
            ItemStack couldNotReturn = external.addItems(failed.createItemStack());
            if (!couldNotReturn.isEmpty()) {
                network.injectItems(channel.createStack(couldNotReturn), Actionable.MODULATE, importSource);
            }
        }
        return extracted.getCount() - failedCount;
    }

    @Override
    public boolean onPartActivate(PlayerEntity player, Hand hand, Vector3d pos) {
        if (!isRemote()) {
            ContainerOpener.openContainer(AdvancedIOBusContainer.TYPE, player, ContainerLocator.forPart(this));
        }
        return true;
    }
}
