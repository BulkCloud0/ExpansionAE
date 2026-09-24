package com.bulkcloud.expansionae.client;

import java.util.List;

import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Slot;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import com.bulkcloud.expansionae.feature.stockexport.StockExportBusContainer;

import appeng.client.gui.implementations.IOBusScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.container.SlotSemantic;

public final class StockExportBusScreen extends IOBusScreen {
    private final StockExportBusContainer stockContainer;

    private TextFieldWidget targetField;
    private int selectedConfigSlot = -1;

    public StockExportBusScreen(
            StockExportBusContainer container,
            PlayerInventory playerInventory,
            ITextComponent title,
            ScreenStyle style) {
        super(container, playerInventory, title, style);
        this.stockContainer = container;
    }

    @Override
    protected void init() {
        super.init();

        this.targetField = new TextFieldWidget(
                this.font,
                this.guiLeft + 8,
                this.guiTop + 24,
                50,
                18,
                new TranslationTextComponent("gui.expansionae.stock_export.target"));
        this.targetField.setMaxStringLength(10);
        this.targetField.setText("64");
        this.addButton(this.targetField);

        this.addButton(new Button(
                this.guiLeft + 60,
                this.guiTop + 24,
                34,
                18,
                new TranslationTextComponent("gui.expansionae.stock_export.set"),
                button -> this.applyTarget()));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        List<Slot> configSlots = this.stockContainer.getSlots(SlotSemantic.CONFIG);

        for (int i = 0; i < configSlots.size(); i++) {
            Slot slot = configSlots.get(i);
            int left = this.guiLeft + slot.xPos;
            int top = this.guiTop + slot.yPos;

            if (mouseX >= left
                    && mouseX < left + 16
                    && mouseY >= top
                    && mouseY < top + 16) {
                this.selectedConfigSlot = i;
                this.targetField.setText(
                        Integer.toString(this.stockContainer.getTarget(i)));
                break;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void applyTarget() {
        if (this.selectedConfigSlot < 0) {
            return;
        }

        try {
            int amount = Integer.parseInt(this.targetField.getText());
            if (amount < 1) {
                return;
            }

            this.stockContainer.setTargetFromClient(this.selectedConfigSlot, amount);
        } catch (NumberFormatException ignored) {
        }
    }
}
