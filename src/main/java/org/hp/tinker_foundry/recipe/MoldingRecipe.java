package org.hp.tinker_foundry.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import org.hp.tinker_foundry.registry.TFRecipes;

/** 支持模具、容器输入以及明确返还物品的浇注配方。 */
public record MoldingRecipe(Ingredient mold, SizedFluidIngredient fluid, ItemStack result, int time, ItemStack remainder) implements Recipe<FluidRecipeInput> {
    /** 保留不带返还物品的简写构造方式。 */
    public MoldingRecipe(Ingredient mold, SizedFluidIngredient fluid, ItemStack result, int time) {
        this(mold, fluid, result, time, ItemStack.EMPTY);
    }

    /** 模具配方输入、流体输入和输出统一由 Codec 读写。 */
    public static final MapCodec<MoldingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Ingredient.CODEC_NONEMPTY.fieldOf("mold").forGetter(MoldingRecipe::mold),
        SizedFluidIngredient.NESTED_CODEC.fieldOf("fluid").forGetter(MoldingRecipe::fluid),
        ItemStack.CODEC.fieldOf("result").forGetter(MoldingRecipe::result),
        Codec.INT.fieldOf("time").orElse(60).forGetter(MoldingRecipe::time),
        ItemStack.CODEC.optionalFieldOf("remainder", ItemStack.EMPTY).forGetter(MoldingRecipe::remainder)
    ).apply(instance, MoldingRecipe::new));

    /** 客户端同步配方内容。 */
    public static final StreamCodec<RegistryFriendlyByteBuf, MoldingRecipe> STREAM_CODEC = StreamCodec.of(MoldingRecipe::writeNetwork, MoldingRecipe::readNetwork);

    /** 从网络读取模具配方。 */
    private static MoldingRecipe readNetwork(RegistryFriendlyByteBuf buffer) {
        return new MoldingRecipe(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer), SizedFluidIngredient.STREAM_CODEC.decode(buffer),
            ItemStack.STREAM_CODEC.decode(buffer), buffer.readVarInt(), ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer));
    }

    /** 向网络写入模具配方。 */
    private static void writeNetwork(RegistryFriendlyByteBuf buffer, MoldingRecipe recipe) {
        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.mold);
        SizedFluidIngredient.STREAM_CODEC.encode(buffer, recipe.fluid);
        ItemStack.STREAM_CODEC.encode(buffer, recipe.result);
        buffer.writeVarInt(recipe.time);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, recipe.remainder);
    }

    /** 模具和流体必须同时匹配。 */
    @Override
    public boolean matches(FluidRecipeInput input, Level level) {
        return mold.test(input.item()) && !input.fluids().isEmpty()
            && input.fluids().get(0).getAmount() >= fluid.amount() && fluid.test(input.fluids().get(0));
    }

    /** 返回模具配方输出副本。 */
    @Override
    public ItemStack assemble(FluidRecipeInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    /** 模具配方使用一个工作单元。 */
    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    /** 返回模具配方结果。 */
    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result.copy();
    }

    /** 模具配方不进入原版配方书，交由冶炼界面或 JEI 展示。 */
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
}
