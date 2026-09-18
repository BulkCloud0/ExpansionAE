package dev.bulkcloud.expansionae;

import appeng.api.config.SecurityPermissions;
import appeng.container.guisync.GuiSync;
import appeng.container.implementations.ContainerTypeBuilder;
import appeng.container.implementations.UpgradeableContainer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.ContainerType;

public final class ModExportBusContainer extends UpgradeableContainer implements TextFilterReceiver {
    public static final ContainerType<ModExportBusContainer> TYPE = ContainerTypeBuilder
            .create(ModExportBusContainer::new, ModExportBus.class)
            .requirePermission(SecurityPermissions.BUILD)
            .build("expansionae_mod_export_bus");

    private final ModExportBus host;

    @GuiSync(10)
    public String filter = "";

    public ModExportBusContainer(int id, PlayerInventory player, ModExportBus host) {
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
