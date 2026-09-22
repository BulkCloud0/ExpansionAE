package com.bulkcloud.expansionae.core.registry;

import net.minecraft.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import com.bulkcloud.expansionae.ExpansionAE;

public final class ExpansionAEItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ExpansionAE.MOD_ID);

    private ExpansionAEItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
