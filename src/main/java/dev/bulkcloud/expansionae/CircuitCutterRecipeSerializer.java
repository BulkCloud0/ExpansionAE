package dev.bulkcloud.expansionae;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.google.gson.JsonObject;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapedRecipe;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistryEntry;

/** Serializer for 1.16.5 Circuit Slicer recipes. */
public final class CircuitCutterRecipeSerializer extends ForgeRegistryEntry<IRecipeSerializer<?>>
        implements IRecipeSerializer<CircuitCutterRecipe> {
    public static final CircuitCutterRecipeSerializer INSTANCE = new CircuitCutterRecipeSerializer();

    static {
        INSTANCE.setRegistryName(CircuitCutterRecipe.TYPE_ID);
    }

    private CircuitCutterRecipeSerializer() {
    }

    @Override
    public CircuitCutterRecipe read(ResourceLocation id, JsonObject json) {
        Ingredient input = Ingredient.deserialize(json.get("input"));
        int inputAmount = JSONUtils.getInt(json, "input_count", 1);
        FluidStack fluid = json.has("fluid") ? readFluid(json.getAsJsonObject("fluid")) : FluidStack.EMPTY;
        ItemStack output = ShapedRecipe.deserializeItem(JSONUtils.getJsonObject(json, "output"));
        int energy = JSONUtils.getInt(json, "energy", 2000);
        if (output.isEmpty()) {
            throw new IllegalStateException("Circuit cutter recipe " + id + " has no output");
        }
        return new CircuitCutterRecipe(id, input, inputAmount, fluid, output, energy);
    }

    private static FluidStack readFluid(@Nullable JsonObject json) {
        if (json == null || !json.has("fluid")) return FluidStack.EMPTY;
        net.minecraft.fluid.Fluid fluid = ForgeRegistries.FLUIDS.getValue(
                new ResourceLocation(JSONUtils.getString(json, "fluid")));
        if (fluid == null) return FluidStack.EMPTY;
        return new FluidStack(fluid, JSONUtils.getInt(json, "amount", 1000));
    }

    @Nullable
    @Override
    public CircuitCutterRecipe read(ResourceLocation id, PacketBuffer buffer) {
        Ingredient input = Ingredient.read(buffer);
        int inputAmount = buffer.readVarInt();
        FluidStack fluid = FluidStack.readFromPacket(buffer);
        ItemStack output = buffer.readItemStack();
        int energy = buffer.readVarInt();
        return new CircuitCutterRecipe(id, input, inputAmount, fluid, output, energy);
    }

    @Override
    public void write(PacketBuffer buffer, CircuitCutterRecipe recipe) {
        recipe.getInput().write(buffer);
        buffer.writeVarInt(recipe.getInputAmount());
        recipe.getInputFluid().writeToPacket(buffer);
        buffer.writeItemStack(recipe.getOutputItem());
        buffer.writeVarInt(recipe.getEnergy());
    }
}
