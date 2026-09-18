package dev.bulkcloud.expansionae;

import appeng.api.config.SecurityPermissions;
import appeng.container.guisync.GuiSync;
import appeng.container.implementations.ContainerTypeBuilder;
import appeng.container.implementations.UpgradeableContainer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.ContainerType;

public final class ModStorageBusContainer extends UpgradeableContainer implements TextFilterReceiver {
    public static final ContainerType<ModStorageBusContainer> TYPE = ContainerTypeBuilder
            .create(ModStorageBusContainer::new, ModStorageBus.class)
            .requirePermission(SecurityPermissions.BUILD)
            .build("expansionae_mod_storage_bus");

    private final ModStorageBus host;

    @GuiSync(13)
    public String filter = "";

    public ModStorageBusContainer(int id, PlayerInventory player, ModStorageBus host) {
        super(TYPE, id, player, host);
        this.host = host;
    }

    @Override
    protected void setupConfig() {
        setupUpgrades();
    }

    @Override
    public void detectAndSendChanges() {
        if (isServer()) filter = host.getModFilter();
        super.detectAndSendChanges();
    }

    @Override
    public void applyTextFilter(int key, String value) {
        if (key == 0) host.setModFilter(value);
    }
}
