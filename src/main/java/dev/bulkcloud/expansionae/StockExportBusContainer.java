package dev.bulkcloud.expansionae;

import appeng.api.config.SecurityPermissions;
import appeng.container.SlotSemantic;
import appeng.container.implementations.ContainerTypeBuilder;
import appeng.container.implementations.UpgradeableContainer;
import appeng.container.slot.FakeSlot;
import appeng.container.slot.OptionalFakeSlot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.ContainerType;
import net.minecraftforge.items.IItemHandler;

public final class StockExportBusContainer extends UpgradeableContainer {
    public static final ContainerType<StockExportBusContainer> TYPE = ContainerTypeBuilder
            .create(StockExportBusContainer::new, StockExportBus.class)
            .requirePermission(SecurityPermissions.BUILD)
            .build("expansionae_stock_export_bus");

    public StockExportBusContainer(int id, PlayerInventory player, StockExportBus host) {
        super(TYPE, id, player, host);
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

    @Override
    public int availableUpgrades() {
        return 6;
    }
}
