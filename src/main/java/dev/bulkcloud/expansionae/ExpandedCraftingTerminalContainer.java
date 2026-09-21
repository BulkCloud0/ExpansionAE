package dev.bulkcloud.expansionae;

import java.util.List;

import appeng.api.config.SecurityPermissions;
import appeng.api.storage.ITerminalHost;
import appeng.container.ContainerNull;
import appeng.container.SlotSemantic;
import appeng.container.guisync.GuiSync;
import appeng.container.implementations.ContainerTypeBuilder;
import appeng.container.me.items.ItemTerminalContainer;
import appeng.container.slot.CraftingMatrixSlot;
import appeng.container.slot.CraftingTermSlot;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.InventoryActionPacket;
import appeng.helpers.IContainerCraftingPacket;
import appeng.helpers.InventoryAction;
import appeng.util.InventoryAdaptor;
import appeng.util.inv.AdaptorItemHandler;
import appeng.util.inv.WrapperCursorItemHandler;
import appeng.util.inv.WrapperInvItemHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.RepairContainer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.SmithingRecipe;
import net.minecraft.item.crafting.StonecuttingRecipe;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

/**
 * Multi-mode ME crafting terminal for Minecraft 1.16.5.
 *
 * The 3x3 slot presentation is retained for all modes to stay compatible with
 * AE2 8.4's terminal style. Slots outside the active recipe inventory are
 * disabled by the delegating item handler.
 */
