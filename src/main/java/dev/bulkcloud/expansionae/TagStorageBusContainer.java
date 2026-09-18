package dev.bulkcloud.expansionae;

import appeng.api.config.SecurityPermissions;
import appeng.container.guisync.GuiSync;
import appeng.container.implementations.ContainerTypeBuilder;
import appeng.container.implementations.UpgradeableContainer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.ContainerType;

public final class TagStorageBusContainer extends UpgradeableContainer implements TextFilterReceiver {
    public static final ContainerType<TagStorageBusContainer> TYPE = ContainerTypeBuilder
            .create(TagStorageBusContainer::new, TagStorageBus.class)
            .requirePermission(SecurityPermissions.BUILD)
            .build("expansionae_tag_storage_bus");

    private final TagStorageBus host;

    @GuiSync(14)
    public String whitelist = "";

    @GuiSync(15)
    public String blacklist = "";

    public TagStorageBusContainer(int id, PlayerInventory player, TagStorageBus host) {
        super(TYPE, id, player, host);
        this.host = host;
    }

    @Override
    protected void setupConfig() {
        setupUpgrades();
    }

    @Override
    public void detectAndSendChanges() {
        if (isServer()) {
            whitelist = host.getWhitelist();
            blacklist = host.getBlacklist();
        }
        super.detectAndSendChanges();
    }

    @Override
    public void applyTextFilter(int key, String value) {
        if (key == 0) host.setTagFilter(true, value);
        if (key == 1) host.setTagFilter(false, value);
    }
}
