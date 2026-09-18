package dev.bulkcloud.expansionae.client;

import appeng.api.config.FuzzyMode;
import appeng.api.config.RedstoneMode;
import appeng.api.config.SchedulingMode;
import appeng.api.config.Settings;
import appeng.api.config.Upgrades;
import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ServerSettingToggleButton;
import appeng.client.gui.widgets.SettingToggleButton;
import dev.bulkcloud.expansionae.ThresholdExportBusContainer;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public final class ThresholdExportBusScreen extends UpgradeableScreen<ThresholdExportBusContainer> {
    private final SettingToggleButton<RedstoneMode> redstone;
    private final SettingToggleButton<FuzzyMode> fuzzy;
    private final SettingToggleButton<SchedulingMode> scheduling;
    private Button threshold;

    public ThresholdExportBusScreen(ThresholdExportBusContainer container, PlayerInventory player,
            ITextComponent title, ScreenStyle style) {
        super(container, player, title, style);
        redstone = new ServerSettingToggleButton<>(Settings.REDSTONE_CONTROLLED, RedstoneMode.IGNORE);
        fuzzy = new ServerSettingToggleButton<>(Settings.FUZZY_MODE, FuzzyMode.IGNORE_ALL);
        scheduling = new ServerSettingToggleButton<>(Settings.SCHEDULING_MODE, SchedulingMode.DEFAULT);
        addToLeftToolbar(redstone);
        addToLeftToolbar(fuzzy);
        addToLeftToolbar(scheduling);
    }

    @Override
    protected void init() {
        super.init();
        threshold = addButton(new Button(guiLeft + 108, guiTop + 64, 80, 18,
                thresholdText(), b -> container.toggleThresholdMode()));
    }

    private ITextComponent thresholdText() {
        return new TranslationTextComponent(container.isLowerMode()
                ? "gui.expansionae.threshold.lower"
                : "gui.expansionae.threshold.greater");
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        redstone.set(container.getRedStoneMode());
        redstone.setVisibility(container.hasUpgrade(Upgrades.REDSTONE));
        fuzzy.set(container.getFuzzyMode());
        fuzzy.setVisibility(container.hasUpgrade(Upgrades.FUZZY));
        scheduling.set(container.getSchedulingMode());
        scheduling.setVisibility(container.hasUpgrade(Upgrades.CAPACITY));
        if (threshold != null) threshold.setMessage(thresholdText());
    }
}
