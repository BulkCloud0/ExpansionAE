/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package dev.bulkcloud.expansionae.client.terminal;
// Adapted for ExpansionAE on 2026-09-18.

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import appeng.items.misc.EncodedPatternItem;

/**
 * This slot is used in the {@link InterfaceTerminalScreen} to interact with the internal inventory of interfaces.
 */
public class InterfaceSlot extends Slot {

    private static final Inventory EMPTY_INVENTORY = new Inventory(0);

    private final InterfaceRecord machineInv;

    public InterfaceSlot(InterfaceRecord machineInv, int machineInvSlot, int x, int y) {
        super(EMPTY_INVENTORY, machineInvSlot, x, y);
        this.machineInv = machineInv;
    }

    @Override
    public ItemStack getStack() {
        final ItemStack is = this.machineInv.getInventory().getStackInSlot(this.getSlotIndex());
        if (!is.isEmpty() && is.getItem() instanceof EncodedPatternItem) {
            final EncodedPatternItem iep = (EncodedPatternItem) is.getItem();
            final ItemStack out = iep.getOutput(is);
            if (!out.isEmpty()) {
                return out;
            }
        }
        return is;
    }

    @Override
    public boolean getHasStack() {
        return !this.getStack().isEmpty();
    }

    public InterfaceRecord getMachineInv() {
        return this.machineInv;
    }

    // The following methods are overridden to prevent client-side code from messing with the stack in the slot
    // Any interaction with the real content of this slot must go via a custom packet
    @Override
    public final boolean isItemValid(final ItemStack stack) {
        return false;
    }

    @Override
    public final void putStack(final ItemStack stack) {
    }

    @Override
    public final int getSlotStackLimit() {
        return 0;
    }

    @Override
    public final ItemStack decrStackSize(int amount) {
        return ItemStack.EMPTY;
    }

    @Override
    public final boolean canTakeStack(PlayerEntity player) {
        return false;
    }
}
