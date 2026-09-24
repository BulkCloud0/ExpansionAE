package com.bulkcloud.expansionae.client;

import java.util.List;

import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Slot;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import com.bulkcloud.expansionae.feature.stockexport.StockExportBusContainer;

import appeng.api.config.FuzzyMode;
import appeng.api.config.RedstoneMode;
import appeng.api.config.SchedulingMode;
import appeng.api.config.Settings;
import appeng.api.config.Upgrades;
import appeng.api.config.YesNo;
import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ServerSettingToggleButton;
import appeng.client.gui.widgets.SettingToggleButton;
import appeng.container.SlotSemantic;

public final class StockExportBusScreen
        extends UpgradeableScreen<StockExportBusContainer> {
    private final SettingToggleButton<RedstoneMode> redstoneMode;
    private final SettingToggleButton<FuzzyMode> fuzzyMode;
    private final SettingToggleButton<SchedulingMode> schedulingMode;

    private TextFieldWidget targetField;
    private int selectedConfigSlot = -1;

    public StockExportBusScreen(
            StockExportBusContainer container,
            PlayerInventory playerInventory,
            ITextComponent title,
            ScreenStyle style) {
        super(container, playerInventory, title, style);

        this.redstoneMode = new ServerSettingToggleButton<>(
                Settings.REDSTONE_CONTROLLED,
                RedstoneMode.IGNORE);
        this.addToLeftToolbar(this.redstoneMode);

        this.fuzzyMode = new ServerSettingToggleButton<>(
                Settings.FUZZY_MODE,
                FuzzyMode.IGNORE_ALL);
        this.addToLeftToolbar(this.fuzzyMode);

        this.schedulingMode = new ServerSettingToggleButton<>(
                Settings.SCHEDULING_MODE,
                SchedulingMode.DEFAULT);
        this.addToLeftToolbar(this.schedulingMode);
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
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        this.redstoneMode.set(this.container.getRedStoneMode());
        this.redstoneMode.setVisibility(this.container.hasUpgrade(Upgrades.REDSTONE));

        this.fuzzyMode.set(this.container.getFuzzyMode());
        this.fuzzyMode.setVisibility(this.container.hasUpgrade(Upgrades.FUZZY));

        this.schedulingMode.set(this.container.getSchedulingMode());
        this.schedulingMode.setVisibility(this.container.hasUpgrade(Upgrades.CAPACITY));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        List<Slot> configSlots = this.container.getSlots(SlotSemantic.CONFIG);

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
                        Integer.toString(this.container.getTarget(i)));
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

            this.container.setTargetFromClient(this.selectedConfigSlot, amount);
        } catch (NumberFormatException ignored) {
        }
    }
}