public class ExpandedCraftingTerminalContainer extends ItemTerminalContainer
        implements IContainerCraftingPacket {
    public static final ContainerType<ExpandedCraftingTerminalContainer> TYPE = ContainerTypeBuilder
            .create(ExpandedCraftingTerminalContainer::new, ExpandedCraftingTerminalPart.class)
            .requirePermission(SecurityPermissions.CRAFT)
            .build("expansionae_crafting_terminal");

    private final ExpandedCraftingInventoryHost host;
    private final ModeInventory modeInventory;
    private final CraftingMatrixSlot[] craftingSlots = new CraftingMatrixSlot[9];
    private final ExpandedOutputSlot outputSlot;

    @GuiSync(20) public ExpandedCraftingMode mode = ExpandedCraftingMode.CRAFTING;
    @GuiSync(21) public int stonecutRecipe;
    @GuiSync(22) public int stonecutRecipeCount;
    @GuiSync(23) public int anvilCost;
    @GuiSync(24) public String anvilName = "";

    public ExpandedCraftingTerminalContainer(int id, PlayerInventory player, ExpandedCraftingTerminalPart host) {
        this(TYPE, id, player, host, host);
    }

    protected ExpandedCraftingTerminalContainer(ContainerType<?> type, int id, PlayerInventory player,
            ITerminalHost terminalHost, ExpandedCraftingInventoryHost host) {
        super(type, id, player, terminalHost, false);
        this.host = host;
        this.mode = host.getCraftingMode();
        this.modeInventory = new ModeInventory();

        for (int i = 0; i < craftingSlots.length; i++) {
            addSlot(craftingSlots[i] = new CraftingMatrixSlot(this, modeInventory, i),
                    SlotSemantic.CRAFTING_GRID);
        }

        addSlot(outputSlot = new ExpandedOutputSlot(
                player.player, getActionSource(), powerSource, terminalHost,
                modeInventory, modeInventory, this), SlotSemantic.CRAFTING_RESULT);

        createPlayerInventorySlots(player);

        registerClientAction("cycleMode", () -> setMode(mode.next()));
        registerClientAction("stoneRecipe", Integer.class, this::setStoneRecipe);
        registerClientAction("anvilName", String.class, this::setAnvilName);

        recalculateOutput();
    }

    public void cycleMode() {
        ExpandedCraftingMode next = mode.next();
        if (isRemote()) sendClientAction("cycleMode");
        mode = next;
        stonecutRecipe = 0;
        if (!isRemote()) setMode(next);
    }

    private void setMode(ExpandedCraftingMode newMode) {
        mode = newMode == null ? ExpandedCraftingMode.CRAFTING : newMode;
        stonecutRecipe = 0;
        host.setCraftingMode(mode);
        recalculateOutput();
    }

    public void previousStoneRecipe() {
        setStoneRecipeClient(stonecutRecipe - 1);
    }

    public void nextStoneRecipe() {
        setStoneRecipeClient(stonecutRecipe + 1);
    }

    private void setStoneRecipeClient(int value) {
        int count = Math.max(1, stonecutRecipeCount);
        int normalized = ((value % count) + count) % count;
        stonecutRecipe = normalized;
        if (isRemote()) sendClientAction("stoneRecipe", Integer.valueOf(normalized));
        else setStoneRecipe(normalized);
    }

    private void setStoneRecipe(Integer value) {
        stonecutRecipe = Math.max(0, value == null ? 0 : value.intValue());
        recalculateOutput();
    }

    public void changeAnvilName(String name) {
        String normalized = normalizeName(name);
        anvilName = normalized;
        if (isRemote()) sendClientAction("anvilName", normalized);
        else setAnvilName(normalized);
    }

    private void setAnvilName(String value) {
        anvilName = normalizeName(value);
        recalculateOutput();
    }

    private static String normalizeName(String value) {
        if (value == null) return "";
        return value.length() > 35 ? value.substring(0, 35) : value;
    }

    @Override
    public void detectAndSendChanges() {
        if (isServer()) {
            mode = host.getCraftingMode();
            if (mode == ExpandedCraftingMode.STONECUTTING) {
                stonecutRecipeCount = matchingStoneRecipes().size();
                if (stonecutRecipeCount <= 0) stonecutRecipe = 0;
                else if (stonecutRecipe >= stonecutRecipeCount) stonecutRecipe = stonecutRecipeCount - 1;
            } else {
                stonecutRecipeCount = 0;
            }
        }
        super.detectAndSendChanges();
    }

    @Override
    public void onServerDataSync() {
        super.onServerDataSync();
    }

    @Override
    public void onCraftMatrixChanged(IInventory inventory) {
        recalculateOutput();
    }

    private void recalculateOutput() {
        if (isRemote()) return;
        ItemStack result;
        switch (mode) {
            case SMITHING:
                result = smithingResult();
                anvilCost = 0;
                stonecutRecipeCount = 0;
                break;
            case STONECUTTING:
                List<StonecuttingRecipe> recipes = matchingStoneRecipes();
                stonecutRecipeCount = recipes.size();
                if (recipes.isEmpty()) {
                    stonecutRecipe = 0;
                    result = ItemStack.EMPTY;
                } else {
                    stonecutRecipe = Math.max(0, Math.min(stonecutRecipe, recipes.size() - 1));
                    result = recipes.get(stonecutRecipe).getCraftingResult(makeInventory(1));
                }
                anvilCost = 0;
                break;
            case ANVIL:
                result = anvilPreview(getPlayerInventory());
                // The MCP 1.16.5 cost holder is intentionally not server-public.
                // Actual XP validation and charging are delegated to the vanilla
                // RepairContainer output slot when the craft is taken.
                anvilCost = 0;
                stonecutRecipeCount = 0;
                break;
            case CRAFTING:
            default:
                result = craftingResult();
                anvilCost = 0;
                stonecutRecipeCount = 0;
                break;
        }
        outputSlot.putStack(result);
    }

    private ItemStack craftingResult() {
        CraftingInventory inv = new CraftingInventory(new ContainerNull(), 3, 3);
        for (int i = 0; i < 9; i++) inv.setInventorySlotContents(i, modeInventory.getStackInSlot(i));
        IRecipe<CraftingInventory> recipe = getPlayerInventory().player.world.getRecipeManager()
                .getRecipe(IRecipeType.CRAFTING, inv, getPlayerInventory().player.world).orElse(null);
        return recipe == null ? ItemStack.EMPTY : recipe.getCraftingResult(inv);
    }

    private ItemStack smithingResult() {
        Inventory inv = makeInventory(2);
        SmithingRecipe recipe = getPlayerInventory().player.world.getRecipeManager()
                .getRecipe(IRecipeType.SMITHING, inv, getPlayerInventory().player.world).orElse(null);
        return recipe == null ? ItemStack.EMPTY : recipe.getCraftingResult(inv);
    }

    private List<StonecuttingRecipe> matchingStoneRecipes() {
        Inventory inv = makeInventory(1);
        return getPlayerInventory().player.world.getRecipeManager()
                .getRecipes(IRecipeType.STONECUTTING, inv, getPlayerInventory().player.world);
    }

    private Inventory makeInventory(int size) {
        Inventory inv = new Inventory(size);
        for (int i = 0; i < size; i++) {
            inv.setInventorySlotContents(i, modeInventory.getStackInSlot(i));
        }
        return inv;
    }

    private RepairContainer createAnvil(PlayerInventory playerInventory) {
        RepairContainer repair = new RepairContainer(0, playerInventory);
        repair.getSlot(0).putStack(modeInventory.getStackInSlot(0).copy());
        repair.getSlot(1).putStack(modeInventory.getStackInSlot(1).copy());
        repair.updateItemName(anvilName);
        repair.updateRepairOutput();
        return repair;
    }

    private ItemStack anvilPreview(PlayerInventory playerInventory) {
        return createAnvil(playerInventory).getSlot(2).getStack().copy();
    }

    void performSpecialCraft(InventoryAction action, ServerPlayerEntity player) {
        if (mode == ExpandedCraftingMode.CRAFTING) return;

        InventoryAdaptor destination = action == InventoryAction.CRAFT_SHIFT
                ? InventoryAdaptor.getAdaptor(player)
                : new AdaptorItemHandler(new WrapperCursorItemHandler(player.inventory));
        int maxCrafts = action == InventoryAction.CRAFT_ITEM ? 1 : 64;

        for (int craft = 0; craft < maxCrafts; craft++) {
            recalculateOutput();
            ItemStack result = outputSlot.getStack().copy();
            if (result.isEmpty() || !destination.simulateAdd(result).isEmpty()) break;

            int firstCost = 1;
            int secondCost = 1;
            if (mode == ExpandedCraftingMode.ANVIL) {
                RepairContainer repair = createAnvil(player.inventory);
                result = repair.getSlot(2).getStack().copy();
                if (result.isEmpty() || !repair.getSlot(2).canTakeStack(player)) break;

                int beforeFirst = repair.getSlot(0).getStack().getCount();
                int beforeSecond = repair.getSlot(1).getStack().getCount();

                // Let vanilla apply the exact XP cost and repair-material rules
                // against the temporary copy, then mirror its input deltas back
                // into this terminal's persistent grid.
                repair.getSlot(2).onTake(player, result.copy());

                firstCost = beforeFirst - repair.getSlot(0).getStack().getCount();
                secondCost = beforeSecond - repair.getSlot(1).getStack().getCount();
                if (firstCost <= 0) firstCost = beforeFirst;
                if (secondCost < 0) secondCost = 0;
            }

            if (!consumeInputs(firstCost, secondCost)) break;

            ItemStack failed = destination.addItems(result.copy());
            if (!failed.isEmpty()) {
                player.dropItem(failed, false);
            }
        }

        recalculateOutput();
    }

    private boolean consumeInputs(int anvilFirstCost, int anvilSecondCost) {
        switch (mode) {
            case STONECUTTING:
                return consume(0, 1);
            case SMITHING:
                return consume(0, 1) && consume(1, 1);
            case ANVIL:
                return consume(0, anvilFirstCost)
                        && (anvilSecondCost <= 0 || consume(1, anvilSecondCost));
            default:
                return false;
        }
    }

    private boolean consume(int slot, int amount) {
        ItemStack current = modeInventory.getStackInSlot(slot);
        if (current.isEmpty() || current.getCount() < amount) return false;
        modeInventory.extractItem(slot, amount, false);
        return true;
    }

    public void clearCraftingGrid() {
        if (!isRemote()) return;
        NetworkHandler.instance().sendToServer(
                new InventoryActionPacket(InventoryAction.MOVE_REGION, craftingSlots[0].slotNumber, 0));
    }

    @Override
    public IItemHandler getInventoryByName(String name) {
        if ("player".equals(name)) {
            return new net.minecraftforge.items.wrapper.PlayerInvWrapper(getPlayerInventory());
        }
        return host.getInventoryByName(name);
    }

    @Override
    public boolean useRealItems() {
        return true;
    }

    @Override
    public boolean hasItemType(ItemStack itemStack, int amount) {
        for (CraftingMatrixSlot slot : craftingSlots) {
            ItemStack stack = slot.getStack();
            if (!stack.isEmpty() && appeng.util.Platform.itemComparisons().isSameItem(itemStack, stack)) {
                if (stack.getCount() >= amount) return true;
                amount -= stack.getCount();
            }
        }
        return super.hasItemType(itemStack, amount);
    }

    private final class ModeInventory implements IItemHandlerModifiable {
        private IItemHandler delegate() {
            return host.getModeInventory(mode);
        }

        @Override public int getSlots() { return 9; }

        @Override
        public ItemStack getStackInSlot(int slot) {
            IItemHandler delegate = delegate();
            return slot >= 0 && slot < delegate.getSlots()
                    ? delegate.getStackInSlot(slot) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            IItemHandler delegate = delegate();
            return slot >= 0 && slot < delegate.getSlots()
                    ? delegate.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            IItemHandler delegate = delegate();
            return slot >= 0 && slot < delegate.getSlots()
                    ? delegate.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            IItemHandler delegate = delegate();
            return slot >= 0 && slot < delegate.getSlots() ? delegate.getSlotLimit(slot) : 0;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            IItemHandler delegate = delegate();
            return slot >= 0 && slot < delegate.getSlots() && delegate.isItemValid(slot, stack);
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            IItemHandler delegate = delegate();
            if (slot < 0 || slot >= delegate.getSlots()) return;
            if (delegate instanceof IItemHandlerModifiable) {
                ((IItemHandlerModifiable) delegate).setStackInSlot(slot, stack);
            }
        }
    }

    private static final class ExpandedOutputSlot extends CraftingTermSlot {
        private final ExpandedCraftingTerminalContainer owner;

        private ExpandedOutputSlot(PlayerEntity player,
                appeng.api.networking.security.IActionSource source,
                appeng.api.networking.energy.IEnergySource energy,
                ITerminalHost storage, IItemHandler matrix, IItemHandler secondMatrix,
                ExpandedCraftingTerminalContainer owner) {
            super(player, source, energy, storage, matrix, secondMatrix, owner);
            this.owner = owner;
        }

        @Override
        public void doClick(InventoryAction action, PlayerEntity player) {
            if (owner.mode == ExpandedCraftingMode.CRAFTING) {
                super.doClick(action, player);
            } else if (player instanceof ServerPlayerEntity) {
                owner.performSpecialCraft(action, (ServerPlayerEntity) player);
            }
        }
    }

}
