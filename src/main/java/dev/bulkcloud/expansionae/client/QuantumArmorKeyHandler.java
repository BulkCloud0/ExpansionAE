package dev.bulkcloud.expansionae.client;

import org.lwjgl.glfw.GLFW;

import dev.bulkcloud.expansionae.ExpansionAE;
import dev.bulkcloud.expansionae.ExpansionNetwork;
import dev.bulkcloud.expansionae.OpenQuantumArmorMenu;
import dev.bulkcloud.expansionae.OpenQuantumArmorConfig;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ExpansionAE.ID, value = Dist.CLIENT)
public final class QuantumArmorKeyHandler {
    public static final KeyBinding PORTABLE_WORKBENCH = new KeyBinding(
            "key.expansionae.portable_workbench",
            GLFW.GLFW_KEY_UNKNOWN,
            "key.categories.expansionae");
    public static final KeyBinding ARMOR_CONFIG = new KeyBinding(
            "key.expansionae.quantum_armor_config",
            GLFW.GLFW_KEY_UNKNOWN,
            "key.categories.expansionae");

    @SubscribeEvent
    public static void onKeyInput(InputEvent.KeyInputEvent event) {
        while (PORTABLE_WORKBENCH.isPressed()) {
            ExpansionNetwork.sendToServer(new OpenQuantumArmorMenu());
        }
        while (ARMOR_CONFIG.isPressed()) {
            ExpansionNetwork.sendToServer(new OpenQuantumArmorConfig());
        }
    }

    private QuantumArmorKeyHandler() {
    }
}
