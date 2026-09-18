package dev.bulkcloud.expansionae.client;

import dev.bulkcloud.expansionae.TerminalUpdate;
import dev.bulkcloud.expansionae.client.terminal.ExpandedTerminalScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;

public final class ClientPackets {
    public static void receive(TerminalUpdate message) {
        Screen screen = Minecraft.getInstance().currentScreen;
        if (screen instanceof ExpandedTerminalScreen && message.data != null) {
            ExpandedTerminalScreen terminal = (ExpandedTerminalScreen) screen;
            if (terminal.getContainer().windowId == message.windowId) {
                terminal.postInventoryUpdate(message.clear, message.inventoryId, message.data);
            }
        }
    }
    private ClientPackets() { }
}
