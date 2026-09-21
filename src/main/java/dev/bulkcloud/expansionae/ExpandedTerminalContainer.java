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

package dev.bulkcloud.expansionae;
// Adapted for ExpansionAE on 2026-09-18.
import appeng.container.implementations.ContainerTypeBuilder;
import java.util.ArrayList;
import java.util.List;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.items.IItemHandler;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import appeng.api.config.SecurityPermissions;
import appeng.api.config.Settings;
import appeng.api.config.YesNo;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.container.AEBaseContainer;
import appeng.core.AELog;


import appeng.helpers.DualityInterface;
import appeng.helpers.IInterfaceHost;
import appeng.helpers.InventoryAction;
import appeng.items.misc.EncodedPatternItem;
import appeng.parts.misc.InterfacePart;

import appeng.tile.inventory.AppEngInternalInventory;
import appeng.tile.misc.InterfaceTileEntity;
import appeng.util.InventoryAdaptor;
import appeng.util.helpers.ItemHandlerUtil;
import appeng.util.inv.AdaptorItemHandler;
import appeng.util.inv.WrapperCursorItemHandler;
import appeng.util.inv.WrapperFilteredItemHandler;
import appeng.util.inv.WrapperRangeItemHandler;
import appeng.util.inv.filter.IAEItemFilter;

/**
 * @see appeng.client.gui.me.interfaceterminal.InterfaceTerminalScreen
 */
public class ExpandedTerminalContainer extends AEBaseContainer {

    public static final ContainerType<ExpandedTerminalContainer> TYPE = ContainerTypeBuilder
            .create((ContainerTypeBuilder.ContainerFactory<ExpandedTerminalContainer, ExpandedTerminalPart>) ExpandedTerminalContainer::new,
                    ExpandedTerminalPart.class)
            .requirePermission(SecurityPermissions.BUILD)
            .build("expansionae_pattern_terminal");

    /**
     * this stuff is all server side..
     */

    // We use this serial number to uniquely identify all inventories we send to the client
    // It is used in packets sent by the client to interact with these inventories
    private static long inventorySerial = Long.MIN_VALUE;
    private final Map<IInterfaceHost, List<InvTracker>> diList = new IdentityHashMap<>();
    private final Long2ObjectOpenHashMap<InvTracker> byId = new Long2ObjectOpenHashMap<>();

    public ExpandedTerminalContainer(int id, final PlayerInventory ip, final ExpandedTerminalPart anchor) {
        this(TYPE, id, ip, anchor);
    }

    protected ExpandedTerminalContainer(ContainerType<?> type, int id, final PlayerInventory ip,
            final IActionHost anchor) {
        super(type, id, ip, anchor);
        this.createPlayerInventorySlots(ip);
    }

    @Override
    public void detectAndSendChanges() {
        if (isClient()) {
            return;
        }

        super.detectAndSendChanges();

        IGrid grid = getGrid();

        verifyPermissions(SecurityPermissions.BUILD, true);
        List<IInterfaceHost> hosts = visibleHosts(grid);
        boolean rebuild = hosts.size() != diList.size();
        for (IInterfaceHost host : hosts) {
            List<InvTracker> rows = diList.get(host);
            DualityInterface dual = host.getInterfaceDuality();
            if (rows == null || rows.isEmpty() || !rows.get(0).name.equals(dual.getTermName())) rebuild = true;
        }
        if (rebuild) sendFullUpdate(grid, this::sendUpdate);
        else sendIncrementalUpdate(this::sendUpdate);
    }

    private void sendUpdate(TerminalUpdate update) {
        update.windowId = windowId;
        ExpansionNetwork.send((ServerPlayerEntity) getPlayerInventory().player, update);
    }

    @Nullable
    private IGrid getGrid() {
        IActionHost host = getActionHost();
        IGridNode node = host == null ? null : host.getActionableNode();
        return node != null && node.isActive() ? node.getGrid() : null;
    }

