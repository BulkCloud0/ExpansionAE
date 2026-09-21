package dev.bulkcloud.expansionae;

import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;

public final class ExpansionNetwork {
    private static final String VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ExpansionAE.ID, "terminal"), () -> VERSION, VERSION::equals, VERSION::equals);
    public static Consumer<TerminalUpdate> clientReceiver = message -> { };

    public static void init() {
        CHANNEL.registerMessage(0, TerminalUpdate.class, TerminalUpdate::encode, TerminalUpdate::decode,
                (message, context) -> {
                    context.get().enqueueWork(() -> clientReceiver.accept(message));
                    context.get().setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(1, TextFilterUpdate.class, TextFilterUpdate::encode, TextFilterUpdate::decode,
                TextFilterUpdate::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(2, CircuitCutterConfigUpdate.class,
                CircuitCutterConfigUpdate::encode, CircuitCutterConfigUpdate::decode,
                CircuitCutterConfigUpdate::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(3, CanerModeUpdate.class,
                CanerModeUpdate::encode, CanerModeUpdate::decode,
                CanerModeUpdate::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(4, PortableWorkbenchAction.class,
                PortableWorkbenchAction::encode, PortableWorkbenchAction::decode,
                PortableWorkbenchAction::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(5, OpenQuantumArmorMenu.class,
                OpenQuantumArmorMenu::encode, OpenQuantumArmorMenu::decode,
                OpenQuantumArmorMenu::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(6, OpenQuantumArmorConfig.class,
                OpenQuantumArmorConfig::encode, OpenQuantumArmorConfig::decode,
                OpenQuantumArmorConfig::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }
    public static void send(ServerPlayerEntity player, TerminalUpdate update) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), update);
    }
    public static void sendToServer(TextFilterUpdate update) {
        CHANNEL.sendToServer(update);
    }
    public static void sendToServer(CircuitCutterConfigUpdate update) {
        CHANNEL.sendToServer(update);
    }
    public static void sendToServer(CanerModeUpdate update) {
        CHANNEL.sendToServer(update);
    }
    public static void sendToServer(PortableWorkbenchAction update) {
        CHANNEL.sendToServer(update);
    }
    public static void sendToServer(OpenQuantumArmorMenu update) {
        CHANNEL.sendToServer(update);
    }
    public static void sendToServer(OpenQuantumArmorConfig update) {
        CHANNEL.sendToServer(update);
    }
    private ExpansionNetwork() { }
}
