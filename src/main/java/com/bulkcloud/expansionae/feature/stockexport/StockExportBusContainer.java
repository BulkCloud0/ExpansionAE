package com.bulkcloud.expansionae.feature.stockexport;

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
import appeng.container.guisync.GuiSync;
import appeng.container.implementations.IOBusContainer;
import appeng.util.Platform;

public final class StockExportBusContainer extends IOBusContainer {
    private static final String SET_TARGET_ACTION = "set_stock_target";

    private final StockExportBusPart host;

    @GuiSync(20)
    public int target0;
    @GuiSync(21)
    public int target1;
    @GuiSync(22)
    public int target2;
    @GuiSync(23)
    public int target3;
    @GuiSync(24)
    public int target4;
    @GuiSync(25)
    public int target5;
    @GuiSync(26)
    public int target6;
    @GuiSync(27)
    public int target7;
    @GuiSync(28)
    public int target8;

    public StockExportBusContainer(
            ContainerType<?> type,
            int id,
            PlayerInventory inventory,
            StockExportBusPart host) {
        super(type, id, inventory, host);
        this.host = host;
        this.registerClientAction(SET_TARGET_ACTION, TargetUpdate.class, this::applyTargetUpdate);
        this.copyTargetsFromHost();
    }

    public static StockExportBusContainer fromNetwork(
            int id,
            PlayerInventory inventory,
            PacketBuffer buffer) {
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
        if (!(part instanceof StockExportBusPart)) {
            return null;
        }

        StockExportBusContainer container = new StockExportBusContainer(
                ExpansionAEContainers.STOCK_EXPORT_BUS.get(),
                id,
                inventory,
                (StockExportBusPart) part);
        container.setLocator(locator);
        return container;
    }

    public static boolean open(PlayerEntity player, StockExportBusPart host) {
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
                            StockExportBusContainer container = new StockExportBusContainer(
                                    ExpansionAEContainers.STOCK_EXPORT_BUS.get(),
                                    windowId,
                                    inventory,
                                    host);
                            container.setLocator(locator);
                            return container;
                        },
                        host.getItemStack().getDisplayName()),
                locator::write);
        return true;
    }

    public int getTarget(int slot) {
        switch (slot) {
            case 0:
                return this.target0;
            case 1:
                return this.target1;
            case 2:
                return this.target2;
            case 3:
                return this.target3;
            case 4:
                return this.target4;
            case 5:
                return this.target5;
            case 6:
                return this.target6;
            case 7:
                return this.target7;
            case 8:
                return this.target8;
            default:
                throw new IndexOutOfBoundsException("Stock target slot out of range: " + slot);
        }
    }

    public void setTargetFromClient(int slot, int amount) {
        this.sendClientAction(SET_TARGET_ACTION, new TargetUpdate(slot, amount));
    }

    @Override
    public void detectAndSendChanges() {
        if (!this.isRemote()) {
            this.copyTargetsFromHost();
        }
        super.detectAndSendChanges();
    }

    private void applyTargetUpdate(TargetUpdate update) {
        if (update == null
                || update.slot < 0
                || update.slot >= StockExportTargets.SLOT_COUNT
                || update.amount < 1) {
            return;
        }

        this.host.getTargets().set(update.slot, update.amount);
        this.host.saveChanges();
        this.copyTargetsFromHost();
    }

    private void copyTargetsFromHost() {
        this.target0 = this.host.getTargets().get(0);
        this.target1 = this.host.getTargets().get(1);
        this.target2 = this.host.getTargets().get(2);
        this.target3 = this.host.getTargets().get(3);
        this.target4 = this.host.getTargets().get(4);
        this.target5 = this.host.getTargets().get(5);
        this.target6 = this.host.getTargets().get(6);
        this.target7 = this.host.getTargets().get(7);
        this.target8 = this.host.getTargets().get(8);
    }

    public static final class TargetUpdate {
        public int slot;
        public int amount;

        public TargetUpdate() {
        }

        public TargetUpdate(int slot, int amount) {
            this.slot = slot;
            this.amount = amount;
        }
    }
}
