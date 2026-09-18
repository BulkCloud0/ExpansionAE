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
import appeng.core.Api;
import appeng.me.GridAccessException;
import appeng.me.helpers.MachineSource;
import appeng.parts.automation.ExportBusPart;
import appeng.util.InventoryAdaptor;
import appeng.util.Platform;
import appeng.util.inv.IInventoryDestination;
import appeng.util.inv.ItemSlot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

/**
 * AdvancedAE-style combined item import/export bus for AE2 8.4.
 *
 * <p>The export side uses AE2's native ExportBusPart implementation. Importing
 * gets its own transfer budget and accepts items that are not configured for
 * export. An inverter card reverses the import filter and imports only configured
 * items.</p>
 */
public final class ImportExportBus extends ExportBusPart implements IInventoryDestination {
    private final IActionSource source;

    public ImportExportBus(ItemStack stack) {
        super(stack);
        this.source = new MachineSource(this);
    }

    @Override
    protected TickRateModulation doBusWork() {
        TickRateModulation exportResult = super.doBusWork();

        if (!this.getProxy().isActive() || !this.canDoBusWork()) {
            return exportResult;
        }

        InventoryAdaptor external = this.getHandler();
        if (external == null || !external.containsItems()) {
            return exportResult;
        }

        boolean imported = false;

        try {
            IItemStorageChannel channel = Api.instance().storage().getStorageChannel(IItemStorageChannel.class);
            IMEMonitor<IAEItemStack> network = this.getProxy().getStorage().getInventory(channel);
            IEnergyGrid energy = this.getProxy().getEnergy();
            IItemHandler config = this.getInventoryByName("config");
            FuzzyMode fuzzyMode = (FuzzyMode) this.getConfigManager().getSetting(Settings.FUZZY_MODE);
            boolean fuzzy = this.getInstalledUpgrades(Upgrades.FUZZY) > 0;
            boolean inverted = this.getInstalledUpgrades(Upgrades.INVERTER) > 0;
            int remaining = this.calculateItemsToSend();

            for (ItemSlot slot : external) {
                if (remaining <= 0) {
                    break;
                }

                ItemStack candidate = slot.getItemStack();
                if (candidate.isEmpty() || !slot.isExtractable()
                        || !shouldImport(candidate, config, fuzzy, fuzzyMode, inverted)) {
                    continue;
                }

                int request = Math.min(remaining, candidate.getMaxStackSize());
                ItemStack simulated = external.simulateRemove(request, candidate, this);
                if (simulated.isEmpty()) {
                    continue;
                }

                IAEItemStack simulatedAe = channel.createStack(simulated);
                IAEItemStack notStorable = network.injectItems(simulatedAe, Actionable.SIMULATE, this.source);
                int storable = simulated.getCount();
                if (notStorable != null) {
                    storable -= (int) Math.min(Integer.MAX_VALUE, notStorable.getStackSize());
                }
                if (storable <= 0) {
                    continue;
                }

                ItemStack extracted = external.removeItems(storable, candidate, this);
                if (extracted.isEmpty()) {
                    continue;
                }

                IAEItemStack extractedAe = channel.createStack(extracted);
                IAEItemStack failed = Platform.poweredInsert(energy, network, extractedAe, this.source);
                int failedCount = failed == null ? 0 : (int) Math.min(Integer.MAX_VALUE, failed.getStackSize());
                int inserted = extracted.getCount() - failedCount;

                if (failed != null && failedCount > 0) {
                    ItemStack couldNotReturn = external.addItems(failed.createItemStack());
                    if (!couldNotReturn.isEmpty()) {
                        // Last-resort loss prevention if the target inventory changed mid-transfer.
                        IAEItemStack spill = channel.createStack(couldNotReturn);
                        network.injectItems(spill, Actionable.MODULATE, this.source);
                    }
                }

                if (inserted > 0) {
                    remaining -= inserted;
                    imported = true;
                }
            }
        } catch (GridAccessException ignored) {
            // Network went away between the export and import phases.
        }

        return imported ? TickRateModulation.FASTER : exportResult;
    }

    private boolean shouldImport(ItemStack stack, IItemHandler config, boolean fuzzy, FuzzyMode fuzzyMode,
            boolean inverted) {
        boolean configured = false;

        if (config != null) {
            for (int i = 0; i < this.availableSlots(); i++) {
                ItemStack filter = config.getStackInSlot(i);
                if (filter.isEmpty()) {
                    continue;
                }

                if (fuzzy
                        ? Platform.itemComparisons().isFuzzyEqualItem(stack, filter, fuzzyMode)
                        : Platform.itemComparisons().isSameItem(stack, filter)) {
                    configured = true;
                    break;
                }
            }
        }

        return inverted ? configured : !configured;
    }

    @Override
    public boolean canInsert(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        try {
            IItemStorageChannel channel = Api.instance().storage().getStorageChannel(IItemStorageChannel.class);
            IMEMonitor<IAEItemStack> network = this.getProxy().getStorage().getInventory(channel);
            IAEItemStack remainder = network.injectItems(channel.createStack(stack), Actionable.SIMULATE, this.source);
            return remainder == null || remainder.getStackSize() < stack.getCount();
        } catch (GridAccessException e) {
            return false;
        }
    }

    @Override
    protected int calculateItemsToSend() {
        // AdvancedAE counts import and export separately. Keep the same native
        // per-phase budget here; speed cards affect both phases independently.
        return super.calculateItemsToSend();
    }
}
