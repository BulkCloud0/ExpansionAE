package dev.bulkcloud.expansionae.client;

import appeng.api.config.AccessRestriction;
import appeng.api.config.Settings;
import appeng.api.config.StorageFilter;
import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AETextField;
import appeng.client.gui.widgets.ServerSettingToggleButton;
import appeng.client.gui.widgets.SettingToggleButton;
import dev.bulkcloud.expansionae.ExpansionNetwork;
import dev.bulkcloud.expansionae.TagStorageBusContainer;
import dev.bulkcloud.expansionae.TextFilterUpdate;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;

public final class TagStorageBusScreen extends UpgradeableScreen<TagStorageBusContainer> {
    private final SettingToggleButton<AccessRestriction> access;
    private final SettingToggleButton<StorageFilter> storageFilter;
    private AETextField whitelist;
    private AETextField blacklist;

    public TagStorageBusScreen(TagStorageBusContainer container, PlayerInventory player,
            ITextComponent title, ScreenStyle style) {
        super(container, player, title, style);
        access = new ServerSettingToggleButton<>(Settings.ACCESS, AccessRestriction.READ_WRITE);
        storageFilter = new ServerSettingToggleButton<>(Settings.STORAGE_FILTER, StorageFilter.EXTRACTABLE_ONLY);
        addToLeftToolbar(access);
        addToLeftToolbar(storageFilter);
    }

    @Override
    protected void init() {
        super.init();
        whitelist = new AETextField(font, guiLeft + 20, guiTop + 29, 144, 14);
        whitelist.setMaxStringLength(128);
        whitelist.setText(container.whitelist == null ? "" : container.whitelist);
        whitelist.setResponder(value -> ExpansionNetwork.sendToServer(
                new TextFilterUpdate(container.windowId, 0, value)));
        addListener(whitelist);

        blacklist = new AETextField(font, guiLeft + 20, guiTop + 52, 144, 14);
        blacklist.setMaxStringLength(128);
        blacklist.setText(container.blacklist == null ? "" : container.blacklist);
        blacklist.setResponder(value -> ExpansionNetwork.sendToServer(
                new TextFilterUpdate(container.windowId, 1, value)));
        addListener(blacklist);
        setListenerDefault(whitelist);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (button == 1 && whitelist != null && whitelist.isMouseOver(x, y)) {
            whitelist.setText("");
            return true;
        }
        if (button == 1 && blacklist != null && blacklist.isMouseOver(x, y)) {
            blacklist.setText("");
            return true;
        }
        return super.mouseClicked(x, y, button);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        access.set(container.getAccess());
        storageFilter.set(container.getStorageFilter());
        if (whitelist != null && !whitelist.isFocused() && !whitelist.getText().equals(container.whitelist)) {
            whitelist.setText(container.whitelist == null ? "" : container.whitelist);
        }
        if (blacklist != null && !blacklist.isFocused() && !blacklist.getText().equals(container.blacklist)) {
            blacklist.setText(container.blacklist == null ? "" : container.blacklist);
        }
    }
}
