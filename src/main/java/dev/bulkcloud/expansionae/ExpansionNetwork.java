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
    }
    public static void send(ServerPlayerEntity player, TerminalUpdate update) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), update);
    }
    private ExpansionNetwork() { }
}
