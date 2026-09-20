package org.hp.tinker_foundry.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
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

/** 支持矿石增产倍率的熔炼配方。 */
public record OreMeltingRecipe(Ingredient ingredient, FluidStack result, int temperature, int time, String rate,
                               List<MeltingRecipe.Byproduct> byproducts) implements Recipe<SingleRecipeInput> {
    /** 兼容只提供主产物的矿石配方。 */
    public OreMeltingRecipe(Ingredient ingredient, FluidStack result, int temperature, int time, String rate) {
        this(ingredient, result, temperature, time, rate, List.of());
    }

    /** 矿石配方的 JSON 编解码器。 */
    public static final MapCodec<OreMeltingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(OreMeltingRecipe::ingredient),
        FluidStack.CODEC.fieldOf("result").forGetter(OreMeltingRecipe::result),
        Codec.INT.fieldOf("temperature").forGetter(OreMeltingRecipe::temperature),
        Codec.INT.fieldOf("time").orElse(100).forGetter(OreMeltingRecipe::time),
        Codec.STRING.optionalFieldOf("rate", "metal").forGetter(OreMeltingRecipe::rate),
        MeltingRecipe.Byproduct.CODEC.listOf().optionalFieldOf("byproducts", List.of()).forGetter(OreMeltingRecipe::byproducts)
    ).apply(instance, OreMeltingRecipe::new));

    /** 客户端同步矿石配方。 */
    public static final StreamCodec<RegistryFriendlyByteBuf, OreMeltingRecipe> STREAM_CODEC = StreamCodec.of(
        OreMeltingRecipe::writeNetwork, OreMeltingRecipe::readNetwork);

    /** 读取网络配方。 */
    private static OreMeltingRecipe readNetwork(RegistryFriendlyByteBuf buffer) {
        Ingredient ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
        FluidStack result = FluidStack.STREAM_CODEC.decode(buffer);
        int temperature = buffer.readVarInt();
        int time = buffer.readVarInt();
        String rate = buffer.readUtf();
        int count = buffer.readVarInt();
        if (count < 0 || count > 128) throw new IllegalArgumentException("Invalid ore byproduct count: " + count);
        java.util.ArrayList<MeltingRecipe.Byproduct> byproducts = new java.util.ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            byproducts.add(new MeltingRecipe.Byproduct(FluidStack.STREAM_CODEC.decode(buffer), buffer.readUtf()));
        }
        return new OreMeltingRecipe(ingredient, result, temperature, time, rate, byproducts);
    }

    /** 写入网络配方。 */
    private static void writeNetwork(RegistryFriendlyByteBuf buffer, OreMeltingRecipe recipe) {
        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.ingredient);
        FluidStack.STREAM_CODEC.encode(buffer, recipe.result);
        buffer.writeVarInt(recipe.temperature);
        buffer.writeVarInt(recipe.time);
        buffer.writeUtf(recipe.rate);
        buffer.writeVarInt(recipe.byproducts.size());
        for (MeltingRecipe.Byproduct byproduct : recipe.byproducts) {
            FluidStack.STREAM_CODEC.encode(buffer, byproduct.result());
            buffer.writeUtf(byproduct.rate());
        }
    }

    /** 应用矿物增产倍率；铸造炉保留完整九粒或四片结果。 */
    public FluidStack output(boolean foundry) {
        int base = rate.equals("gem") ? (foundry ? 4 : 4) : (foundry ? 9 : 8);
        int amount = Math.max(1, result.getAmount() * base / (rate.equals("gem") ? 4 : 9));
        return result.copyWithAmount(amount);
    }

    /** 计算副产物倍率。 */
    public List<FluidStack> byproductOutputs() {
        return byproducts.stream().map(byproduct -> byproduct.result().copy()).toList();
    }

    /** 匹配矿石输入。 */
    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return ingredient.test(input.item());
    }

    /** 熔炼配方没有物品结果。 */
    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    /** 熔炼配方只需要一个输入格。 */
    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    /** 配方浏览器不使用物品结果。 */
    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    /** 矿石熔炼由冶炼界面和配方浏览器展示。 */
    @Override
    public boolean isSpecial() {
        return true;
    }

    /** 返回矿石熔炼序列化器。 */
    @Override
    public RecipeSerializer<?> getSerializer() {
        return TFRecipes.ORE_MELTING_SERIALIZER.get();
    }

    /** 返回矿石熔炼类型。 */
    @Override
    public RecipeType<?> getType() {
        return TFRecipes.ORE_MELTING.get();
    }

    /** 返回输入列表。 */
    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, ingredient);
    }
}
