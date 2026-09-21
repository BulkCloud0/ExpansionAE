package dev.bulkcloud.expansionae.client;

import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.ScreenStyle;
import dev.bulkcloud.expansionae.QuantumCrafterContainer;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.Direction;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

public final class QuantumCrafterScreen extends UpgradeableScreen<QuantumCrafterContainer> {
    private Button patternButton;
    private Button inputButton;
    private Button minimumButton;
    private Button maximumButton;
    private Button enabledButton;
    private Button exportButton;
    private final Button[] sideButtons = new Button[6];

    public QuantumCrafterScreen(QuantumCrafterContainer container, PlayerInventory player,
            ITextComponent title, ScreenStyle style) {
        super(container, player, title, style);
    }

    @Override
    protected void init() {
        super.init();

        addButton(new Button(guiLeft + 8, guiTop + 88, 16, 16,
                new StringTextComponent("<"), b -> container.selectPattern(container.selectedPattern - 1)));
        patternButton = addButton(new Button(guiLeft + 25, guiTop + 88, 45, 16,
                patternText(), b -> { }));
        addButton(new Button(guiLeft + 71, guiTop + 88, 16, 16,
                new StringTextComponent(">"), b -> container.selectPattern(container.selectedPattern + 1)));

        addButton(new Button(guiLeft + 8, guiTop + 106, 16, 16,
                new StringTextComponent("<"), b -> container.selectInput(container.selectedInput - 1)));
        inputButton = addButton(new Button(guiLeft + 25, guiTop + 106, 45, 16,
                inputText(), b -> { }));
        addButton(new Button(guiLeft + 71, guiTop + 106, 16, 16,
                new StringTextComponent(">"), b -> container.selectInput(container.selectedInput + 1)));

        addButton(new Button(guiLeft + 8, guiTop + 124, 18, 16,
                new StringTextComponent("-"), b -> container.adjustMinimum(-1L)));
        minimumButton = addButton(new Button(guiLeft + 27, guiTop + 124, 40, 16,
                minimumText(), b -> { }));
        addButton(new Button(guiLeft + 68, guiTop + 124, 19, 16,
                new StringTextComponent("+"), b -> container.adjustMinimum(1L)));

        addButton(new Button(guiLeft + 8, guiTop + 142, 18, 16,
                new StringTextComponent("-"), b -> container.adjustMaximum(-1L)));
        maximumButton = addButton(new Button(guiLeft + 27, guiTop + 142, 40, 16,
                maximumText(), b -> { }));
        addButton(new Button(guiLeft + 68, guiTop + 142, 19, 16,
                new StringTextComponent("+"), b -> container.adjustMaximum(1L)));

        enabledButton = addButton(new Button(guiLeft + 8, guiTop + 160, 79, 16,
                enabledText(), b -> container.togglePattern()));
        exportButton = addButton(new Button(guiLeft + 8, guiTop + 178, 79, 16,
                exportText(), b -> container.toggleExport()));

        Direction[] directions = Direction.values();
        for (int i = 0; i < directions.length; i++) {
            final int ordinal = i;
            sideButtons[i] = addButton(new Button(
                    guiLeft + 8 + i * 13, guiTop + 196, 12, 14,
                    sideText(directions[i]), b -> container.toggleOutputSide(ordinal)));
        }
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        if (patternButton != null) patternButton.setMessage(patternText());
        if (inputButton != null) inputButton.setMessage(inputText());
        if (minimumButton != null) minimumButton.setMessage(minimumText());
        if (maximumButton != null) maximumButton.setMessage(maximumText());
        if (enabledButton != null) enabledButton.setMessage(enabledText());
        if (exportButton != null) exportButton.setMessage(exportText());

        Direction[] directions = Direction.values();
        for (int i = 0; i < sideButtons.length; i++) {
            if (sideButtons[i] != null) {
                sideButtons[i].setMessage(sideText(directions[i]));
                sideButtons[i].active = !container.exportToME;
            }
        }
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
        String label;
        switch (direction) {
            case DOWN: label = "D"; break;
            case UP: label = "U"; break;
            case NORTH: label = "N"; break;
            case SOUTH: label = "S"; break;
            case WEST: label = "W"; break;
            case EAST: label = "E"; break;
            default: label = "?";
        }
        boolean selected = (container.outputSideMask & (1 << direction.ordinal())) != 0;
        return new StringTextComponent(selected ? "[" + label + "]" : label);
    }

    private static String compact(long value) {
        if (value >= 1_000_000_000L) return (value / 1_000_000_000L) + "G";
        if (value >= 1_000_000L) return (value / 1_000_000L) + "M";
        if (value >= 1_000L) return (value / 1_000L) + "K";
        return Long.toString(value);
    }
}
