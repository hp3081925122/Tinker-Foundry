package org.hp.tinker_foundry.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;
import org.hp.tinker_foundry.registry.TFRecipes;

/** 固体或流体燃料定义，统一描述持续时间、温度和消耗速率。 */
public record FuelRecipe(Optional<Ingredient> itemFuel, Optional<FluidIngredient> fluidFuel, int duration, int temperature, int consumption, int rate) implements Recipe<net.minecraft.world.item.crafting.SingleRecipeInput> {
    /** 燃料输入可以是物品标签或流体标签。 */
    public static final MapCodec<FuelRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Ingredient.CODEC.optionalFieldOf("item").forGetter(FuelRecipe::itemFuel),
        FluidIngredient.CODEC.optionalFieldOf("fluid").forGetter(FuelRecipe::fluidFuel),
        Codec.INT.fieldOf("duration").forGetter(FuelRecipe::duration),
        Codec.INT.fieldOf("temperature").forGetter(FuelRecipe::temperature),
        Codec.INT.fieldOf("consumption").orElse(1).forGetter(FuelRecipe::consumption),
        Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("rate", 10).forGetter(FuelRecipe::rate)
    ).apply(instance, FuelRecipe::new));

    /** 客户端同步配方内容。 */
    public static final StreamCodec<RegistryFriendlyByteBuf, FuelRecipe> STREAM_CODEC = StreamCodec.of(FuelRecipe::writeNetwork, FuelRecipe::readNetwork);

    /** 从网络读取燃料配方。 */
    private static FuelRecipe readNetwork(RegistryFriendlyByteBuf buffer) {
        return new FuelRecipe(
            ByteBufCodecs.optional(Ingredient.CONTENTS_STREAM_CODEC).decode(buffer),
            ByteBufCodecs.optional(FluidIngredient.STREAM_CODEC).decode(buffer),
            buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt()
        );
    }

    /** 向网络写入燃料配方。 */
    private static void writeNetwork(RegistryFriendlyByteBuf buffer, FuelRecipe recipe) {
        ByteBufCodecs.optional(Ingredient.CONTENTS_STREAM_CODEC).encode(buffer, recipe.itemFuel);
        ByteBufCodecs.optional(FluidIngredient.STREAM_CODEC).encode(buffer, recipe.fluidFuel);
        buffer.writeVarInt(recipe.duration);
        buffer.writeVarInt(recipe.temperature);
        buffer.writeVarInt(recipe.consumption);
        buffer.writeVarInt(recipe.rate);
    }

    /** 匹配固体燃料。 */
    @Override
    public boolean matches(net.minecraft.world.item.crafting.SingleRecipeInput input, Level level) {
        return itemFuel.map(value -> value.test(input.item())).orElse(false);
    }

    /** 匹配流体燃料。 */
    public boolean matchesFluid(FluidStack input) {
        return fluidFuel.map(value -> value.test(input)).orElse(false);
    }

    /** 燃料配方不产生物品。 */
    @Override
    public ItemStack assemble(net.minecraft.world.item.crafting.SingleRecipeInput input, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    /** 燃料配方占用一个逻辑工作单元。 */
    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    /** 返回空物品结果。 */
    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    /** 燃料配方不进入原版配方书，交由设备逻辑或 JEI 展示。 */
    @Override
    public boolean isSpecial() {
        return true;
    }

    /** 返回燃料配方序列化器。 */
    @Override
    public RecipeSerializer<?> getSerializer() {
        return TFRecipes.FUEL_SERIALIZER.get();
    }

    /** 返回独立 fuel 配方类型。 */
    @Override
    public RecipeType<?> getType() {
        return TFRecipes.FUEL.get();
    }
}
