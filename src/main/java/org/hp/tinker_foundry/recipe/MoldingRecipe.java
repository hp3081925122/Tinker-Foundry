package org.hp.tinker_foundry.recipe;

import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import com.google.gson.JsonObject;
import org.hp.tinker_foundry.TinkerFoundry;
import org.hp.tinker_foundry.registry.TFRecipes;

/** 支持模具、容器输入以及明确返还物品的浇注配方。 */
public record MoldingRecipe(ResourceLocation id, Ingredient mold, FoundryFluidAmount fluid, ItemStack result, int time, ItemStack remainder,
                            boolean patternConsumed) implements Recipe<FluidRecipeInput> {
    /** 返回 Forge 1.20.1 Recipe 接口要求的配方 ID。 */
    @Override
    public ResourceLocation getId() {
        return id;
    }

    /** 保留不带返还物品的简写构造方式。 */
    public MoldingRecipe(Ingredient mold, FoundryFluidAmount fluid, ItemStack result, int time) {
        this(runtimeId(), mold, fluid, result, time, ItemStack.EMPTY, true);
    }

    /** 保留旧构造签名。 */
    public MoldingRecipe(Ingredient mold, FoundryFluidAmount fluid, ItemStack result, int time, ItemStack remainder) {
        this(runtimeId(), mold, fluid, result, time, remainder, true);
    }

    /** 从数据包读取模具配方。 */
    public static MoldingRecipe fromJson(ResourceLocation id, JsonObject json) {
        return new MoldingRecipe(id, Ingredient.fromJson(json.get("mold")), FoundryFluidAmount.fromJson(json.getAsJsonObject("fluid")),
            FoundryRecipeData.item(json.getAsJsonObject("result")), json.has("time") ? json.get("time").getAsInt() : 60,
            json.has("remainder") ? FoundryRecipeData.item(json.getAsJsonObject("remainder")) : ItemStack.EMPTY,
            !json.has("pattern_consumed") || json.get("pattern_consumed").getAsBoolean());
    }

    /** 从网络读取模具配方。 */
    public static MoldingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
        return new MoldingRecipe(id, Ingredient.fromNetwork(buffer), FoundryFluidAmount.fromNetwork(buffer), buffer.readItem(), buffer.readVarInt(),
            buffer.readBoolean() ? buffer.readItem() : ItemStack.EMPTY, buffer.readBoolean());
    }

    /** 向网络写入模具配方。 */
    public static void toNetwork(FriendlyByteBuf buffer, MoldingRecipe recipe) {
        recipe.mold.toNetwork(buffer);
        recipe.fluid.toNetwork(buffer);
        buffer.writeItem(recipe.result);
        buffer.writeVarInt(recipe.time);
        buffer.writeBoolean(!recipe.remainder.isEmpty());
        if (!recipe.remainder.isEmpty()) buffer.writeItem(recipe.remainder);
        buffer.writeBoolean(recipe.patternConsumed);
    }

    /** 模具和流体必须同时匹配。 */
    @Override
    public boolean matches(FluidRecipeInput input, Level level) {
        return input.requireMold() && mold.test(input.getItem(0)) && !input.fluids().isEmpty() && fluid.test(input.fluids().get(0));
    }

    /** 返回模具配方输出副本。 */
    @Override
    public ItemStack assemble(FluidRecipeInput input, RegistryAccess registries) {
        return result.copy();
    }

    /** 模具配方使用一个工作单元。 */
    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    /** 返回模具配方结果。 */
    @Override
    public ItemStack getResultItem(RegistryAccess registries) {
        return result.copy();
    }

    /** 模具配方不进入原版配方书。 */
    @Override
    public boolean isSpecial() {
        return true;
    }

    /** 返回模具配方序列化器。 */
    @Override
    public RecipeSerializer<?> getSerializer() {
        return TFRecipes.MOLDING_SERIALIZER.get();
    }

    /** 返回独立 molding 配方类型。 */
    @Override
    public RecipeType<?> getType() {
        return TFRecipes.MOLDING.get();
    }

    /** 旧构造使用的稳定内部 ID。 */
    private static ResourceLocation runtimeId() {
        return new ResourceLocation(TinkerFoundry.MOD_ID, "molding/runtime");
    }
}
