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
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

public final class ExpandedMolecularAssemblerContainer extends UpgradeableContainer
        implements IProgressProvider {
    public static final ContainerType<ExpandedMolecularAssemblerContainer> TYPE =
            ContainerTypeBuilder
                    .create(ExpandedMolecularAssemblerContainer::new,
                            ExpandedMolecularAssemblerTile.class)
                    .requirePermission(SecurityPermissions.BUILD)
                    .build("expansionae_ex_molecular_assembler");

    private final ExpandedMolecularAssemblerTile host;
    private final PagedHandler lane;

    @GuiSync(7)
    public int page;
    @GuiSync(4)
    public int craftProgress;

    public ExpandedMolecularAssemblerContainer(int id, PlayerInventory player,
            ExpandedMolecularAssemblerTile host) {
        super(TYPE, id, player, host);
        this.host = host;
        this.lane = new PagedHandler();

        for (int i = 0; i < 9; i++) {
            addSlot(new AppEngSlot(lane, i), SlotSemantic.MACHINE_CRAFTING_GRID);
        }
        addSlot(new OutputSlot(lane, 9, null), SlotSemantic.MACHINE_OUTPUT);

        registerClientAction("nextPage", () -> setPage((page + 1) % ExpandedMolecularAssemblerTile.LANES));
        registerClientAction("previousPage", () -> setPage(
                (page + ExpandedMolecularAssemblerTile.LANES - 1)
                        % ExpandedMolecularAssemblerTile.LANES));
    }

    @Override
    protected void setupConfig() {
        setupUpgrades();
    }

    @Override protected boolean supportCapacity() { return false; }
    @Override public int availableUpgrades() { return 5; }

    public void nextPage() {
        if (isRemote()) sendClientAction("nextPage");
        else setPage((page + 1) % ExpandedMolecularAssemblerTile.LANES);
    }

    public void previousPage() {
        if (isRemote()) sendClientAction("previousPage");
        else setPage((page + ExpandedMolecularAssemblerTile.LANES - 1)
                % ExpandedMolecularAssemblerTile.LANES);
    }

    private void setPage(int page) {
        this.page = Math.max(0, Math.min(ExpandedMolecularAssemblerTile.LANES - 1, page));
    }

    @Override
    public void detectAndSendChanges() {
        verifyPermissions(SecurityPermissions.BUILD, false);
        if (isServer()) {
            craftProgress = host.getCraftingProgress(page);
        }
        standardDetectAndSendChanges();
    }

    @Override public int getCurrentProgress() { return craftProgress; }
    @Override public int getMaxProgress() { return 100; }

    private final class PagedHandler implements IItemHandlerModifiable {
        private IItemHandler delegate() { return host.getLaneInventory(page); }
        @Override public void setStackInSlot(int slot, ItemStack stack) {
            IItemHandler handler = delegate();
            if (handler instanceof IItemHandlerModifiable) {
                ((IItemHandlerModifiable) handler).setStackInSlot(slot, stack);
            }
        }
        @Override public int getSlots() { return 10; }
        @Override public ItemStack getStackInSlot(int slot) { return delegate().getStackInSlot(slot); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return delegate().insertItem(slot, stack, simulate);
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return delegate().extractItem(slot, amount, simulate);
        }
        @Override public int getSlotLimit(int slot) { return delegate().getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return delegate().isItemValid(slot, stack);
        }
    }
}
