package com.bulkcloud.expansionae.client;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;

import com.bulkcloud.expansionae.feature.extendedbus.ExpansionExportBusPart;
import com.bulkcloud.expansionae.feature.extendedbus.ExtendedBusContainer;

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

public final class ExtendedBusScreen extends UpgradeableScreen<ExtendedBusContainer> {
    private static final int IMPORT_ACCENT = 0xFF41D2FF;
    private static final int EXPORT_ACCENT = 0xFFFFB040;

    private final boolean exportBus;
    private final SettingToggleButton<RedstoneMode> redstoneMode;
    private final SettingToggleButton<FuzzyMode> fuzzyMode;
    private final SettingToggleButton<YesNo> craftMode;
    private final SettingToggleButton<SchedulingMode> schedulingMode;

    public ExtendedBusScreen(
            ExtendedBusContainer container,
            PlayerInventory inventory,
            ITextComponent title,
            ScreenStyle style) {
        super(container, inventory, title, style);
        this.exportBus = container.getUpgradeable() instanceof ExpansionExportBusPart;

        this.redstoneMode = new ServerSettingToggleButton<>(
                Settings.REDSTONE_CONTROLLED, RedstoneMode.IGNORE);
        this.addToLeftToolbar(this.redstoneMode);

        this.fuzzyMode = new ServerSettingToggleButton<>(
                Settings.FUZZY_MODE, FuzzyMode.IGNORE_ALL);
        this.addToLeftToolbar(this.fuzzyMode);

        if (this.exportBus) {
            this.craftMode = new ServerSettingToggleButton<>(Settings.CRAFT_ONLY, YesNo.NO);
            this.addToLeftToolbar(this.craftMode);
            this.schedulingMode = new ServerSettingToggleButton<>(
                    Settings.SCHEDULING_MODE, SchedulingMode.DEFAULT);
            this.addToLeftToolbar(this.schedulingMode);
        } else {
            this.craftMode = null;
            this.schedulingMode = null;
        }
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        this.redstoneMode.set(this.container.getRedStoneMode());
        this.redstoneMode.setVisibility(this.container.hasUpgrade(Upgrades.REDSTONE));
        this.fuzzyMode.set(this.container.getFuzzyMode());
        this.fuzzyMode.setVisibility(this.container.hasUpgrade(Upgrades.FUZZY));

        if (this.craftMode != null) {
            this.craftMode.set(this.container.getCraftingMode());
            this.craftMode.setVisibility(this.container.hasUpgrade(Upgrades.CRAFTING));
        }
        if (this.schedulingMode != null) {
            this.schedulingMode.set(this.container.getSchedulingMode());
            this.schedulingMode.setVisibility(this.container.hasUpgrade(Upgrades.CAPACITY));
        }
    }

    @Override
    public void drawBG(
            MatrixStack matrixStack,
            int offsetX,
            int offsetY,
            int mouseX,
            int mouseY,
            float partialTicks) {
        super.drawBG(matrixStack, offsetX, offsetY, mouseX, mouseY, partialTicks);
        int accent = this.exportBus ? EXPORT_ACCENT : IMPORT_ACCENT;
        fill(matrixStack, offsetX + 6, offsetY + 26, offsetX + 170, offsetY + 28, accent);
    }
}
