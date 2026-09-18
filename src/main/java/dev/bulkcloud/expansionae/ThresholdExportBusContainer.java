package dev.bulkcloud.expansionae;

import appeng.api.config.SecurityPermissions;
import appeng.container.SlotSemantic;
import appeng.container.guisync.GuiSync;
import appeng.container.implementations.ContainerTypeBuilder;
import appeng.container.implementations.UpgradeableContainer;
import appeng.container.slot.FakeSlot;
import appeng.container.slot.OptionalFakeSlot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.ContainerType;
import net.minecraftforge.items.IItemHandler;

public final class ThresholdExportBusContainer extends UpgradeableContainer {
    public static final ContainerType<ThresholdExportBusContainer> TYPE = ContainerTypeBuilder
            .create(ThresholdExportBusContainer::new, ThresholdExportBus.class)
            .requirePermission(SecurityPermissions.BUILD)
            .build("expansionae_threshold_export_bus");

    private final ThresholdExportBus host;

    @GuiSync(8)
    public boolean lowerMode;

    public ThresholdExportBusContainer(int id, PlayerInventory player, ThresholdExportBus host) {
        super(TYPE, id, player, host);
        this.host = host;
        registerClientAction("toggleThresholdMode", () -> host.setLowerMode(!host.isLowerMode()));
    }

    @Override
    protected void setupConfig() {
        setupUpgrades();
        IItemHandler config = getUpgradeable().getInventoryByName("config");
        addSlot(new FakeSlot(config, 0), SlotSemantic.CONFIG);
        for (int i = 1; i <= 4; i++) {
            addSlot(new OptionalFakeSlot(config, this, i, 1), SlotSemantic.CONFIG);
        }
        for (int i = 5; i <= 8; i++) {
            addSlot(new OptionalFakeSlot(config, this, i, 2), SlotSemantic.CONFIG);
        }
    }

    public void toggleThresholdMode() {
        if (isRemote()) sendClientAction("toggleThresholdMode");
        else host.setLowerMode(!host.isLowerMode());
    }

    public boolean isLowerMode() {
        return lowerMode;
    }

    @Override
    public void detectAndSendChanges() {
        if (isServer()) lowerMode = host.isLowerMode();
        super.detectAndSendChanges();
    }
}
