package dev.bulkcloud.expansionae;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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

public final class ReactionChamberRecipeSerializer extends ForgeRegistryEntry<IRecipeSerializer<?>>
        implements IRecipeSerializer<ReactionChamberRecipe> {
    public static final ReactionChamberRecipeSerializer INSTANCE = new ReactionChamberRecipeSerializer();

    static {
        INSTANCE.setRegistryName(ReactionChamberRecipe.TYPE_ID);
    }

    private ReactionChamberRecipeSerializer() { }

    @Override
    public ReactionChamberRecipe read(ResourceLocation id, JsonObject json) {
        List<ReactionChamberRecipe.Input> inputs = new ArrayList<>();
        JsonArray inputArray = JSONUtils.getJsonArray(json, "input_items");
        for (JsonElement element : inputArray) {
            JsonObject input = element.getAsJsonObject();
            int amount = JSONUtils.getInt(input, "amount", 1);
            inputs.add(new ReactionChamberRecipe.Input(
                    Ingredient.deserialize(input.get("ingredient")), amount));
        }

        FluidStack inputFluid = readFluid(json.getAsJsonObject("fluid"));

        ItemStack outputItem = ItemStack.EMPTY;
        FluidStack outputFluid = FluidStack.EMPTY;
        if (json.has("output_item")) {
            outputItem = ShapedRecipe.deserializeItem(json.getAsJsonObject("output_item"));
        } else if (json.has("output_fluid")) {
            outputFluid = readFluid(json.getAsJsonObject("output_fluid"));
        } else if (json.has("output")) {
            // Compatibility with AdvancedAE's 1.20 generated reaction JSON.
            JsonObject output = json.getAsJsonObject("output");
            String channel = JSONUtils.getString(output, "#c", "");
            int amount = JSONUtils.getInt(output, "#", 1);
            ResourceLocation outputId = new ResourceLocation(JSONUtils.getString(output, "id"));
            if ("ae2:f".equals(channel)) {
                net.minecraft.fluid.Fluid fluid = ForgeRegistries.FLUIDS.getValue(outputId);
                if (fluid != null) outputFluid = new FluidStack(fluid, amount);
            } else {
                net.minecraft.item.Item item = ForgeRegistries.ITEMS.getValue(outputId);
                if (item != null) outputItem = new ItemStack(item, amount);
            }
        }

        if (outputItem.isEmpty() && outputFluid.isEmpty()) {
            throw new IllegalStateException("Reaction recipe " + id + " has no valid output");
        }

        return new ReactionChamberRecipe(id, inputs, inputFluid, outputItem, outputFluid,
                JSONUtils.getInt(json, "energy", 20000));
    }

    private static FluidStack readFluid(@Nullable JsonObject json) {
        if (json == null) return FluidStack.EMPTY;
        JsonObject data = json.has("fluidStack") ? json.getAsJsonObject("fluidStack") : json;
        String key = data.has("FluidName") ? "FluidName" : "fluid";
        String amountKey = data.has("Amount") ? "Amount" : "amount";
        if (!data.has(key)) return FluidStack.EMPTY;
        net.minecraft.fluid.Fluid fluid = ForgeRegistries.FLUIDS.getValue(
                new ResourceLocation(JSONUtils.getString(data, key)));
        return fluid == null ? FluidStack.EMPTY
                : new FluidStack(fluid, JSONUtils.getInt(data, amountKey, 1000));
    }

    @Nullable
    @Override
    public ReactionChamberRecipe read(ResourceLocation id, PacketBuffer buffer) {
        int size = buffer.readVarInt();
        List<ReactionChamberRecipe.Input> inputs = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            inputs.add(new ReactionChamberRecipe.Input(Ingredient.read(buffer), buffer.readVarInt()));
        }
        FluidStack inputFluid = FluidStack.readFromPacket(buffer);
        ItemStack outputItem = buffer.readItemStack();
        FluidStack outputFluid = FluidStack.readFromPacket(buffer);
        int energy = buffer.readVarInt();
        return new ReactionChamberRecipe(id, inputs, inputFluid, outputItem, outputFluid, energy);
    }

    @Override
    public void write(PacketBuffer buffer, ReactionChamberRecipe recipe) {
        buffer.writeVarInt(recipe.getInputs().size());
        for (ReactionChamberRecipe.Input input : recipe.getInputs()) {
            input.getIngredient().write(buffer);
            buffer.writeVarInt(input.getAmount());
        }
        recipe.getInputFluid().writeToPacket(buffer);
        buffer.writeItemStack(recipe.getOutputItem());
        recipe.getOutputFluid().writeToPacket(buffer);
        buffer.writeVarInt(recipe.getEnergy());
    }
}
