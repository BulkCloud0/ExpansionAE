package dev.bulkcloud.expansionae.client;

import java.io.IOException;
import appeng.client.gui.style.StyleManager;
import dev.bulkcloud.expansionae.ExpansionAE;
import dev.bulkcloud.expansionae.ExpandedContainer;
import dev.bulkcloud.expansionae.PatternEncoderContainer;
import dev.bulkcloud.expansionae.ExpandedTerminalContainer;
import dev.bulkcloud.expansionae.ExpansionNetwork;
import dev.bulkcloud.expansionae.client.terminal.ExpandedTerminalScreen;
import net.minecraft.client.gui.ScreenManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = ExpansionAE.ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() { }
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        ExpansionNetwork.clientReceiver = ClientPackets::receive;
        event.enqueueWork(() -> ScreenManager.<ExpandedTerminalContainer, ExpandedTerminalScreen>registerFactory(ExpandedTerminalContainer.TYPE, (container, inventory, title) -> {
            try {
                return new ExpandedTerminalScreen(container, inventory, title, StyleManager.loadStyleDoc("/screens/expansionae_pattern_terminal.json"));
            } catch (IOException e) { throw new IllegalStateException("Cannot load pattern access terminal", e); }
        }));
        event.enqueueWork(() -> ScreenManager.<PatternEncoderContainer, PatternEncoderScreen>registerFactory(PatternEncoderContainer.TYPE, (container, inventory, title) -> {
            try {
                return new PatternEncoderScreen(container, inventory, title, StyleManager.loadStyleDoc("/screens/expansionae_encoder.json"));
            } catch (IOException e) { throw new IllegalStateException("Cannot load pattern encoder screen", e); }
        }));
        event.enqueueWork(() -> ScreenManager.<ExpandedContainer, ExpandedScreen>registerFactory(ExpandedContainer.TYPE, (container, inventory, title) -> {
            try {
                String style = container.storageSlots == 36 ? "expansionae_interface" : "expansionae_provider";
                return new ExpandedScreen(container, inventory, title, StyleManager.loadStyleDoc("/screens/" + style + ".json"));
            } catch (IOException e) { throw new IllegalStateException("Cannot load ExpansionAE screen", e); }
        }));
    }
}
