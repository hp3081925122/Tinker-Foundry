package org.hp.tinker_foundry.recipe;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import com.google.gson.JsonObject;
import org.hp.tinker_foundry.TinkerFoundry;
import org.hp.tinker_foundry.registry.TFRecipes;

/** 两种或多种流体按配方消耗后生成合金流体。 */
public record AlloyingRecipe(ResourceLocation id, List<AlloyIngredient> ingredients, FluidStack result, int temperature)
    implements Recipe<FluidRecipeInput> {
    /** 返回 Forge 1.20.1 Recipe 接口要求的配方 ID。 */
    @Override
    public ResourceLocation getId() {
        return id;
    }

    /** 合金配方中的一个流体输入，催化输入匹配但不在成功后扣除。 */
    public record AlloyIngredient(FoundryFluidAmount ingredient, boolean catalyst) {
        /** 从数据包读取带 catalyst 标记的流体输入。 */
        public static AlloyIngredient fromJson(JsonObject json) {
            return new AlloyIngredient(FoundryFluidAmount.fromJson(json), json.has("catalyst") && json.get("catalyst").getAsBoolean());
        }

        /** 从网络读取合金输入。 */
        public static AlloyIngredient fromNetwork(FriendlyByteBuf buffer) {
            return new AlloyIngredient(FoundryFluidAmount.fromNetwork(buffer), buffer.readBoolean());
        }

        /** 向网络写入合金输入。 */
        public void toNetwork(FriendlyByteBuf buffer) {
            ingredient.toNetwork(buffer);
            buffer.writeBoolean(catalyst);
        }
    }

    /** 保留旧构造签名。 */
    public AlloyingRecipe(List<AlloyIngredient> ingredients, FluidStack result, int temperature) {
        this(runtimeId(), ingredients, result, temperature);
    }

    /** 从数据包读取合金配方。 */
    public static AlloyingRecipe fromJson(ResourceLocation id, JsonObject json) {
        List<AlloyIngredient> inputs = new ArrayList<>();
        for (var element : json.getAsJsonArray("ingredients")) inputs.add(AlloyIngredient.fromJson(element.getAsJsonObject()));
        if (inputs.size() < 2 || inputs.size() > 32) throw new IllegalArgumentException("Invalid alloy ingredient count: " + inputs.size());
        return new AlloyingRecipe(id, inputs, FoundryRecipeData.fluid(json.getAsJsonObject("result")), json.get("temperature").getAsInt());
    }

    /** 从网络读取合金配方。 */
    public static AlloyingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        if (size < 2 || size > 32) throw new IllegalArgumentException("Invalid alloy ingredient count: " + size);
        List<AlloyIngredient> inputs = new ArrayList<>(size);
        for (int index = 0; index < size; index++) inputs.add(AlloyIngredient.fromNetwork(buffer));
        return new AlloyingRecipe(id, inputs, FoundryRecipeData.fluid(buffer), buffer.readVarInt());
    }

    /** 向网络写入合金配方。 */
    public static void toNetwork(FriendlyByteBuf buffer, AlloyingRecipe recipe) {
        buffer.writeVarInt(recipe.ingredients.size());
        recipe.ingredients.forEach(input -> input.toNetwork(buffer));
        FoundryRecipeData.fluid(buffer, recipe.result);
        buffer.writeVarInt(recipe.temperature);
    }

    /** 使用回溯匹配避免标签输入发生贪心误配，并提前检查催化/原料数量。 */
    @Override
    public boolean matches(FluidRecipeInput input, Level level) {
        if (ingredients.size() < 2 || input.fluids().size() < ingredients.size()) return false;
        return matchRemaining(ingredients, input.fluids(), 0, new boolean[input.fluids().size()]);
    }

    /** 执行流体输入与配方输入的一对一匹配。 */
    private static boolean matchRemaining(List<AlloyIngredient> required, List<FluidStack> available, int index, boolean[] used) {
        if (index == required.size()) return true;
        AlloyIngredient ingredient = required.get(index);
        for (int fluidIndex = 0; fluidIndex < available.size(); fluidIndex++) {
            FluidStack stack = available.get(fluidIndex);
            if (!used[fluidIndex] && stack.getAmount() >= ingredient.ingredient().amount()
                && ingredient.ingredient().test(stack)) {
                used[fluidIndex] = true;
                if (matchRemaining(required, available, index + 1, used)) return true;
                used[fluidIndex] = false;
            }
        }
        return false;
    }

    /** 合金配方没有物品输出。 */
    @Override
    public ItemStack assemble(FluidRecipeInput input) {
        return ItemStack.EMPTY;
    }

    /** 流体配方占用一个逻辑工作单元。 */
    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    /** 返回空物品结果。 */
    @Override
    public ItemStack getResultItem() {
        return ItemStack.EMPTY;
    }

    /** 合金配方不进入原版配方书。 */
    @Override
    public boolean isSpecial() {
        return true;
    }

    /** 返回合金配方序列化器。 */
    @Override
    public RecipeSerializer<?> getSerializer() {
        return TFRecipes.ALLOYING_SERIALIZER.get();
    }

    /** 返回独立 alloying 配方类型。 */
    @Override
    public RecipeType<?> getType() {
        return TFRecipes.ALLOYING.get();
    }

    /** 旧构造使用的稳定内部 ID。 */
    private static ResourceLocation runtimeId() {
        return new ResourceLocation(TinkerFoundry.MOD_ID, "alloying/runtime");
    }
}
