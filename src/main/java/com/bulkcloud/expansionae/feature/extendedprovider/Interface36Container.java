package com.bulkcloud.expansionae.feature.extendedprovider;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.SimpleNamedContainerProvider;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.network.NetworkHooks;

import com.bulkcloud.expansionae.core.registry.ExpansionAEContainers;

import appeng.api.config.SecurityPermissions;
import appeng.api.config.Settings;
import appeng.api.config.YesNo;
import appeng.api.util.IConfigManager;
import appeng.container.ContainerLocator;
import appeng.container.SlotSemantic;
import appeng.container.guisync.GuiSync;
import appeng.container.implementations.UpgradeableContainer;
import appeng.container.slot.AppEngSlot;
import appeng.container.slot.FakeSlot;
import appeng.container.slot.RestrictedInputSlot;
import appeng.helpers.DualityInterface;
import appeng.util.Platform;

public final class Interface36Container extends UpgradeableContainer {
    @GuiSync(30)
    public YesNo blockingMode = YesNo.NO;

    @GuiSync(31)
    public YesNo interfaceTerminalMode = YesNo.YES;

    public Interface36Container(
            ContainerType<?> type,
            int id,
            PlayerInventory inventory,
            Interface36TileEntity host) {
        super(type, id, inventory, host.getInterfaceDuality().getHost());

        DualityInterface duality = host.getInterfaceDuality();
        assertExpanded(duality);

        for (int slot = 0; slot < Interface36TileEntity.SLOTS; slot++) {
            this.addSlot(
                    new RestrictedInputSlot(
                            RestrictedInputSlot.PlacableItemType.ENCODED_PATTERN,
                            duality.getPatterns(),
                            slot),
                    SlotSemantic.ENCODED_PATTERN);
            this.addSlot(
                    new FakeSlot(duality.getConfig(), slot),
                    SlotSemantic.CONFIG);
            this.addSlot(
                    new AppEngSlot(duality.getStorage(), slot),
                    SlotSemantic.STORAGE);
        }
    }

    public static Interface36Container fromNetwork(
            int id,
            PlayerInventory inventory,
            PacketBuffer buffer) {
        ContainerLocator locator = ContainerLocator.read(buffer);
        if (!locator.hasBlockPos()) {
            return null;
        }
        if (locator.getWorldId() == null
                || !locator.getWorldId().equals(
                        inventory.player.world.getDimensionKey().getLocation())) {
            return null;
        }

        TileEntity tile = inventory.player.world.getTileEntity(locator.getBlockPos());
        if (!(tile instanceof Interface36TileEntity)) {
            return null;
        }

        Interface36Container container = new Interface36Container(
                ExpansionAEContainers.INTERFACE_36.get(),
                id,
                inventory,
                (Interface36TileEntity) tile);
        container.setLocator(locator);
        return container;
    }

    public static boolean open(PlayerEntity player, Interface36TileEntity host) {
        if (!(player instanceof ServerPlayerEntity)) {
            return false;
        }
        if (!Platform.checkPermissions(player, host, SecurityPermissions.BUILD, true)) {
            return false;
        }

        ContainerLocator locator = ContainerLocator.forTileEntity(host);
        NetworkHooks.openGui(
                (ServerPlayerEntity) player,
                new SimpleNamedContainerProvider(
                        (windowId, inventory, ignored) -> {
                            Interface36Container container = new Interface36Container(
                                    ExpansionAEContainers.INTERFACE_36.get(),
                                    windowId,
                                    inventory,
                                    host);
                            container.setLocator(locator);
                            return container;
                        },
                        host.getItemStackRepresentation().getDisplayName()),
                locator::write);
        return true;
    }

    @Override
    protected void setupConfig() {
        this.setupUpgrades();
    }

    @Override
    public int availableUpgrades() {
        return 1;
    }

    @Override
    protected void loadSettingsFromHost(IConfigManager manager) {
        this.blockingMode = (YesNo) manager.getSetting(Settings.BLOCK);
        this.interfaceTerminalMode =
                (YesNo) manager.getSetting(Settings.INTERFACE_TERMINAL);
    }

    public YesNo getBlockingMode() {
        return this.blockingMode;
    }

    public YesNo getInterfaceTerminalMode() {
        return this.interfaceTerminalMode;
    }

    public static void assertExpanded(DualityInterface duality) {
        if (duality.getConfig().getSlots() != Interface36TileEntity.SLOTS
                || duality.getStorage().getSlots() != Interface36TileEntity.SLOTS
                || duality.getPatterns().getSlots() != Interface36TileEntity.SLOTS) {
            throw new IllegalStateException(
                    "ExpansionAE Interface duality was not expanded to 36/36/36 slots");
        }
    }
}
