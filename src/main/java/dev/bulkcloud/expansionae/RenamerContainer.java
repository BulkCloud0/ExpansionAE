package dev.bulkcloud.expansionae;

import appeng.api.parts.PartItemStack;
import appeng.container.AEBaseContainer;
import appeng.container.implementations.ContainerTypeBuilder;
import appeng.helpers.ICustomNameObject;
import appeng.parts.AEBasePart;
import appeng.tile.AEBaseTileEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.StringTextComponent;

/**
 * ExtendedAE-style renamer for AE2 block entities and cable-bus parts.
 */
public final class RenamerContainer extends AEBaseContainer {
    public static final ContainerType<RenamerContainer> TYPE = ContainerTypeBuilder
            .create(RenamerContainer::new, ICustomNameObject.class)
            .build("expansionae_renamer");

    private final ICustomNameObject host;

    public RenamerContainer(int id, PlayerInventory inventory, ICustomNameObject host) {
        super(TYPE, id, inventory, host);
        this.host = host;
    }

    public String getCurrentName() {
        return host != null && host.hasCustomInventoryName()
                ? host.getCustomInventoryName().getString()
                : "";
    }

    public void setNameServer(String name) {
        if (isRemote() || host == null) return;

        String normalized = name == null ? "" : name.trim();
        if (normalized.length() > 64) normalized = normalized.substring(0, 64);

        if (host instanceof AEBaseTileEntity) {
            AEBaseTileEntity tile = (AEBaseTileEntity) host;
            tile.setName(normalized);
            tile.markDirty();
            return;
        }

        if (host instanceof AEBasePart) {
            AEBasePart part = (AEBasePart) host;
            ItemStack stack = part.getItemStack(PartItemStack.WORLD);
            if (normalized.isEmpty()) stack.clearCustomName();
            else stack.setDisplayName(new StringTextComponent(normalized));
            part.saveChanges();
        }
    }
}
