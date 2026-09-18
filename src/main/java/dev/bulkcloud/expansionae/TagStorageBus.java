package dev.bulkcloud.expansionae;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import appeng.api.storage.data.IAEItemStack;
import appeng.container.ContainerLocator;
import appeng.container.ContainerOpener;
import appeng.util.prioritylist.IPartitionList;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;

/** ExtendedAE-style tag-expression storage bus. */
public final class TagStorageBus extends FilteredStorageBus {
    private String whitelist = "";
    private String blacklist = "";
    private transient Predicate<Set<String>> whitePredicate = TagExpression.compile("");
    private transient Predicate<Set<String>> blackPredicate = TagExpression.compile("");

    public TagStorageBus(ItemStack stack) {
        super(stack);
    }

    @Override
    protected int getUpgradeSlots() {
        return 2;
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
                forceFilterUpdate();
            }
        } else if (!normalized.equals(blacklist)) {
            blacklist = normalized;
            blackPredicate = TagExpression.compile(blacklist);
            getHost().markForSave();
            forceFilterUpdate();
        }
    }

    @Override
    public void readFromNBT(CompoundNBT data) {
        super.readFromNBT(data);
        whitelist = data.getString("tagWhitelist");
        blacklist = data.getString("tagBlacklist");
        whitePredicate = TagExpression.compile(whitelist);
        blackPredicate = TagExpression.compile(blacklist);
    }

    @Override
    public void writeToNBT(CompoundNBT data) {
        super.writeToNBT(data);
        data.putString("tagWhitelist", whitelist);
        data.putString("tagBlacklist", blacklist);
    }

    @Override
    protected IPartitionList<IAEItemStack> createPartitionList() {
        final boolean whitelistActive = whitelist != null && !whitelist.trim().isEmpty();
        final Predicate<Set<String>> white = whitePredicate;
        final Predicate<Set<String>> black = blackPredicate;

        return new IPartitionList<IAEItemStack>() {
            @Override
            public boolean isListed(IAEItemStack input) {
                if (input == null) return false;
                ItemStack stack = input.createItemStack();
                Set<String> tags = new HashSet<>();
                ResourceLocation id = stack.getItem().getRegistryName();
                if (id != null) tags.add(id.toString());
                for (ResourceLocation tag : ItemTags.getCollection().getOwningTags(stack.getItem())) {
                    tags.add(tag.toString());
                }
                boolean whiteMatch = white.test(tags);
                boolean blackMatch = black.test(tags);
                return (!whitelistActive || whiteMatch) && !blackMatch;
            }

            @Override
            public boolean isEmpty() {
                return !whitelistActive && (blacklist == null || blacklist.trim().isEmpty());
            }

            @Override
            public Iterable<IAEItemStack> getItems() {
                return Collections.emptyList();
            }
        };
    }

    @Override
    public boolean onPartActivate(PlayerEntity player, Hand hand, Vector3d pos) {
        if (!isRemote()) {
            ContainerOpener.openContainer(TagStorageBusContainer.TYPE, player, ContainerLocator.forPart(this));
        }
        return true;
    }
}