    private List<IInterfaceHost> visibleHosts(@Nullable IGrid grid) {
        List<IInterfaceHost> result = new ArrayList<>();
        if (grid == null) return result;
        for (IGridNode node : grid.getNodes()) {
            if (!node.isActive() || !(node.getMachine() instanceof IInterfaceHost)) continue;
            IInterfaceHost host = (IInterfaceHost) node.getMachine();
            DualityInterface dual = host.getInterfaceDuality();
            if (dual.getPatterns().getSlots() > 0 && dual.getConfigManager().getSetting(Settings.INTERFACE_TERMINAL) == YesNo.YES) {
                result.add(host);
            }
        }
        return result;
    }

    @Override
    public void doAction(final ServerPlayerEntity player, final InventoryAction action, final int slot, final long id) {
        if (player != getPlayerInventory().player || !isValidContainer() || !hasAccess(SecurityPermissions.BUILD, true)) return;
        final InvTracker inv = this.byId.get(id);
        if (inv == null || !visibleHosts(getGrid()).contains(inv.host)) {
            // Can occur if the client sent an interaction packet right before an inventory got removed
            return;
        }
        if (slot < 0 || slot >= inv.server.getSlots()) {
            // Client refers to an invalid slot. This should NOT happen
            AELog.warn("Client refers to invalid slot %d of inventory %s", slot, inv.name.getString());
            return;
        }

        final ItemStack is = inv.server.getStackInSlot(slot);
        final boolean hasItemInHand = !player.inventory.getItemStack().isEmpty();

        final InventoryAdaptor playerHand = new AdaptorItemHandler(new WrapperCursorItemHandler(player.inventory));

        final IItemHandler theSlot = new WrapperFilteredItemHandler(
                new WrapperRangeItemHandler(inv.server, slot, slot + 1), new PatternSlotFilter());
        final InventoryAdaptor interfaceSlot = new AdaptorItemHandler(theSlot);

        switch (action) {
            case PICKUP_OR_SET_DOWN:

                if (hasItemInHand) {
                    ItemStack inSlot = theSlot.getStackInSlot(0);
                    if (inSlot.isEmpty()) {
                        player.inventory.setItemStack(interfaceSlot.addItems(player.inventory.getItemStack()));
                    } else {
                        inSlot = inSlot.copy();
                        final ItemStack inHand = player.inventory.getItemStack().copy();

                        ItemHandlerUtil.setStackInSlot(theSlot, 0, ItemStack.EMPTY);
                        player.inventory.setItemStack(ItemStack.EMPTY);

                        player.inventory.setItemStack(interfaceSlot.addItems(inHand.copy()));

                        if (player.inventory.getItemStack().isEmpty()) {
                            player.inventory.setItemStack(inSlot);
                        } else {
                            player.inventory.setItemStack(inHand);
                            ItemHandlerUtil.setStackInSlot(theSlot, 0, inSlot);
                        }
                    }
                } else {
                    ItemHandlerUtil.setStackInSlot(theSlot, 0, playerHand.addItems(theSlot.getStackInSlot(0)));
                }

                break;
            case SPLIT_OR_PLACE_SINGLE:

                if (hasItemInHand) {
                    ItemStack extra = playerHand.removeItems(1, ItemStack.EMPTY, null);
                    if (!extra.isEmpty()) {
                        extra = interfaceSlot.addItems(extra);
                    }
                    if (!extra.isEmpty()) {
                        playerHand.addItems(extra);
                    }
                } else if (!is.isEmpty()) {
                    ItemStack extra = interfaceSlot.removeItems((is.getCount() + 1) / 2, ItemStack.EMPTY, null);
                    if (!extra.isEmpty()) {
                        extra = playerHand.addItems(extra);
                    }
                    if (!extra.isEmpty()) {
                        interfaceSlot.addItems(extra);
                    }
                }

                break;
            case SHIFT_CLICK:

                final InventoryAdaptor playerInv = InventoryAdaptor.getAdaptor(player);

                ItemHandlerUtil.setStackInSlot(theSlot, 0, playerInv.addItems(theSlot.getStackInSlot(0)));

                break;
            case MOVE_REGION:

                final InventoryAdaptor playerInvAd = InventoryAdaptor.getAdaptor(player);
                for (int x = 0; x < inv.server.getSlots(); x++) {
                    ItemHandlerUtil.setStackInSlot(inv.server, x,
                            playerInvAd.addItems(inv.server.getStackInSlot(x)));
                }

                break;
            case CREATIVE_DUPLICATE:

                if (player.abilities.isCreativeMode && !hasItemInHand) {
                    player.inventory.setItemStack(is.isEmpty() ? ItemStack.EMPTY : is.copy());
                }

                break;
            default:
                return;
        }

        this.updateHeld(player);
    }

