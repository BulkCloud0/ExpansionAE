package dev.bulkcloud.expansionae.client;

import appeng.api.config.AccessRestriction;
import appeng.api.config.FuzzyMode;
import appeng.api.config.Settings;
import appeng.api.config.StorageFilter;
import appeng.api.config.Upgrades;
import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ServerSettingToggleButton;
import appeng.client.gui.widgets.SettingToggleButton;
import dev.bulkcloud.expansionae.PreciseStorageBusContainer;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public final class PreciseStorageBusScreen extends UpgradeableScreen<PreciseStorageBusContainer> {
    private final SettingToggleButton<AccessRestriction> access;
    private final SettingToggleButton<StorageFilter> storageFilter;
    private final SettingToggleButton<FuzzyMode> fuzzy;
    private Button mode;

    public PreciseStorageBusScreen(PreciseStorageBusContainer container, PlayerInventory player,
            ITextComponent title, ScreenStyle style) {
        super(container, player, title, style);
        access = new ServerSettingToggleButton<>(Settings.ACCESS, AccessRestriction.READ_WRITE);
        storageFilter = new ServerSettingToggleButton<>(Settings.STORAGE_FILTER, StorageFilter.EXTRACTABLE_ONLY);
        fuzzy = new ServerSettingToggleButton<>(Settings.FUZZY_MODE, FuzzyMode.IGNORE_ALL);
        addToLeftToolbar(access);
        addToLeftToolbar(storageFilter);
        addToLeftToolbar(fuzzy);
    }

    @Override
    protected void init() {
        super.init();
        mode = addButton(new Button(guiLeft + 88, guiTop + 7, 82, 18,
                modeText(), b -> container.cycleStorageMode()));
    }

    private ITextComponent modeText() {
        return new TranslationTextComponent("gui.expansionae.precise_storage."
                + container.getStorageMode().name().toLowerCase(java.util.Locale.ROOT));
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        access.set(container.getReadWriteMode());
        storageFilter.set(container.getStorageFilter());
        fuzzy.set(container.getFuzzyMode());
        fuzzy.setVisibility(container.hasUpgrade(Upgrades.FUZZY));
        if (mode != null) mode.setMessage(modeText());
    }
}
