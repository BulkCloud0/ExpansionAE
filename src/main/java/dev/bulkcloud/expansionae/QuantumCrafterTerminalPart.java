package dev.bulkcloud.expansionae;

import appeng.container.ContainerLocator;
import appeng.container.ContainerOpener;
import appeng.parts.reporting.InterfaceTerminalPart;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.vector.Vector3d;

/** Network terminal used to manage Quantum Crafters attached to the same ME grid. */
public final class QuantumCrafterTerminalPart extends InterfaceTerminalPart {
    public QuantumCrafterTerminalPart(ItemStack stack) {
        super(stack);
    }

    @Override
    public boolean onPartActivate(PlayerEntity player, Hand hand, Vector3d pos) {
        if (!isRemote()) {
            ContainerOpener.openContainer(QuantumCrafterTerminalContainer.TYPE, player,
                    ContainerLocator.forPart(this));
        }
        return true;
    }
}
