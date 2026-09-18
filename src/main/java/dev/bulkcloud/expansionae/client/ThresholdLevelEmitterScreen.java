package dev.bulkcloud.expansionae.client;

import appeng.api.config.FuzzyMode;
import appeng.api.config.RedstoneMode;
import appeng.api.config.Settings;
import appeng.api.config.Upgrades;
import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AETextField;
import appeng.client.gui.widgets.ServerSettingToggleButton;
import appeng.client.gui.widgets.SettingToggleButton;
import dev.bulkcloud.expansionae.ExpansionNetwork;
import dev.bulkcloud.expansionae.TextFilterUpdate;
import dev.bulkcloud.expansionae.ThresholdLevelEmitterContainer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;

public final class ThresholdLevelEmitterScreen extends UpgradeableScreen<ThresholdLevelEmitterContainer> {
    private final SettingToggleButton<RedstoneMode> redstone;
    private final SettingToggleButton<FuzzyMode> fuzzy;
    private AETextField upper;
    private AETextField lower;

    public ThresholdLevelEmitterScreen(ThresholdLevelEmitterContainer container, PlayerInventory player,
            ITextComponent title, ScreenStyle style) {
        super(container, player, title, style);
        redstone = new ServerSettingToggleButton<>(Settings.REDSTONE_EMITTER, RedstoneMode.HIGH_SIGNAL);
        fuzzy = new ServerSettingToggleButton<>(Settings.FUZZY_MODE, FuzzyMode.IGNORE_ALL);
        addToLeftToolbar(redstone);
        addToLeftToolbar(fuzzy);
    }

    @Override
    protected void init() {
        super.init();

        upper = new AETextField(font, guiLeft + 93, guiTop + 30, 72, 14);
        upper.setMaxStringLength(19);
        upper.setText(Long.toString(container.upperValue));
        upper.setResponder(value -> send(0, value));
        addListener(upper);

        lower = new AETextField(font, guiLeft + 93, guiTop + 52, 72, 14);
        lower.setMaxStringLength(19);
        lower.setText(Long.toString(container.lowerValue));
        lower.setResponder(value -> send(1, value));
        addListener(lower);

        setListenerDefault(upper);
    }

    private void send(int key, String value) {
        if (value != null && !value.trim().isEmpty()) {
            ExpansionNetwork.sendToServer(new TextFilterUpdate(container.windowId, key, value));
        }
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        redstone.set(container.getRedStoneMode());
        fuzzy.set(container.getFuzzyMode());
        fuzzy.setVisibility(container.hasUpgrade(Upgrades.FUZZY));

        if (upper != null && !upper.isFocused()) {
            String value = Long.toString(container.upperValue);
            if (!upper.getText().equals(value)) upper.setText(value);
        }
        if (lower != null && !lower.isFocused()) {
            String value = Long.toString(container.lowerValue);
            if (!lower.getText().equals(value)) lower.setText(value);
        }
    }
}
