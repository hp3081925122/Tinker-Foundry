package org.hp.tinker_foundry.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import org.hp.tinker_foundry.registry.TFRecipes;

/** 物品到熔融流体的熔炼配方。 */
public record MeltingRecipe(Ingredient ingredient, FluidStack result, int temperature, int time) implements Recipe<SingleRecipeInput> {
    /** 数据包配方编解码器，支持标签或精确物品输入。 */
    public static final MapCodec<MeltingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(MeltingRecipe::ingredient),
        FluidStack.CODEC.fieldOf("result").forGetter(MeltingRecipe::result),
        Codec.INT.fieldOf("temperature").forGetter(MeltingRecipe::temperature),
        Codec.INT.fieldOf("time").orElse(100).forGetter(MeltingRecipe::time)
    ).apply(instance, MeltingRecipe::new));

    /** 客户端同步配方内容。 */
    public static final StreamCodec<RegistryFriendlyByteBuf, MeltingRecipe> STREAM_CODEC = StreamCodec.of(MeltingRecipe::writeNetwork, MeltingRecipe::readNetwork);

    /** 从网络读取熔炼配方。 */
    private static MeltingRecipe readNetwork(RegistryFriendlyByteBuf buffer) {
        return new MeltingRecipe(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer), FluidStack.STREAM_CODEC.decode(buffer), buffer.readVarInt(), buffer.readVarInt());
    }

    /** 向网络写入熔炼配方。 */
    private static void writeNetwork(RegistryFriendlyByteBuf buffer, MeltingRecipe recipe) {
        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.ingredient);
        FluidStack.STREAM_CODEC.encode(buffer, recipe.result);
        buffer.writeVarInt(recipe.temperature);
        buffer.writeVarInt(recipe.time);
    }

    /** 检查单物品输入。 */
    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return ingredient.test(input.item());
    }

    /** 熔炼配方没有物品输出。 */
    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    /** 熔炼只占用一个逻辑槽位。 */
    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    /** 配方浏览器通过空物品结果识别流体输出。 */
    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    /** 熔炼配方不进入原版配方书，交由冶炼界面或 JEI 展示。 */
    @Override
    public boolean isSpecial() {
        return true;
    }

    /** 返回物品输入列表。 */
    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, ingredient);
    }

    /** 返回独立配方序列化器。 */
    @Override
    public RecipeSerializer<?> getSerializer() {
        return TFRecipes.MELTING_SERIALIZER.get();
    }

    /** 返回独立 melting 配方类型。 */
    @Override
    public RecipeType<?> getType() {
        return TFRecipes.MELTING.get();
    }
}
