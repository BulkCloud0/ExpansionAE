package dev.bulkcloud.expansionae;

import appeng.api.config.SecurityPermissions;
import appeng.container.AEBaseContainer;
import appeng.container.SlotSemantic;
import appeng.container.guisync.GuiSync;
import appeng.container.implementations.ContainerTypeBuilder;
import appeng.container.slot.AppEngSlot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.util.math.BlockPos;

public final class CanerContainer extends AEBaseContainer {
    public static final ContainerType<CanerContainer> TYPE = ContainerTypeBuilder
            .create(CanerContainer::new, CanerTile.class)
            .requirePermission(SecurityPermissions.BUILD)
            .build("expansionae_caner");

    private final CanerTile host;

    @GuiSync(0)
    public int modeOrdinal;
    @GuiSync(1)
    public int fluidAmount;

    public CanerContainer(int id, PlayerInventory player, CanerTile host) {
        super(TYPE, id, player, host);
        this.host = host;
        addSlot(new AppEngSlot(host.getInternalInventory(), 0), SlotSemantic.MACHINE_INPUT);
        createPlayerInventorySlots(player);
    }

    @Override
    public void detectAndSendChanges() {
        verifyPermissions(SecurityPermissions.BUILD, false);
        if (!isRemote()) {
            modeOrdinal = host.getMode().ordinal();
            fluidAmount = host.getFluidAmount();
        }
        super.detectAndSendChanges();
    }

    public CanerMode getMode() {
        CanerMode[] values = CanerMode.values();
        return modeOrdinal >= 0 && modeOrdinal < values.length ? values[modeOrdinal] : CanerMode.FILL;
    }

    public int getFluidAmount() { return fluidAmount; }
    public BlockPos getHostPos() { return host.getPos(); }
}
