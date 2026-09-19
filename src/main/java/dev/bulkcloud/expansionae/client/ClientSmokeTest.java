package dev.bulkcloud.expansionae.client;

import appeng.api.config.Actionable;
import appeng.api.parts.IPartItem;
import appeng.api.storage.cells.ICellInventoryHandler;
import appeng.api.storage.channels.IFluidStorageChannel;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.core.Api;
import appeng.me.helpers.MachineSource;
import dev.bulkcloud.expansionae.ExpandedInterfacePart;
import dev.bulkcloud.expansionae.ExpandedInterfaceTile;
import dev.bulkcloud.expansionae.ExpansionAE;
import dev.bulkcloud.expansionae.InfinityCellHandler;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.Mod;

/** Opt-in CI checks. A normal client never executes this test or exits automatically. */
@Mod.EventBusSubscriber(modid = ExpansionAE.ID, value = Dist.CLIENT)
public final class ClientSmokeTest {
    private static boolean complete;

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (!Boolean.getBoolean("expansionae.smokeTest") || complete || event.phase != TickEvent.Phase.END) return;
        Minecraft client = Minecraft.getInstance();
        if (!(client.currentScreen instanceof MainMenuScreen)) return;
        complete = true;
        ExpandedInterfaceTile provider = ExpansionAE.PROVIDER_TILE.get().create();
        check(provider.getInterfaceDuality().getPatterns().getSlots() == 36, "provider pattern inventory");
        check(provider.getProxy().getMachineRepresentation().getItem() == ExpansionAE.PROVIDER_ITEM.get(), "provider network icon");
        ExpandedInterfaceTile iface = ExpansionAE.INTERFACE_TILE.get().create();
        check(iface.getInternalInventory().getSlots() == 36, "interface storage inventory");
        check(iface.getProxy().getMachineRepresentation().getItem() == ExpansionAE.INTERFACE_ITEM.get(), "interface network icon");
        check(ExpansionAE.ADV_PROVIDER_TILE.get().create().supportsAdvancedRouting(), "advanced provider type");
        check(ExpansionAE.SMALL_ADV_PROVIDER_TILE.get().create().getInterfaceDuality().getPatterns().getSlots() == 9, "small advanced provider inventory");
        check(ExpansionAE.CIRCUIT_CUTTER_TILE.get().create().getInternalInventory().getSlots() == 2,
                "circuit cutter inventory");
        check(ExpansionAE.CIRCUIT_CUTTER_TILE.get().create().getMaxProcessingTime() == 200,
                "circuit cutter processing window");
        check(ExpansionAE.INGREDIENT_BUFFER_TILE.get().create().getInternalInventory().getSlots() == 36,
                "ingredient buffer item slots");
        check(ExpansionAE.INGREDIENT_BUFFER_TILE.get().create().getFluidInventory().getTanks() == 36,
                "ingredient buffer fluid slots");
        check(ExpansionAE.CANER_TILE.get().create().getFluidHandler().getTankCapacity(0) == 64000,
                "caner fluid capacity");
        check(ExpansionAE.QUANTUM_CRAFTER_TILE.get().create().getPatternInventory().getSlots() == 9,
                "quantum crafter pattern slots");
        check(ExpansionAE.QUANTUM_CRAFTER_TILE.get().create().getOutputInventory().getSlots() == 18,
                "quantum crafter output slots");
        QuantumArmorItem quantumHelmet = (QuantumArmorItem) ExpansionAE.QUANTUM_HELMET.get();
        check(quantumHelmet.getAEMaxPower(new ItemStack(ExpansionAE.QUANTUM_HELMET.get())) == 200000000D,
                "quantum armor capacity");
        check(quantumHelmet.canInstall(QuantumUpgradeType.WATER_BREATHING),
                "quantum helmet water-breathing upgrade");
        for (Item item : new Item[]{ExpansionAE.PROVIDER_PART.get(), ExpansionAE.INTERFACE_PART.get(),
                ExpansionAE.ADV_PROVIDER_PART.get(), ExpansionAE.SMALL_ADV_PROVIDER_PART.get()}) {
            ExpandedInterfacePart part = (ExpandedInterfacePart) ((IPartItem<?>) item).createPart(new ItemStack(item));
            check(part.getItemStackRepresentation().getItem() == item, "multipart factory and identity");
        }
        InfinityCellHandler cells = new InfinityCellHandler();
        IItemStorageChannel itemChannel = Api.instance().storage().getStorageChannel(IItemStorageChannel.class);
        IFluidStorageChannel fluidChannel = Api.instance().storage().getStorageChannel(IFluidStorageChannel.class);
        ItemStack cobbleCell = new ItemStack(ExpansionAE.COBBLE_CELL.get());
        ItemStack waterCell = new ItemStack(ExpansionAE.WATER_CELL.get());
        ICellInventoryHandler<IAEItemStack> cobble = cells.getCellInventory(cobbleCell, null, itemChannel);
        ICellInventoryHandler<IAEFluidStack> water = cells.getCellInventory(waterCell, null, fluidChannel);
        check(cells.getCellInventory(cobbleCell, null, fluidChannel) == null, "cobble cell rejects fluid channel");
        check(cells.getCellInventory(waterCell, null, itemChannel) == null, "water cell rejects item channel");
        MachineSource source = new MachineSource(provider);
        for (Actionable mode : Actionable.values()) {
            IAEItemStack request = itemChannel.createStack(new ItemStack(Items.COBBLESTONE, 64));
            IAEItemStack extracted = cobble.extractItems(request, mode, source);
            check(extracted != null && extracted.getStackSize() == 64 && request.getStackSize() == 64, "cobble extraction and request ownership");
            check(cobble.extractItems(itemChannel.createStack(new ItemStack(Items.DIAMOND)), mode, source) == null, "wrong item extraction");
            check(cobble.injectItems(request, mode, source).getStackSize() == 64, "cell rejects deposits without loss");
            IAEFluidStack fluid = fluidChannel.createStack(new FluidStack(Fluids.WATER, 1000));
            check(water.extractItems(fluid, mode, source).getStackSize() == 1000, "water extraction");
            check(water.extractItems(fluidChannel.createStack(new FluidStack(Fluids.LAVA, 1000)), mode, source) == null, "wrong fluid extraction");
            check(water.injectItems(fluid, mode, source).getStackSize() == 1000, "water cell rejects deposits");
        }
        try {
            Files.write(Paths.get("expansionae-smoke-ok.txt"),
                    "Client title screen reached; block and multipart factories; network item identity; infinite cell simulation, extraction and rejected deposits passed. No world/autocrafting test was run.\n".getBytes(StandardCharsets.UTF_8));
        } catch (java.io.IOException error) { throw new IllegalStateException("Cannot write smoke-test result", error); }
        System.out.println("EXPANSIONAE_SMOKE_OK");
        client.shutdown();
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("ExpansionAE smoke test failed: " + message);
    }
    private ClientSmokeTest() { }
}
