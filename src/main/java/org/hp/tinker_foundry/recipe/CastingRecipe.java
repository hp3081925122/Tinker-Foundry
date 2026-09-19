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
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import org.hp.tinker_foundry.registry.TFRecipes;

/** 流体经过可选模具冷却后产出物品。 */
public record CastingRecipe(SizedFluidIngredient fluid, Optional<Ingredient> mold, ItemStack result, int time,
                            boolean castConsumed, boolean switchSlots) implements Recipe<FluidRecipeInput> {
    /** 保留旧构造签名，默认使用可重复铸模且不切换槽位。 */
    public CastingRecipe(SizedFluidIngredient fluid, Optional<Ingredient> mold, ItemStack result, int time) {
        this(fluid, mold, result, time, false, false);
    }

    /** 浇注配方支持可选模具和冷却时间。 */
    public static final MapCodec<CastingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        SizedFluidIngredient.NESTED_CODEC.fieldOf("fluid").forGetter(CastingRecipe::fluid),
        Ingredient.CODEC.optionalFieldOf("mold").forGetter(CastingRecipe::mold),
        ItemStack.CODEC.fieldOf("result").forGetter(CastingRecipe::result),
        Codec.INT.fieldOf("time").orElse(60).forGetter(CastingRecipe::time),
        Codec.BOOL.optionalFieldOf("cast_consumed", false).forGetter(CastingRecipe::castConsumed),
        Codec.BOOL.optionalFieldOf("switch_slots", false).forGetter(CastingRecipe::switchSlots)
    ).apply(instance, CastingRecipe::new));

    /** 客户端同步配方内容。 */
    public static final StreamCodec<RegistryFriendlyByteBuf, CastingRecipe> STREAM_CODEC = StreamCodec.of(CastingRecipe::writeNetwork, CastingRecipe::readNetwork);

    /** 从网络读取浇注配方。 */
    private static CastingRecipe readNetwork(RegistryFriendlyByteBuf buffer) {
        return new CastingRecipe(
            SizedFluidIngredient.STREAM_CODEC.decode(buffer),
            ByteBufCodecs.optional(Ingredient.CONTENTS_STREAM_CODEC).decode(buffer),
            ItemStack.STREAM_CODEC.decode(buffer),
            buffer.readVarInt(),
            buffer.readBoolean(),
            buffer.readBoolean()
        );
    }

    /** 向网络写入浇注配方。 */
    private static void writeNetwork(RegistryFriendlyByteBuf buffer, CastingRecipe recipe) {
        SizedFluidIngredient.STREAM_CODEC.encode(buffer, recipe.fluid);
        ByteBufCodecs.optional(Ingredient.CONTENTS_STREAM_CODEC).encode(buffer, recipe.mold);
        ItemStack.STREAM_CODEC.encode(buffer, recipe.result);
        buffer.writeVarInt(recipe.time);
        buffer.writeBoolean(recipe.castConsumed);
        buffer.writeBoolean(recipe.switchSlots);
    }

    /** 校验流体和可选模具。 */
    @Override
    public boolean matches(FluidRecipeInput input, Level level) {
        // 有模具的配方必须使用对应物品，没有模具的配方只允许空物品槽，避免块浇注配方抢先匹配锭浇注。
        boolean moldMatches = mold.map(value -> !input.item().isEmpty() && value.test(input.item())).orElse(input.item().isEmpty());
        return !input.fluids().isEmpty() && input.fluids().get(0).getAmount() >= fluid.amount() && fluid.test(input.fluids().get(0)) && moldMatches;
    }

    /** 返回浇注物品副本。 */
    @Override
    public ItemStack assemble(FluidRecipeInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    /** 浇注使用一个工作单元。 */
    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    /** 返回浇注结果。 */
    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result.copy();
    }

    /** 浇注配方不进入原版配方书，交由冶炼界面或 JEI 展示。 */
    @Override
    public boolean isSpecial() {
        return true;
    }

    /** 返回浇注配方序列化器。 */
    @Override
    public RecipeSerializer<?> getSerializer() {
        return TFRecipes.CASTING_SERIALIZER.get();
    }

    /** 返回独立 casting 配方类型。 */
    @Override
    public RecipeType<?> getType() {
        return TFRecipes.CASTING.get();
    }
}
