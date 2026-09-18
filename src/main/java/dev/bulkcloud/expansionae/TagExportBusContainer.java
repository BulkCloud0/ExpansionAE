package dev.bulkcloud.expansionae;

import appeng.api.config.SecurityPermissions;
import appeng.container.guisync.GuiSync;
import appeng.container.implementations.ContainerTypeBuilder;
import appeng.container.implementations.UpgradeableContainer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.ContainerType;

public final class TagExportBusContainer extends UpgradeableContainer implements TextFilterReceiver {
    public static final ContainerType<TagExportBusContainer> TYPE = ContainerTypeBuilder
            .create(TagExportBusContainer::new, TagExportBus.class)
            .requirePermission(SecurityPermissions.BUILD)
            .build("expansionae_tag_export_bus");

    private final TagExportBus host;

    @GuiSync(11)
    public String whitelist = "";

    @GuiSync(12)
    public String blacklist = "";

    public TagExportBusContainer(int id, PlayerInventory player, TagExportBus host) {
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
        if (key == 0) {
            host.setTagFilter(true, value);
        } else if (key == 1) {
            host.setTagFilter(false, value);
        }
    }
}
