package dev.bulkcloud.expansionae;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.IItemHandler;

public final class ReactionChamberRecipe implements IRecipe<IInventory> {
    public static final ResourceLocation TYPE_ID = new ResourceLocation(ExpansionAE.ID, "reaction");
    public static final IRecipeType<ReactionChamberRecipe> TYPE = IRecipeType.register(TYPE_ID.toString());

    public static final class Input {
        private final Ingredient ingredient;
        private final int amount;

        public Input(Ingredient ingredient, int amount) {
            this.ingredient = ingredient;
            this.amount = Math.max(1, amount);
        }

        public Ingredient getIngredient() { return ingredient; }
        public int getAmount() { return amount; }
    }

    private final ResourceLocation id;
    private final List<Input> inputs;
    private final FluidStack inputFluid;
    private final ItemStack outputItem;
    private final FluidStack outputFluid;
    private final int energy;

    public ReactionChamberRecipe(ResourceLocation id, List<Input> inputs, FluidStack inputFluid,
            ItemStack outputItem, FluidStack outputFluid, int energy) {
        this.id = id;
        this.inputs = Collections.unmodifiableList(new ArrayList<>(inputs));
        this.inputFluid = inputFluid.copy();
        this.outputItem = outputItem.copy();
        this.outputFluid = outputFluid.copy();
        this.energy = Math.max(1, energy);
    }

    public List<Input> getInputs() { return inputs; }
    public FluidStack getInputFluid() { return inputFluid.copy(); }
    public ItemStack getOutputItem() { return outputItem.copy(); }
    public FluidStack getOutputFluid() { return outputFluid.copy(); }
    public int getEnergy() { return energy; }

    public boolean matchesMachine(IItemHandler inventory, FluidStack fluid) {
        for (Input required : inputs) {
            int found = 0;
            for (int slot = 0; slot < 9; slot++) {
                ItemStack stack = inventory.getStackInSlot(slot);
                if (!stack.isEmpty() && required.ingredient.test(stack)) {
                    found += stack.getCount();
                }
            }
            if (found < required.amount) {
                return false;
            }
        }

        if (!inputFluid.isEmpty()) {
            return !fluid.isEmpty()
                    && fluid.isFluidEqual(inputFluid)
                    && fluid.getAmount() >= inputFluid.getAmount();
        }
        return true;
    }

    @Override public boolean matches(IInventory inv, World worldIn) { return false; }
    @Override public ItemStack getCraftingResult(IInventory inv) { return outputItem.copy(); }
    @Override public boolean canFit(int width, int height) { return true; }
    @Override public ItemStack getRecipeOutput() { return outputItem.copy(); }
    @Override public ResourceLocation getId() { return id; }
    @Override public IRecipeSerializer<?> getSerializer() { return ReactionChamberRecipeSerializer.INSTANCE; }
    @Override public IRecipeType<?> getType() { return TYPE; }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> result = NonNullList.create();
        for (Input input : inputs) result.add(input.ingredient);
        return result;
    }
}