    private void sendFullUpdate(@Nullable IGrid grid, Consumer<TerminalUpdate> packetSender) {
        this.byId.clear();
        this.diList.clear();

        packetSender.accept(TerminalUpdate.clearExistingData());

        if (grid == null) {
            return;
        }

        for (IInterfaceHost host : visibleHosts(grid)) {
            DualityInterface dual = host.getInterfaceDuality();
            List<InvTracker> rows = new ArrayList<>();
            IItemHandler patterns = dual.getPatterns();
            for (int start = 0; start < patterns.getSlots(); start += 9) {
                InvTracker row = new InvTracker(host, dual,
                        new WrapperRangeItemHandler(patterns, start, Math.min(start + 9, patterns.getSlots())), dual.getTermName());
                rows.add(row);
                byId.put(row.serverId, row);
                CompoundNBT data = new CompoundNBT();
                addItems(data, row, 0, row.server.getSlots());
                packetSender.accept(TerminalUpdate.inventory(row.serverId, data));
            }
            diList.put(host, rows);
        }
    }

    private void sendIncrementalUpdate(Consumer<TerminalUpdate> packetSender) {
        for (InvTracker inv : byId.values()) {
            CompoundNBT data = new CompoundNBT();
            for (int x = 0; x < inv.server.getSlots(); x++) {
                if (isDifferent(inv.server.getStackInSlot(x), inv.client.getStackInSlot(x))) addItems(data, inv, x, 1);
            }
            if (!data.isEmpty()) packetSender.accept(TerminalUpdate.inventory(inv.serverId, data));
        }
    }

    private boolean isDifferent(final ItemStack a, final ItemStack b) {
        if (a.isEmpty() && b.isEmpty()) {
            return false;
        }

        if (a.isEmpty() || b.isEmpty()) {
            return true;
        }

        return !ItemStack.areItemStacksEqual(a, b);
    }

    private void addItems(CompoundNBT tag, InvTracker inv, int offset, int length) {
        if (tag.isEmpty()) {
            tag.putLong("sortBy", inv.sortBy);
            tag.putInt("slots", inv.server.getSlots());
            tag.putString("un", ITextComponent.Serializer.toJson(inv.name));
        }

        for (int x = 0; x < length; x++) {
            final CompoundNBT itemNBT = new CompoundNBT();

            final ItemStack is = inv.server.getStackInSlot(x + offset);

            // "update" client side.
            ItemHandlerUtil.setStackInSlot(inv.client, x + offset, is.isEmpty() ? ItemStack.EMPTY : is.copy());

            if (!is.isEmpty()) {
                is.write(itemNBT);
            }

            tag.put(Integer.toString(x + offset), itemNBT);
        }
    }

    private static class InvTracker {

        private final IInterfaceHost host;
        private final long sortBy;
        private final long serverId = inventorySerial++;
        private final ITextComponent name;
        // This is used to track the inventory contents we sent to the client for change detection
        private final IItemHandler client;
        // This is a reference to the real inventory used by this machine
        private final IItemHandler server;

        public InvTracker(IInterfaceHost host, final DualityInterface dual, final IItemHandler patterns, final ITextComponent name) {
            this.host = host;
            this.server = patterns;
            this.client = new AppEngInternalInventory(null, this.server.getSlots());
            this.name = name;
            this.sortBy = dual.getSortValue();
        }
    }

    private static class PatternSlotFilter implements IAEItemFilter {
        @Override
        public boolean allowExtract(IItemHandler inv, int slot, int amount) {
            return true;
        }

        @Override
        public boolean allowInsert(IItemHandler inv, int slot, ItemStack stack) {
            return !stack.isEmpty() && stack.getItem() instanceof EncodedPatternItem;
        }
    }
}
