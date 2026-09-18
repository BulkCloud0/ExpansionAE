package dev.bulkcloud.expansionae.client;

import com.mojang.blaze3d.matrix.MatrixStack;

import appeng.api.config.FuzzyMode;
import appeng.api.config.LevelType;
import appeng.api.config.RedstoneMode;
import appeng.api.config.Settings;
import appeng.api.config.Upgrades;
import appeng.api.config.YesNo;
import appeng.client.gui.NumberEntryType;
import appeng.client.gui.implementations.NumberEntryWidget;
import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.PaletteColor;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ServerSettingToggleButton;
import appeng.client.gui.widgets.SettingToggleButton;
import dev.bulkcloud.expansionae.ThresholdLevelEmitterContainer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public final class ThresholdLevelEmitterScreen extends UpgradeableScreen<ThresholdLevelEmitterContainer> {
    private final SettingToggleButton<LevelType> levelMode;
    private final SettingToggleButton<YesNo> craftingMode;
    private final SettingToggleButton<RedstoneMode> redstoneMode;
    private final SettingToggleButton<FuzzyMode> fuzzyMode;
    private final NumberEntryWidget upper;
    private final NumberEntryWidget lower;

    public ThresholdLevelEmitterScreen(ThresholdLevelEmitterContainer container, PlayerInventory player,
            ITextComponent title, ScreenStyle style) {
        super(container, player, title, style);
        levelMode = new ServerSettingToggleButton<>(Settings.LEVEL_TYPE, LevelType.ITEM_LEVEL);
        craftingMode = new ServerSettingToggleButton<>(Settings.CRAFT_VIA_REDSTONE, YesNo.NO);
        redstoneMode = new ServerSettingToggleButton<>(Settings.REDSTONE_EMITTER, RedstoneMode.HIGH_SIGNAL);
        fuzzyMode = new ServerSettingToggleButton<>(Settings.FUZZY_MODE, FuzzyMode.IGNORE_ALL);
        addToLeftToolbar(levelMode);
        addToLeftToolbar(redstoneMode);
        addToLeftToolbar(craftingMode);
        addToLeftToolbar(fuzzyMode);

        upper = new NumberEntryWidget(NumberEntryType.LEVEL_ITEM_COUNT);
        upper.setTextFieldBounds(86, 39, 76);
        upper.setValue(container.upperValue);
        upper.setOnChange(this::saveUpper);
        widgets.add("upper", upper);

        lower = new NumberEntryWidget(NumberEntryType.LEVEL_ITEM_COUNT);
        lower.setTextFieldBounds(86, 62, 76);
        lower.setValue(container.lowerValue);
        lower.setOnChange(this::saveLower);
        lower.setOnConfirm(this::closeScreen);
        widgets.add("lower", lower);
    }

    private void saveUpper() {
        upper.getLongValue().ifPresent(container::setUpperValue);
    }

    private void saveLower() {
        lower.getLongValue().ifPresent(container::setLowerValue);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        boolean notCrafting = !container.hasUpgrade(Upgrades.CRAFTING);
        upper.setActive(notCrafting);
        lower.setActive(notCrafting);
        levelMode.active = notCrafting;
        levelMode.set(container.getLevelType());
        redstoneMode.active = notCrafting;
        redstoneMode.set(container.getRedStoneMode());
        craftingMode.set(container.getCraftingMode());
        craftingMode.setVisibility(!notCrafting);
        fuzzyMode.set(container.getFuzzyMode());
        fuzzyMode.setVisibility(container.hasUpgrade(Upgrades.FUZZY));
    }

    @Override
    public void drawFG(MatrixStack matrices, int offsetX, int offsetY, int mouseX, int mouseY) {
        super.drawFG(matrices, offsetX, offsetY, mouseX, mouseY);
        int color = style.getColor(PaletteColor.DEFAULT_TEXT_COLOR).toARGB();
        font.drawString(matrices, new TranslationTextComponent("gui.expansionae.threshold_level.upper").getString(),
                28, 43, color);
        font.drawString(matrices, new TranslationTextComponent("gui.expansionae.threshold_level.lower").getString(),
                28, 66, color);
    }

    @Override
    public void drawBG(MatrixStack matrices, int offsetX, int offsetY, int mouseX, int mouseY, float partialTicks) {
        super.drawBG(matrices, offsetX, offsetY, mouseX, mouseY, partialTicks);
        upper.render(matrices, mouseX, mouseY, partialTicks);
        lower.render(matrices, mouseX, mouseY, partialTicks);
    }
}
