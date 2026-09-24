package com.bulkcloud.expansionae.feature.extendedprovider;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.SimpleNamedContainerProvider;
import net.minecraft.inventory.container.Slot;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.NetworkHooks;

import com.bulkcloud.expansionae.core.registry.ExpansionAEContainers;

import appeng.api.config.SecurityPermissions;
import appeng.container.AEBaseContainer;
import appeng.container.SlotSemantic;
import appeng.container.slot.RestrictedInputSlot;
import appeng.util.Platform;

public final class PatternProvider36Container extends AEBaseContainer {
    public PatternProvider36Container(
            ContainerType<?> type,
            int id,
            PlayerInventory playerInventory,
            PatternProvider36TileEntity host) {
        super(type, id, playerInventory, host);

        for (int slot = 0; slot < PatternProvider36TileEntity.PATTERN_SLOTS; slot++) {
            int col = slot % 6;
            int row = slot / 6;
            RestrictedInputSlot patternSlot =
                    new RestrictedInputSlot(
                            RestrictedInputSlot.PlacableItemType.ENCODED_PATTERN,
                            host.getPatterns(),
                            slot);
            patternSlot.xPos = 35 + col * 18;
            patternSlot.yPos = 22 + row * 18;
            this.addSlot(patternSlot, SlotSemantic.ENCODED_PATTERN);
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                Slot playerSlot =
                        new Slot(
                                playerInventory,
                                col + row * 9 + 9,
                                8 + col * 18,
                                146 + row * 18);
                this.addSlot(playerSlot, SlotSemantic.PLAYER_INVENTORY);
            }
        }

        for (int col = 0; col < 9; col++) {
            Slot hotbar =
                    new Slot(
                            playerInventory,
                            col,
                            8 + col * 18,
                            204);
            this.addSlot(hotbar, SlotSemantic.PLAYER_HOTBAR);
        }
    }

    public static PatternProvider36Container fromNetwork(
            int id,
            PlayerInventory inventory,
            PacketBuffer buffer) {
        BlockPos pos = buffer.readBlockPos();
        TileEntity tile = inventory.player.world.getTileEntity(pos);
        if (!(tile instanceof PatternProvider36TileEntity)) {
            return null;
        }

        return new PatternProvider36Container(
                ExpansionAEContainers.PATTERN_PROVIDER_36.get(),
                id,
                inventory,
                (PatternProvider36TileEntity) tile);
    }

    public static boolean open(
            PlayerEntity player,
            PatternProvider36TileEntity host) {
        if (!(player instanceof ServerPlayerEntity)) {
            return false;
        }

        if (!Platform.checkPermissions(
                player,
                host,
                SecurityPermissions.BUILD,
                true)) {
            return false;
        }

        NetworkHooks.openGui(
                (ServerPlayerEntity) player,
                new SimpleNamedContainerProvider(
                        (windowId, inventory, ignored) ->
                                new PatternProvider36Container(
                                        ExpansionAEContainers.PATTERN_PROVIDER_36.get(),
                                        windowId,
                                        inventory,
                                        host),
                        new net.minecraft.util.text.TranslationTextComponent(
                                "block.expansionae.pattern_provider_36")),
                buffer -> buffer.writeBlockPos(host.getPos()));
        return true;
    }

    @Override
    public void detectAndSendChanges() {
        this.verifyPermissions(SecurityPermissions.BUILD, true);
        super.detectAndSendChanges();
    }
}
