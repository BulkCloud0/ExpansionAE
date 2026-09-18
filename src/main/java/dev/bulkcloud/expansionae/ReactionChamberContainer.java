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
import net.minecraftforge.items.IItemHandler;

public final class ReactionChamberContainer extends UpgradeableContainer implements IProgressProvider {
    public static final ContainerType<ReactionChamberContainer> TYPE = ContainerTypeBuilder
            .create(ReactionChamberContainer::new, ReactionChamberTile.class)
            .requirePermission(SecurityPermissions.BUILD)
            .build("expansionae_reaction_chamber");

    private final ReactionChamberTile host;

    @GuiSync(2)
    public int processingTime;
    @GuiSync(3)
    public int maxProcessingTime;

    public ReactionChamberContainer(int id, PlayerInventory player, ReactionChamberTile host) {
        super(TYPE, id, player, host);
        this.host = host;
    }

    @Override
    protected void setupConfig() {
        IItemHandler inv = host.getInternalInventory();
        for (int i = 0; i < ReactionChamberTile.INPUT_SLOTS; i++) {
            addSlot(new AppEngSlot(inv, i), SlotSemantic.MACHINE_INPUT);
        }
        addSlot(new OutputSlot(inv, ReactionChamberTile.OUTPUT_SLOT, null), SlotSemantic.MACHINE_OUTPUT);
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
        }
        standardDetectAndSendChanges();
    }

    @Override public int getCurrentProgress() { return processingTime; }
    @Override public int getMaxProgress() { return Math.max(1, maxProcessingTime); }
}
