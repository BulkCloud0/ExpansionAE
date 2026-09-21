package dev.bulkcloud.expansionae;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nullable;

import appeng.api.config.SecurityPermissions;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.container.AEBaseContainer;
import appeng.container.guisync.GuiSync;
import appeng.container.implementations.ContainerTypeBuilder;
import appeng.core.Api;
import appeng.util.helpers.ItemHandlerUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 * 1.16.5 adaptation of AdvancedAE's Quantum Crafter Terminal.
 *
 * Modern AdvancedAE renders all crafters in one virtual scrolling inventory.
 * AE2 8.4 predates that menu-host infrastructure, so this container pages
 * through active Quantum Crafters while retaining remote pattern editing and
 * every stock/output control exposed by the machine.
 */
public class QuantumCrafterTerminalContainer extends AEBaseContainer {
    public static final ContainerType<QuantumCrafterTerminalContainer> TYPE = ContainerTypeBuilder
            .create((ContainerTypeBuilder.ContainerFactory<QuantumCrafterTerminalContainer, QuantumCrafterTerminalPart>)
                    QuantumCrafterTerminalContainer::new, QuantumCrafterTerminalPart.class)
            .requirePermission(SecurityPermissions.BUILD)
            .build("expansionae_quantum_crafter_terminal");

    private final RemotePatternHandler remotePatterns = new RemotePatternHandler();

    @GuiSync(20) public int machineCount;
    @GuiSync(21) public int selectedMachine;
    @GuiSync(22) public int selectedPattern;
    @GuiSync(23) public int selectedInput;
    @GuiSync(24) public boolean selectedEnabled;
    @GuiSync(25) public long selectedMinimum;
    @GuiSync(26) public long selectedMaximum;
    @GuiSync(27) public boolean exportToME;
    @GuiSync(28) public int outputSideMask;
    @GuiSync(29) public int machineX;
    @GuiSync(30) public int machineY;
    @GuiSync(31) public int machineZ;

    public QuantumCrafterTerminalContainer(int id, PlayerInventory player, QuantumCrafterTerminalPart host) {
        this(TYPE, id, player, host);
    }

    protected QuantumCrafterTerminalContainer(ContainerType<?> type, int id, PlayerInventory player,
            IActionHost host) {
        super(type, id, player, host);

        for (int i = 0; i < QuantumCrafterTile.PATTERN_SLOTS; i++) {
            addSlot(new QuantumPatternSlot(remotePatterns, i, 12 + i * 18, 42, player.player));
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(player, column + row * 9 + 9,
                        30 + column * 18, 174 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(player, column, 30 + column * 18, 232));
        }

        registerClientAction("selectMachine", Integer.class, this::selectMachineServer);
        registerClientAction("selectPattern", Integer.class, value -> {
            selectedPattern = clamp(value, 0, QuantumCrafterTile.PATTERN_SLOTS - 1);
            selectedInput = 0;
        });
        registerClientAction("selectInput", Integer.class,
                value -> selectedInput = clamp(value, 0, QuantumCrafterTile.INPUT_CONFIG_SLOTS - 1));
        registerClientAction("togglePattern", () -> {
            QuantumCrafterTile machine = managedMachine();
            if (machine != null) machine.togglePatternEnabled(selectedPattern);
        });
        registerClientAction("adjustMinimum", Long.class, value -> {
            QuantumCrafterTile machine = managedMachine();
            if (machine != null) machine.adjustMinimumInputStock(selectedPattern, selectedInput, value);
        });
        registerClientAction("adjustMaximum", Long.class, value -> {
            QuantumCrafterTile machine = managedMachine();
            if (machine != null) machine.adjustMaximumOutputStock(selectedPattern, value);
        });
        registerClientAction("toggleExport", () -> {
            QuantumCrafterTile machine = managedMachine();
            if (machine != null) machine.toggleExportToME();
        });
        registerClientAction("toggleOutputSide", Integer.class, value -> {
            QuantumCrafterTile machine = managedMachine();
            if (machine != null) machine.toggleOutputSide(value);
        });
    }

    public void selectMachine(int value) {
        int normalized = Math.max(0, value);
        selectedMachine = normalized;
        if (isRemote()) sendClientAction("selectMachine", Integer.valueOf(normalized));
    }

    public void selectPattern(int value) {
        int normalized = clamp(value, 0, QuantumCrafterTile.PATTERN_SLOTS - 1);
        selectedPattern = normalized;
        selectedInput = 0;
        if (isRemote()) sendClientAction("selectPattern", Integer.valueOf(normalized));
    }

    public void selectInput(int value) {
        int normalized = clamp(value, 0, QuantumCrafterTile.INPUT_CONFIG_SLOTS - 1);
        selectedInput = normalized;
        if (isRemote()) sendClientAction("selectInput", Integer.valueOf(normalized));
    }

    public void togglePattern() {
        if (isRemote()) sendClientAction("togglePattern");
        else {
            QuantumCrafterTile machine = managedMachine();
            if (machine != null) machine.togglePatternEnabled(selectedPattern);
        }
    }

    public void adjustMinimum(long delta) {
        if (isRemote()) sendClientAction("adjustMinimum", Long.valueOf(delta));
        else {
            QuantumCrafterTile machine = managedMachine();
            if (machine != null) machine.adjustMinimumInputStock(selectedPattern, selectedInput, delta);
        }
    }

    public void adjustMaximum(long delta) {
        if (isRemote()) sendClientAction("adjustMaximum", Long.valueOf(delta));
        else {
            QuantumCrafterTile machine = managedMachine();
            if (machine != null) machine.adjustMaximumOutputStock(selectedPattern, delta);
        }
    }

