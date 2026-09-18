package dev.bulkcloud.expansionae;

import appeng.api.config.SecurityPermissions;
import appeng.container.SlotSemantic;
import appeng.container.guisync.GuiSync;
import appeng.container.implementations.ContainerTypeBuilder;
import appeng.container.implementations.UpgradeableContainer;
import appeng.container.interfaces.IProgressProvider;
import appeng.container.slot.OutputSlot;
import appeng.container.slot.RestrictedInputSlot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

public final class ExpandedInscriberContainer extends UpgradeableContainer implements IProgressProvider {
    public static final ContainerType<ExpandedInscriberContainer> TYPE = ContainerTypeBuilder
            .create(ExpandedInscriberContainer::new, ExpandedInscriberTile.class)
            .requirePermission(SecurityPermissions.BUILD)
            .build("expansionae_ex_inscriber");

    private final ExpandedInscriberTile host;
    private final PagedHandler pageInventory;

    @GuiSync(7)
    public int page;
    @GuiSync(2)
    public int maxProcessingTime;
    @GuiSync(3)
    public int processingTime;

    public ExpandedInscriberContainer(int id, PlayerInventory player, ExpandedInscriberTile host) {
        super(TYPE, id, player, host);
        this.host = host;
        this.pageInventory = new PagedHandler(host.getInternalInventory());

        RestrictedInputSlot top = new RestrictedInputSlot(
                RestrictedInputSlot.PlacableItemType.INSCRIBER_PLATE, pageInventory, 0);
        top.setStackLimit(1);
        addSlot(top, SlotSemantic.INSCRIBER_PLATE_TOP);

        RestrictedInputSlot bottom = new RestrictedInputSlot(
                RestrictedInputSlot.PlacableItemType.INSCRIBER_PLATE, pageInventory, 1);
        bottom.setStackLimit(1);
        addSlot(bottom, SlotSemantic.INSCRIBER_PLATE_BOTTOM);

        addSlot(new RestrictedInputSlot(
                RestrictedInputSlot.PlacableItemType.INSCRIBER_INPUT, pageInventory, 2),
                SlotSemantic.MACHINE_INPUT);
        addSlot(new OutputSlot(pageInventory, 3, null), SlotSemantic.MACHINE_OUTPUT);

        registerClientAction("nextPage", () -> setPage((page + 1) % ExpandedInscriberTile.THREADS));
        registerClientAction("previousPage", () -> setPage((page + ExpandedInscriberTile.THREADS - 1)
                % ExpandedInscriberTile.THREADS));
    }

    @Override
    protected void setupConfig() {
        setupUpgrades();
    }

    @Override
    protected boolean supportCapacity() {
        return false;
    }

    @Override
    public int availableUpgrades() {
        return 4;
    }

    public void nextPage() {
        if (isRemote()) sendClientAction("nextPage");
        else setPage((page + 1) % ExpandedInscriberTile.THREADS);
    }

    public void previousPage() {
        if (isRemote()) sendClientAction("previousPage");
        else setPage((page + ExpandedInscriberTile.THREADS - 1) % ExpandedInscriberTile.THREADS);
    }

    private void setPage(int value) {
        page = Math.max(0, Math.min(ExpandedInscriberTile.THREADS - 1, value));
        pageInventory.page = page;
    }

    @Override
    public void detectAndSendChanges() {
        verifyPermissions(SecurityPermissions.BUILD, false);
        pageInventory.page = page;
        if (isServer()) {
            maxProcessingTime = host.getMaxProcessingTime();
            processingTime = host.getProcessingTime(page);
        }
        standardDetectAndSendChanges();
    }

    @Override public int getCurrentProgress() { return processingTime; }
    @Override public int getMaxProgress() { return Math.max(1, maxProcessingTime); }

    private final class PagedHandler implements IItemHandlerModifiable {
        private final IItemHandler delegate;
        private int page;

        PagedHandler(IItemHandler delegate) { this.delegate = delegate; }

        private int map(int slot) {
            switch (slot) {
                case 0: return page;
                case 1: return 4 + page;
                case 2: return 8 + page;
                case 3: return 12 + page;
                default: throw new IndexOutOfBoundsException();
            }
        }

        @Override public void setStackInSlot(int slot, ItemStack stack) {
            if (delegate instanceof IItemHandlerModifiable) {
                ((IItemHandlerModifiable) delegate).setStackInSlot(map(slot), stack);
            }
        }
        @Override public int getSlots() { return 4; }
        @Override public ItemStack getStackInSlot(int slot) { return delegate.getStackInSlot(map(slot)); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return delegate.insertItem(map(slot), stack, simulate);
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return delegate.extractItem(map(slot), amount, simulate);
        }
        @Override public int getSlotLimit(int slot) { return delegate.getSlotLimit(map(slot)); }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return delegate.isItemValid(map(slot), stack); }
    }
}
