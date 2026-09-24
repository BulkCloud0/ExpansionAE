package com.bulkcloud.expansionae.feature.extendedbus;

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
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.container.ContainerLocator;
import appeng.container.implementations.IOBusContainer;
import appeng.parts.automation.SharedItemBusPart;
import appeng.util.Platform;

public final class ExtendedBusContainer extends IOBusContainer {
    private ExtendedBusContainer(
            ContainerType<?> type,
            int id,
            PlayerInventory inventory,
            SharedItemBusPart host) {
        super(type, id, inventory, host);
    }

    public static ExtendedBusContainer fromImportNetwork(
            int id, PlayerInventory inventory, PacketBuffer buffer) {
        return fromNetwork(
                ExpansionAEContainers.EXTENDED_IMPORT_BUS.get(),
                id, inventory, buffer, true);
    }

    public static ExtendedBusContainer fromExportNetwork(
            int id, PlayerInventory inventory, PacketBuffer buffer) {
        return fromNetwork(
                ExpansionAEContainers.EXTENDED_EXPORT_BUS.get(),
                id, inventory, buffer, false);
    }

    private static ExtendedBusContainer fromNetwork(
            ContainerType<?> type,
            int id,
            PlayerInventory inventory,
            PacketBuffer buffer,
            boolean importBus) {
        ContainerLocator locator = ContainerLocator.read(buffer);
        if (!locator.hasBlockPos() || !locator.hasSide()) {
            return null;
        }
        if (locator.getWorldId() == null
                || !locator.getWorldId().equals(
                        inventory.player.world.getDimensionKey().getLocation())) {
            return null;
        }

        TileEntity tile = inventory.player.world.getTileEntity(locator.getBlockPos());
        if (!(tile instanceof IPartHost)) {
            return null;
        }

        IPart part = ((IPartHost) tile).getPart(locator.getSide());
        SharedItemBusPart bus;
        if (importBus && part instanceof ExpansionImportBusPart) {
            bus = (ExpansionImportBusPart) part;
        } else if (!importBus && part instanceof ExpansionExportBusPart) {
            bus = (ExpansionExportBusPart) part;
        } else {
            return null;
        }

        ExtendedBusContainer container = new ExtendedBusContainer(type, id, inventory, bus);
        container.setLocator(locator);
        return container;
    }

    public static boolean openImport(PlayerEntity player, ExpansionImportBusPart host) {
        return open(player, host, ExpansionAEContainers.EXTENDED_IMPORT_BUS.get());
    }

    public static boolean openExport(PlayerEntity player, ExpansionExportBusPart host) {
        return open(player, host, ExpansionAEContainers.EXTENDED_EXPORT_BUS.get());
    }

    private static boolean open(
            PlayerEntity player,
            SharedItemBusPart host,
            ContainerType<?> type) {
        if (!(player instanceof ServerPlayerEntity)) {
            return false;
        }
        if (!Platform.checkPermissions(player, host, SecurityPermissions.BUILD, true)) {
            return false;
        }

        ContainerLocator locator = ContainerLocator.forPart(host);
        NetworkHooks.openGui(
                (ServerPlayerEntity) player,
                new SimpleNamedContainerProvider(
                        (windowId, inventory, ignored) -> {
                            ExtendedBusContainer container =
                                    new ExtendedBusContainer(type, windowId, inventory, host);
                            container.setLocator(locator);
                            return container;
                        },
                        host.getItemStack().getDisplayName()),
                locator::write);
        return true;
    }
}
