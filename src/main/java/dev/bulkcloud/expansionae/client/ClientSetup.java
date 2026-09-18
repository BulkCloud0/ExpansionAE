package dev.bulkcloud.expansionae.client;

import java.io.IOException;
import appeng.client.gui.style.StyleManager;
import dev.bulkcloud.expansionae.ExpansionAE;
import dev.bulkcloud.expansionae.ExpandedContainer;
import net.minecraft.client.gui.ScreenManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = ExpansionAE.ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() { }
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ScreenManager.registerFactory(ExpandedContainer.TYPE, (container, inventory, title) -> {
            try {
                String style = container.storageSlots == 36 ? "expansionae_interface" : "expansionae_provider";
                return new ExpandedScreen(container, inventory, title, StyleManager.loadStyleDoc("/screens/" + style + ".json"));
            } catch (IOException e) { throw new IllegalStateException("Cannot load ExpansionAE screen", e); }
        }));
    }
}
