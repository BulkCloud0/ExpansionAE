package dev.bulkcloud.expansionae;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import appeng.api.config.Actionable;
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
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;

/**
 * ExtendedAE-style tag export bus with whitelist/blacklist expressions.
 *
 * Supports !, &, ^, |, parentheses and * wildcards. Empty whitelist means no
 * whitelist restriction; blacklist matches are always rejected.
 */
public final class TagExportBus extends ExportBusPart {
    private final IActionSource source = new MachineSource(this);
    private String whitelist = "";
    private String blacklist = "";
    private transient Predicate<Set<String>> whitePredicate = TagExpression.compile("");
    private transient Predicate<Set<String>> blackPredicate = TagExpression.compile("");

    public TagExportBus(ItemStack stack) {
        super(stack);
    }

    public String getWhitelist() {
        return whitelist;
    }

    public String getBlacklist() {
        return blacklist;
    }

    public void setTagFilter(boolean white, String value) {
        String normalized = value == null ? "" : value;
        if (normalized.length() > 128) normalized = normalized.substring(0, 128);
        if (white) {
            if (!normalized.equals(whitelist)) {
                whitelist = normalized;
                whitePredicate = TagExpression.compile(whitelist);
                getHost().markForSave();
            }
        } else if (!normalized.equals(blacklist)) {
            blacklist = normalized;
            blackPredicate = TagExpression.compile(blacklist);
            getHost().markForSave();
        }
    }

    @Override
    public void readFromNBT(CompoundNBT extra) {
        super.readFromNBT(extra);
        whitelist = extra.getString("tagWhitelist");
        blacklist = extra.getString("tagBlacklist");
        whitePredicate = TagExpression.compile(whitelist);
        blackPredicate = TagExpression.compile(blacklist);
    }

    @Override
    public void writeToNBT(CompoundNBT extra) {
        super.writeToNBT(extra);
        extra.putString("tagWhitelist", whitelist);
        extra.putString("tagBlacklist", blacklist);
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
            int budget = calculateItemsToSend();
            boolean worked = false;

            for (IAEItemStack candidate : network.getStorageList()) {
                if (budget <= 0) break;
                if (candidate == null || candidate.getStackSize() <= 0 || !matches(candidate)) continue;

                ItemStack probe = candidate.createItemStack();
                int count = (int) Math.min(Math.min((long) budget, candidate.getStackSize()), probe.getMaxStackSize());
                if (count <= 0) continue;
                probe.setCount(count);

                ItemStack leftover = destination.simulateAdd(probe);
                int canFit = probe.getCount() - (leftover.isEmpty() ? 0 : leftover.getCount());
                if (canFit <= 0) continue;

                IAEItemStack request = candidate.copy();
                request.setStackSize(canFit);
                IAEItemStack extracted = Platform.poweredExtraction(energy, network, request, source);
                if (extracted == null || extracted.getStackSize() <= 0) continue;

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

            return worked ? TickRateModulation.FASTER : TickRateModulation.SLOWER;
        } catch (GridAccessException e) {
            return TickRateModulation.IDLE;
        }
    }

    private boolean matches(IAEItemStack candidate) {
        ItemStack stack = candidate.createItemStack();
        Set<String> tags = new HashSet<>();
        ResourceLocation id = stack.getItem().getRegistryName();
        if (id != null) tags.add(id.toString());
        for (ResourceLocation tag : ItemTags.getCollection().getOwningTags(stack.getItem())) {
            tags.add(tag.toString());
        }

        boolean whitelistActive = whitelist != null && !whitelist.trim().isEmpty();
        boolean whiteMatches = whitePredicate.test(tags);
        boolean blackMatches = blackPredicate.test(tags);
        return (!whitelistActive || whiteMatches) && !blackMatches;
    }

    @Override
    public boolean onPartActivate(PlayerEntity player, Hand hand, Vector3d pos) {
        if (!isRemote()) {
            ContainerOpener.openContainer(TagExportBusContainer.TYPE, player, ContainerLocator.forPart(this));
        }
        return true;
    }
}
