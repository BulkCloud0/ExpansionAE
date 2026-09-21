package dev.bulkcloud.expansionae.client;

import com.mojang.blaze3d.matrix.MatrixStack;

import dev.bulkcloud.expansionae.QuantumCrafterTerminalContainer;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.Direction;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

/**
 * Compact 1.16.5 Quantum Crafter terminal. Patterns remain real remote slots;
 * the controls below edit the selected crafter's stock/output configuration.
 */
public final class QuantumCrafterTerminalScreen<C extends QuantumCrafterTerminalContainer>
        extends ContainerScreen<C> {
    private Button machineButton;
    private Button patternButton;
    private Button inputButton;
    private Button minimumButton;
    private Button maximumButton;
    private Button enabledButton;
    private Button exportButton;
    private final Button[] sideButtons = new Button[6];

    public QuantumCrafterTerminalScreen(C container, PlayerInventory inventory, ITextComponent title) {
        super(container, inventory, title);
        this.xSize = 220;
        this.ySize = 258;
    }

    @Override
    protected void init() {
        super.init();

        addButton(new Button(guiLeft + 8, guiTop + 19, 18, 18,
                new StringTextComponent("<"), b -> container.selectMachine(container.selectedMachine - 1)));
        machineButton = addButton(new Button(guiLeft + 27, guiTop + 19, 166, 18,
                machineText(), b -> { }));
        addButton(new Button(guiLeft + 194, guiTop + 19, 18, 18,
                new StringTextComponent(">"), b -> container.selectMachine(container.selectedMachine + 1)));

        addButton(new Button(guiLeft + 8, guiTop + 66, 18, 16,
                new StringTextComponent("<"), b -> container.selectPattern(container.selectedPattern - 1)));
        patternButton = addButton(new Button(guiLeft + 27, guiTop + 66, 48, 16,
                patternText(), b -> { }));
        addButton(new Button(guiLeft + 76, guiTop + 66, 18, 16,
                new StringTextComponent(">"), b -> container.selectPattern(container.selectedPattern + 1)));

        addButton(new Button(guiLeft + 8, guiTop + 84, 18, 16,
                new StringTextComponent("<"), b -> container.selectInput(container.selectedInput - 1)));
        inputButton = addButton(new Button(guiLeft + 27, guiTop + 84, 48, 16,
                inputText(), b -> { }));
        addButton(new Button(guiLeft + 76, guiTop + 84, 18, 16,
                new StringTextComponent(">"), b -> container.selectInput(container.selectedInput + 1)));

        addButton(new Button(guiLeft + 8, guiTop + 102, 18, 16,
                new StringTextComponent("-"), b -> container.adjustMinimum(-1L)));
        minimumButton = addButton(new Button(guiLeft + 27, guiTop + 102, 48, 16,
                minimumText(), b -> { }));
        addButton(new Button(guiLeft + 76, guiTop + 102, 18, 16,
                new StringTextComponent("+"), b -> container.adjustMinimum(1L)));

        addButton(new Button(guiLeft + 8, guiTop + 120, 18, 16,
                new StringTextComponent("-"), b -> container.adjustMaximum(-1L)));
        maximumButton = addButton(new Button(guiLeft + 27, guiTop + 120, 48, 16,
                maximumText(), b -> { }));
        addButton(new Button(guiLeft + 76, guiTop + 120, 18, 16,
                new StringTextComponent("+"), b -> container.adjustMaximum(1L)));

        enabledButton = addButton(new Button(guiLeft + 100, guiTop + 66, 112, 16,
                enabledText(), b -> container.togglePattern()));
        exportButton = addButton(new Button(guiLeft + 100, guiTop + 84, 112, 16,
                exportText(), b -> container.toggleExport()));

        Direction[] dirs = Direction.values();
        for (int i = 0; i < dirs.length; i++) {
            final int ordinal = i;
            sideButtons[i] = addButton(new Button(guiLeft + 100 + i * 18, guiTop + 102, 17, 16,
                    sideText(dirs[i]), b -> container.toggleOutputSide(ordinal)));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (machineButton != null) machineButton.setMessage(machineText());
        if (patternButton != null) patternButton.setMessage(patternText());
        if (inputButton != null) inputButton.setMessage(inputText());
        if (minimumButton != null) minimumButton.setMessage(minimumText());
        if (maximumButton != null) maximumButton.setMessage(maximumText());
        if (enabledButton != null) enabledButton.setMessage(enabledText());
        if (exportButton != null) exportButton.setMessage(exportText());
        for (int i = 0; i < sideButtons.length; i++) {
            if (sideButtons[i] != null) {
                sideButtons[i].setMessage(sideText(Direction.values()[i]));
                sideButtons[i].active = !container.exportToME && container.machineCount > 0;
            }
        }
    }

    private ITextComponent machineText() {
        if (container.machineCount <= 0) return new StringTextComponent("No active Quantum Crafters");
        return new StringTextComponent("Crafter " + (container.selectedMachine + 1) + "/"
                + container.machineCount + " @ " + container.machineX + ","
                + container.machineY + "," + container.machineZ);
    }

    private ITextComponent patternText() {
        return new StringTextComponent("P " + (container.selectedPattern + 1) + "/9");
    }

    private ITextComponent inputText() {
        return new StringTextComponent("I " + (container.selectedInput + 1) + "/9");
    }

    private ITextComponent minimumText() {
        return new StringTextComponent("Min " + compact(container.selectedMinimum));
    }

    private ITextComponent maximumText() {
        return new StringTextComponent("Max " + compact(container.selectedMaximum));
    }

    private ITextComponent enabledText() {
        return new StringTextComponent(container.selectedEnabled ? "Pattern: ON" : "Pattern: OFF");
    }

    private ITextComponent exportText() {
        return new StringTextComponent(container.exportToME ? "Output: ME" : "Output: Sides");
    }

    private ITextComponent sideText(Direction direction) {
        String label = direction.getString().substring(0, 1).toUpperCase(java.util.Locale.ROOT);
        boolean selected = (container.outputSideMask & (1 << direction.ordinal())) != 0;
        return new StringTextComponent(selected ? "[" + label + "]" : label);
    }

    private static String compact(long value) {
        if (value >= 1_000_000_000L) return (value / 1_000_000_000L) + "G";
        if (value >= 1_000_000L) return (value / 1_000_000L) + "M";
        if (value >= 1_000L) return (value / 1_000L) + "K";
        return Long.toString(value);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(MatrixStack matrixStack, float partialTicks,
            int mouseX, int mouseY) {
        fill(matrixStack, guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, 0xFF202020);
        fill(matrixStack, guiLeft + 4, guiTop + 4, guiLeft + xSize - 4, guiTop + 17, 0xFF303030);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(MatrixStack matrixStack, int mouseX, int mouseY) {
        font.drawString(matrixStack, title.getString(), 8, 6, 0xFFFFFF);
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        renderHoveredTooltip(matrixStack, mouseX, mouseY);
    }
}
