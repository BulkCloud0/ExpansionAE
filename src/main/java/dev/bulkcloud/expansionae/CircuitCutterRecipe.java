package dev.bulkcloud.expansionae;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;

/**
 * ExtendedAE Circuit Slicer recipe adapted to the 1.16.5 item/fluid APIs.
 */
public final class CircuitCutterRecipe implements IRecipe<IInventory> {
    public static final ResourceLocation TYPE_ID = new ResourceLocation(ExpansionAE.ID, "circuit_cutter");
    public static final IRecipeType<CircuitCutterRecipe> TYPE = IRecipeType.register(TYPE_ID.toString());

    private final ResourceLocation id;
    private final Ingredient input;
    private final int inputAmount;
    private final FluidStack inputFluid;
    private final ItemStack output;
    private final int energy;

    public CircuitCutterRecipe(ResourceLocation id, Ingredient input, int inputAmount,
            FluidStack inputFluid, ItemStack output, int energy) {
        this.id = id;
        this.input = input;
        this.inputAmount = Math.max(1, inputAmount);
        this.inputFluid = inputFluid.copy();
        this.output = output.copy();
        this.energy = Math.max(1, energy);
    }

    public Ingredient getInput() { return input; }
    public int getInputAmount() { return inputAmount; }
    public FluidStack getInputFluid() { return inputFluid.copy(); }
    public ItemStack getOutputItem() { return output.copy(); }
    public int getEnergy() { return energy; }

    public boolean matchesMachine(ItemStack stack, FluidStack fluid) {
        if (stack.isEmpty() || stack.getCount() < inputAmount || !input.test(stack)) {
            return false;
        }
        if (inputFluid.isEmpty()) {
            return true;
        }
        return !fluid.isEmpty()
                && fluid.isFluidEqual(inputFluid)
                && fluid.getAmount() >= inputFluid.getAmount();
    }

    @Override
    public boolean matches(IInventory inv, World worldIn) {
        if (inv.getSizeInventory() <= 0) return false;
        ItemStack stack = inv.getStackInSlot(0);
        return !stack.isEmpty() && stack.getCount() >= inputAmount && input.test(stack);
    }

    @Override public ItemStack getCraftingResult(IInventory inv) { return output.copy(); }
    @Override public boolean canFit(int width, int height) { return true; }
    @Override public ItemStack getRecipeOutput() { return output; }
    @Override public ResourceLocation getId() { return id; }
    @Override public IRecipeSerializer<?> getSerializer() { return CircuitCutterRecipeSerializer.INSTANCE; }
    @Override public IRecipeType<?> getType() { return TYPE; }
    @Override public boolean isDynamic() { return true; }
}