    public void toggleExport() {
        if (isRemote()) sendClientAction("toggleExport");
        else {
            QuantumCrafterTile machine = managedMachine();
            if (machine != null) machine.toggleExportToME();
        }
    }

    public void toggleOutputSide(int ordinal) {
        if (isRemote()) sendClientAction("toggleOutputSide", Integer.valueOf(ordinal));
        else {
            QuantumCrafterTile machine = managedMachine();
            if (machine != null) machine.toggleOutputSide(ordinal);
        }
    }

    private void selectMachineServer(Integer value) {
        List<QuantumCrafterTile> machines = activeMachines();
        selectedMachine = machines.isEmpty() ? 0 : clamp(value, 0, machines.size() - 1);
        selectedPattern = 0;
        selectedInput = 0;
    }

    @Override
    public void detectAndSendChanges() {
        if (isServer()) {
            verifyPermissions(SecurityPermissions.BUILD, true);
            refreshState();
        }
        super.detectAndSendChanges();
    }

    private void refreshState() {
        List<QuantumCrafterTile> machines = activeMachines();
        machineCount = machines.size();
        if (machines.isEmpty()) {
            selectedMachine = 0;
            selectedEnabled = false;
            selectedMinimum = 0;
            selectedMaximum = 0;
            exportToME = true;
            outputSideMask = 0;
            machineX = machineY = machineZ = 0;
            return;
        }

        selectedMachine = clamp(selectedMachine, 0, machines.size() - 1);
        QuantumCrafterTile machine = machines.get(selectedMachine);
        selectedPattern = clamp(selectedPattern, 0, QuantumCrafterTile.PATTERN_SLOTS - 1);
        selectedInput = clamp(selectedInput, 0, QuantumCrafterTile.INPUT_CONFIG_SLOTS - 1);
        selectedEnabled = machine.isPatternEnabled(selectedPattern);
        selectedMinimum = machine.getMinimumInputStock(selectedPattern, selectedInput);
        selectedMaximum = machine.getMaximumOutputStock(selectedPattern);
        exportToME = machine.isExportToME();
        outputSideMask = machine.getOutputSideMask();
        machineX = machine.getPos().getX();
        machineY = machine.getPos().getY();
        machineZ = machine.getPos().getZ();
    }

    @Nullable
    protected QuantumCrafterTile selectedMachineTile() {
        List<QuantumCrafterTile> machines = activeMachines();
        return machines.isEmpty() ? null : machines.get(clamp(selectedMachine, 0, machines.size() - 1));
    }

    @Nullable
    private QuantumCrafterTile managedMachine() {
        if (!isServer() || !isValidContainer() || !hasAccess(SecurityPermissions.BUILD, true)) return null;
        return selectedMachineTile();
    }

    private List<QuantumCrafterTile> activeMachines() {
        List<QuantumCrafterTile> result = new ArrayList<>();
        IGrid grid = getGrid();
        if (grid == null) return result;

        for (IGridNode node : grid.getMachines(QuantumCrafterTile.class)) {
            if (node.isActive() && node.getMachine() instanceof QuantumCrafterTile) {
                result.add((QuantumCrafterTile) node.getMachine());
            }
        }

        result.sort(Comparator
                .comparing((QuantumCrafterTile tile) -> tile.getWorld() == null ? ""
                        : tile.getWorld().getDimensionKey().getLocation().toString())
                .thenComparingLong(tile -> tile.getPos().toLong()));
        return result;
    }

    @Nullable
    private IGrid getGrid() {
        IActionHost host = getActionHost();
        IGridNode node = host == null ? null : host.getActionableNode();
        return node != null && node.isActive() ? node.getGrid() : null;
    }

    private static int clamp(Integer value, int min, int max) {
        int v = value == null ? min : value.intValue();
        return Math.max(min, Math.min(max, v));
    }

    private final class RemotePatternHandler implements IItemHandlerModifiable {
        private final ItemStackHandler clientShadow =
                new ItemStackHandler(QuantumCrafterTile.PATTERN_SLOTS);

        private IItemHandler delegate() {
            QuantumCrafterTile tile = selectedMachineTile();
            return tile == null ? null : tile.getPatternInventory();
        }

        private boolean client() {
            return getPlayerInventory().player.world.isRemote;
        }

        @Override
        public int getSlots() {
            return QuantumCrafterTile.PATTERN_SLOTS;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            IItemHandler target = client() ? clientShadow : delegate();
            return target == null ? ItemStack.EMPTY : target.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            IItemHandler target = client() ? clientShadow : delegate();
            return target == null ? stack : target.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            IItemHandler target = client() ? clientShadow : delegate();
            return target == null ? ItemStack.EMPTY : target.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            IItemHandler target = client() ? clientShadow : delegate();
            return target == null ? 1 : target.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            IItemHandler target = client() ? clientShadow : delegate();
            return target == null || target.isItemValid(slot, stack);
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            if (client()) {
                clientShadow.setStackInSlot(slot, stack);
                return;
            }
            IItemHandler target = delegate();
            if (target != null) ItemHandlerUtil.setStackInSlot(target, slot, stack);
        }
    }

    private static final class QuantumPatternSlot extends SlotItemHandler {
        private final PlayerEntity player;

        QuantumPatternSlot(IItemHandler handler, int index, int x, int y, PlayerEntity player) {
            super(handler, index, x, y);
            this.player = player;
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            if (stack.isEmpty()) return false;
            appeng.api.networking.crafting.ICraftingPatternDetails details =
                    Api.instance().crafting().decodePattern(stack, player.world);
            return details != null && details.isCraftable() && super.isItemValid(stack);
        }

        @Override
        public int getSlotStackLimit() {
            return 1;
        }
    }
}
