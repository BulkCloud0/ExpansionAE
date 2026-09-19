package dev.bulkcloud.expansionae;

import appeng.api.config.SecurityPermissions;
import appeng.container.SlotSemantic;
import appeng.container.guisync.GuiSync;
import appeng.container.implementations.ContainerTypeBuilder;
import appeng.container.implementations.UpgradeableContainer;
import appeng.container.interfaces.IProgressProvider;
import appeng.container.slot.AppEngSlot;
import appeng.container.slot.OutputSlot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.items.IItemHandler;

public final class CircuitCutterContainer extends UpgradeableContainer implements IProgressProvider {
    public static final ContainerType<CircuitCutterContainer> TYPE = ContainerTypeBuilder
            .create(CircuitCutterContainer::new, CircuitCutterTile.class)
            .requirePermission(SecurityPermissions.BUILD)
            .build("expansionae_circuit_cutter");

    private final CircuitCutterTile host;

    @GuiSync(2)
    public int processingTime;
    @GuiSync(3)
    public int maxProcessingTime;
    @GuiSync(4)
    public int fluidAmount;
    @GuiSync(7)
    public boolean autoExport;

    public CircuitCutterContainer(int id, PlayerInventory player, CircuitCutterTile host) {
        super(TYPE, id, player, host);
        this.host = host;
    }

    @Override
    protected void setupConfig() {
        IItemHandler inv = host.getInternalInventory();
        addSlot(new AppEngSlot(inv, CircuitCutterTile.INPUT_SLOT), SlotSemantic.MACHINE_INPUT);
        addSlot(new OutputSlot(inv, CircuitCutterTile.OUTPUT_SLOT, null), SlotSemantic.MACHINE_OUTPUT);
        setupUpgrades();
    }

    @Override protected boolean supportCapacity() { return false; }
    @Override public int availableUpgrades() { return 4; }

    @Override
    public void detectAndSendChanges() {
        verifyPermissions(SecurityPermissions.BUILD, false);
        if (isServer()) {
            processingTime = host.getProcessingTime();
            maxProcessingTime = host.getMaxProcessingTime();
            fluidAmount = host.getFluidAmount();
            autoExport = host.isAutoExport();
        }
        standardDetectAndSendChanges();
    }

    public boolean isAutoExport() { return autoExport; }
    public int getFluidAmount() { return fluidAmount; }
    public BlockPos getHostPos() { return host.getPos(); }
    @Override public int getCurrentProgress() { return processingTime; }
    @Override public int getMaxProgress() { return Math.max(1, maxProcessingTime); }
}
