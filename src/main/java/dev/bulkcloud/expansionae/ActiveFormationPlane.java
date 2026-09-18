package dev.bulkcloud.expansionae;

import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.config.RedstoneMode;
import appeng.api.config.Settings;
import appeng.api.config.Upgrades;
import appeng.api.config.YesNo;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.core.Api;
import appeng.core.settings.TickRates;
import appeng.me.GridAccessException;
import appeng.me.helpers.MachineSource;
import appeng.parts.automation.FormationPlanePart;
import appeng.util.Platform;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

/**
 * ExtendedAE-style active formation plane implemented on top of AE2 8.4's
 * native FormationPlanePart. Placement and entity-spawn semantics are delegated
 * to AE2; this class only actively extracts matching items from network storage.
 */
public final class ActiveFormationPlane extends FormationPlanePart implements IGridTickable {
    private final IActionSource source;

    public ActiveFormationPlane(ItemStack stack) {
        super(stack);
        this.source = new MachineSource(this);
        this.getConfigManager().registerSetting(Settings.REDSTONE_CONTROLLED, RedstoneMode.IGNORE);
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(TickRates.ExportBus.getMin(), TickRates.ExportBus.getMax(), this.isSleeping(), false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        if (this.isSleeping()) {
            return TickRateModulation.SLEEP;
        }
        if (!this.getProxy().isActive()) {
            return TickRateModulation.IDLE;
        }

        try {
            IItemStorageChannel channel = Api.instance().storage().getStorageChannel(IItemStorageChannel.class);
            IMEMonitor<IAEItemStack> network = this.getProxy().getStorage().getInventory(channel);
            IAEItemStack candidate = findCandidate(network, channel);
            if (candidate == null || candidate.getStackSize() <= 0) {
                return TickRateModulation.SLOWER;
            }

            int budget = getTransferBudget();
            IAEItemStack request = candidate.copy();
            request.setStackSize(Math.min(candidate.getStackSize(), budget));

            IAEItemStack simulatedRemainder = this.injectItems(request, Actionable.SIMULATE, this.source);
            long accepted = request.getStackSize()
                    - (simulatedRemainder == null ? 0 : simulatedRemainder.getStackSize());
            if (accepted <= 0) {
                return TickRateModulation.SLOWER;
            }

            IAEItemStack extraction = candidate.copy();
            extraction.setStackSize(accepted);
            IAEItemStack extracted = Platform.poweredExtraction(
                    this.getProxy().getEnergy(), network, extraction, this.source);
            if (extracted == null || extracted.getStackSize() <= 0) {
                return TickRateModulation.SLOWER;
            }

            IAEItemStack remainder = this.injectItems(extracted, Actionable.MODULATE, this.source);
            long placed = extracted.getStackSize() - (remainder == null ? 0 : remainder.getStackSize());

            if (remainder != null && remainder.getStackSize() > 0) {
                network.injectItems(remainder, Actionable.MODULATE, this.source);
            }

            return placed > 0 ? TickRateModulation.FASTER : TickRateModulation.SLOWER;
        } catch (GridAccessException e) {
            return TickRateModulation.IDLE;
        }
    }

    private IAEItemStack findCandidate(IMEMonitor<IAEItemStack> network, IItemStorageChannel channel) {
        IItemHandler config = this.getInventoryByName("config");
        boolean inverted = this.getInstalledUpgrades(Upgrades.INVERTER) > 0;
        boolean fuzzy = this.getInstalledUpgrades(Upgrades.FUZZY) > 0;
        FuzzyMode fuzzyMode = (FuzzyMode) this.getConfigManager().getSetting(Settings.FUZZY_MODE);

        if (!inverted) {
            for (int i = 0; i < availableConfigSlots(); i++) {
                ItemStack filter = config.getStackInSlot(i);
                if (filter.isEmpty()) {
                    continue;
                }

                IAEItemStack filterAe = channel.createStack(filter);
                if (fuzzy) {
                    for (IAEItemStack match : network.getStorageList().findFuzzy(filterAe, fuzzyMode)) {
                        if (match != null && match.getStackSize() > 0) {
                            return match;
                        }
                    }
                } else {
                    IAEItemStack match = network.getStorageList().findPrecise(filterAe);
                    if (match != null && match.getStackSize() > 0) {
                        return match;
                    }
                }
            }
            return null;
        }

        for (IAEItemStack stored : network.getStorageList()) {
            if (stored != null && stored.getStackSize() > 0
                    && !matchesConfigured(stored.createItemStack(), config, fuzzy, fuzzyMode)) {
                return stored;
            }
        }
        return null;
    }

    private boolean matchesConfigured(ItemStack stack, IItemHandler config, boolean fuzzy, FuzzyMode fuzzyMode) {
        for (int i = 0; i < availableConfigSlots(); i++) {
            ItemStack filter = config.getStackInSlot(i);
            if (filter.isEmpty()) {
                continue;
            }
            if (fuzzy
                    ? Platform.itemComparisons().isFuzzyEqualItem(stack, filter, fuzzyMode)
                    : Platform.itemComparisons().isSameItem(stack, filter)) {
                return true;
            }
        }
        return false;
    }

    private int availableConfigSlots() {
        return Math.min(18 + this.getInstalledUpgrades(Upgrades.CAPACITY) * 9,
                this.getInventoryByName("config").getSlots());
    }

    private int getTransferBudget() {
        if (this.getConfigManager().getSetting(Settings.PLACE_BLOCK) == YesNo.YES) {
            return 1;
        }
        switch (this.getInstalledUpgrades(Upgrades.SPEED)) {
            case 1:
                return 8;
            case 2:
                return 32;
            case 3:
                return 64;
            case 4:
                return 96;
            default:
                return 1;
        }
    }

    @Override
    public RedstoneMode getRSMode() {
        return (RedstoneMode) this.getConfigManager().getSetting(Settings.REDSTONE_CONTROLLED);
    }
}
