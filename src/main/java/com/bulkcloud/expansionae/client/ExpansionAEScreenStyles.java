package com.bulkcloud.expansionae.client;

import appeng.client.gui.style.ScreenStyle;

public final class ExpansionAEScreenStyles {
    private static final int TARGET_TOP = 28;
    private static final int TARGET_HEIGHT = 18;
    private static final int STOCK_CONFIG_CENTER_TOP = 66;

    private ExpansionAEScreenStyles() {
    }

    public static ScreenStyle extendedImportBus() {
        return ioBusStyle(
                "gui.expansionae.extended_import_bus.title",
                "gui.expansionae.extended_import_bus.subtitle",
                52);
    }

    public static ScreenStyle extendedExportBus() {
        return ioBusStyle(
                "gui.expansionae.extended_export_bus.title",
                "gui.expansionae.extended_export_bus.subtitle",
                52);
    }

    public static ScreenStyle stockExportBus() {
        return ioBusStyle(
                "gui.expansionae.stock_export_bus.title",
                "gui.expansionae.stock_export_bus.subtitle",
                STOCK_CONFIG_CENTER_TOP);
    }

    public static ScreenStyle interface36() {
        String json = "{"
                + "\"palette\":{\"DEFAULT_TEXT_COLOR\":\"#404040\"},"
                + "\"slots\":{"
                + "\"CONFIG\":{\"left\":8,\"top\":35,"
                + "\"grid\":\"BREAK_AFTER_9COLS\"},"
                + "\"STORAGE\":{\"left\":8,\"top\":115,"
                + "\"grid\":\"BREAK_AFTER_9COLS\"},"
                + "\"ENCODED_PATTERN\":{\"left\":8,\"top\":195,"
                + "\"grid\":\"BREAK_AFTER_9COLS\"},"
                + "\"PLAYER_INVENTORY\":{\"left\":8,\"bottom\":82,"
                + "\"grid\":\"BREAK_AFTER_9COLS\"},"
                + "\"PLAYER_HOTBAR\":{\"left\":8,\"bottom\":24,"
                + "\"grid\":\"HORIZONTAL\"},"
                + "\"TOOLBOX\":{\"bottom\":82,\"right\":-10,"
                + "\"grid\":\"BREAK_AFTER_3COLS\"}},"
                + "\"images\":{\"toolbox\":{"
                + "\"texture\":\"appliedenergistics2:textures/guis/extra_panels.png\","
                + "\"textureWidth\":128,\"textureHeight\":128,"
                + "\"srcRect\":[60,60,68,68]}},"
                + "\"widgets\":{"
                + "\"verticalToolbar\":{\"left\":-2,\"top\":6},"
                + "\"upgrades\":{\"right\":-2,\"top\":0},"
                + "\"openPriority\":{\"left\":154,\"top\":0},"
                + "\"toolbox\":{\"right\":-2,\"bottom\":90,"
                + "\"width\":68,\"height\":68}}"
                + "}";

        ScreenStyle style = ScreenStyle.GSON.fromJson(json, ScreenStyle.class);
        style.validate();
        return style;
    }

    public static int stockTargetTop() {
        return TARGET_TOP;
    }

    public static void validateContracts() {
        int topMostConfigSlot = STOCK_CONFIG_CENTER_TOP - 18;
        int editorBottom = TARGET_TOP + TARGET_HEIGHT;
        if (editorBottom >= topMostConfigSlot) {
            throw new IllegalStateException(
                    "Stock Export target editor overlaps config slots: editorBottom="
                            + editorBottom + ", configTop=" + topMostConfigSlot);
        }
    }

    private static ScreenStyle ioBusStyle(
            String titleKey,
            String subtitleKey,
            int configTop) {
        String json = "{"
                + "\"palette\":{\"DEFAULT_TEXT_COLOR\":\"#404040\"},"
                + "\"background\":{\"texture\":\"appliedenergistics2:textures/guis/bus.png\","
                + "\"srcRect\":[0,0,176,184]},"
                + "\"slots\":{"
                + "\"CONFIG\":{\"left\":80,\"top\":" + configTop + ",\"grid\":\"IO_BUS_CONFIG\"},"
                + "\"PLAYER_INVENTORY\":{\"left\":8,\"bottom\":82,\"grid\":\"BREAK_AFTER_9COLS\"},"
                + "\"PLAYER_HOTBAR\":{\"left\":8,\"bottom\":24,\"grid\":\"HORIZONTAL\"},"
                + "\"TOOLBOX\":{\"bottom\":82,\"right\":-10,\"grid\":\"BREAK_AFTER_3COLS\"}},"
                + "\"text\":{"
                + "\"dialog_title\":{\"text\":{\"translate\":\"" + titleKey + "\"},"
                + "\"position\":{\"left\":8,\"top\":6}},"
                + "\"expansionae_subtitle\":{\"text\":{\"translate\":\"" + subtitleKey
                + "\",\"color\":\"#6A6A6A\"},\"position\":{\"left\":8,\"top\":17}},"
                + "\"player_inventory_title\":{\"text\":{\"translate\":\"container.inventory\"},"
                + "\"position\":{\"left\":8,\"bottom\":93}}},"
                + "\"images\":{\"toolbox\":{\"texture\":\"appliedenergistics2:textures/guis/extra_panels.png\","
                + "\"textureWidth\":128,\"textureHeight\":128,\"srcRect\":[60,60,68,68]}},"
                + "\"widgets\":{"
                + "\"verticalToolbar\":{\"left\":-2,\"top\":6},"
                + "\"upgrades\":{\"right\":-2,\"top\":0},"
                + "\"toolbox\":{\"right\":-2,\"bottom\":90,\"width\":68,\"height\":68}}"
                + "}";

        ScreenStyle style = ScreenStyle.GSON.fromJson(json, ScreenStyle.class);
        style.validate();
        return style;
    }
}
