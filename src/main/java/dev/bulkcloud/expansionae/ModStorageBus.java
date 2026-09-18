package dev.bulkcloud.expansionae;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import appeng.api.storage.data.IAEItemStack;
import appeng.container.ContainerLocator;
import appeng.container.ContainerOpener;
import appeng.util.Platform;
import appeng.util.prioritylist.IPartitionList;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;

/** ExtendedAE-style storage bus partitioned by comma-separated mod ids or display names. */
public final class ModStorageBus extends FilteredStorageBus {
    private String modFilter = "";

    public ModStorageBus(ItemStack stack) {
        super(stack);
    }

    @Override
    protected int getUpgradeSlots() {
        return 2;
    }

    public String getModFilter() {
        return modFilter;
    }

    public void setModFilter(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.length() > 128) normalized = normalized.substring(0, 128);
        if (!normalized.equals(modFilter)) {
            modFilter = normalized;
            getHost().markForSave();
            forceFilterUpdate();
        }
    }

    @Override
    public void readFromNBT(CompoundNBT data) {
        super.readFromNBT(data);
        modFilter = data.getString("modFilter");
    }

    @Override
    public void writeToNBT(CompoundNBT data) {
        super.writeToNBT(data);
        data.putString("modFilter", modFilter);
    }

    @Override
    protected IPartitionList<IAEItemStack> createPartitionList() {
        final Set<String> accepted = new HashSet<>();
        for (String token : modFilter.split(",")) {
            String value = token.trim().toLowerCase(Locale.ROOT);
            if (!value.isEmpty()) accepted.add(value);
        }

        return new IPartitionList<IAEItemStack>() {
            @Override
            public boolean isListed(IAEItemStack input) {
                if (input == null || accepted.isEmpty()) return false;
                ItemStack stack = input.createItemStack();
                ResourceLocation id = stack.getItem().getRegistryName();
                if (id == null) return false;
                String namespace = id.getNamespace().toLowerCase(Locale.ROOT);
                if (accepted.contains(namespace)) return true;
                String display = Platform.getModName(namespace);
                return display != null && accepted.contains(display.toLowerCase(Locale.ROOT));
            }

            @Override
            public boolean isEmpty() {
                return accepted.isEmpty();
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
            ContainerOpener.openContainer(ModStorageBusContainer.TYPE, player, ContainerLocator.forPart(this));
        }
        return true;
    }
}
