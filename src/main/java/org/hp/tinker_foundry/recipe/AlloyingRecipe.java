package org.hp.tinker_foundry.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import org.hp.tinker_foundry.registry.TFRecipes;

/** 两种或多种流体按配方消耗后生成合金流体。 */
public record AlloyingRecipe(List<AlloyIngredient> ingredients, FluidStack result, int temperature) implements Recipe<FluidRecipeInput> {
    /** 支持流体精确注册名、流体标签，以及不消耗的催化流体输入。 */
    public static final MapCodec<AlloyingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        AlloyIngredient.CODEC.codec().listOf().fieldOf("ingredients").forGetter(AlloyingRecipe::ingredients),
        FluidStack.CODEC.fieldOf("result").forGetter(AlloyingRecipe::result),
        Codec.INT.fieldOf("temperature").forGetter(AlloyingRecipe::temperature)
    ).apply(instance, AlloyingRecipe::new));

    /** 客户端同步配方内容。 */
    public static final StreamCodec<RegistryFriendlyByteBuf, AlloyingRecipe> STREAM_CODEC = StreamCodec.of(AlloyingRecipe::writeNetwork, AlloyingRecipe::readNetwork);

    /** 从网络读取合金配方。 */
    private static AlloyingRecipe readNetwork(RegistryFriendlyByteBuf buffer) {
        List<AlloyIngredient> inputs = new ArrayList<>();
        int size = buffer.readVarInt();
        for (int index = 0; index < size; index++) {
            inputs.add(AlloyIngredient.STREAM_CODEC.decode(buffer));
        }
        FluidStack result = FluidStack.STREAM_CODEC.decode(buffer);
        int temperature = buffer.readVarInt();
        return new AlloyingRecipe(inputs, result, temperature);
    }

    /** 向网络写入合金配方。 */
    private static void writeNetwork(RegistryFriendlyByteBuf buffer, AlloyingRecipe recipe) {
        buffer.writeVarInt(recipe.ingredients.size());
        recipe.ingredients.forEach(input -> AlloyIngredient.STREAM_CODEC.encode(buffer, input));
        FluidStack.STREAM_CODEC.encode(buffer, recipe.result);
        buffer.writeVarInt(recipe.temperature);
    }

    /** 以回溯匹配避免标签输入发生贪心误配。 */
    @Override
    public boolean matches(FluidRecipeInput input, Level level) {
        if (ingredients.size() < 2 || input.fluids().size() < ingredients.size()) {
            return false;
        }
        return matchRemaining(ingredients, input.fluids(), 0, new boolean[input.fluids().size()]);
    }

    /** 执行流体输入与配方输入的一对一匹配。 */
    private static boolean matchRemaining(List<AlloyIngredient> required, List<FluidStack> available, int index, boolean[] used) {
        if (index == required.size()) {
            return true;
        }
        for (int fluidIndex = 0; fluidIndex < available.size(); fluidIndex++) {
            AlloyIngredient ingredient = required.get(index);
            FluidStack stack = available.get(fluidIndex);
            if (!used[fluidIndex] && stack.getAmount() >= ingredient.ingredient().amount() && ingredient.ingredient().test(stack)) {
                used[fluidIndex] = true;
                if (matchRemaining(required, available, index + 1, used)) {
                    return true;
                }
                used[fluidIndex] = false;
            }
        }
        return false;
    }

    /** 合金配方没有物品输出。 */
    @Override
    public ItemStack assemble(FluidRecipeInput input, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    /** 流体配方占用一个逻辑工作单元。 */
    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    /** 返回空物品结果。 */
    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    /** 合金配方不进入原版配方书，交由冶炼界面或 JEI 展示。 */
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

    /** 合金配方中的一个流体输入，催化输入匹配但不在成功后扣除。 */
    public record AlloyIngredient(SizedFluidIngredient ingredient, boolean catalyst) {
        /** 合金输入 JSON，催化标记缺省为 false，保持普通配方简洁。 */
        public static final MapCodec<AlloyIngredient> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            net.neoforged.neoforge.fluids.crafting.FluidIngredient.CODEC_NON_EMPTY.fieldOf("ingredient")
                .forGetter(input -> input.ingredient().ingredient()),
            Codec.INT.fieldOf("amount").forGetter(input -> input.ingredient().amount()),
            Codec.BOOL.optionalFieldOf("catalyst", false).forGetter(AlloyIngredient::catalyst)
        ).apply(instance, (fluid, amount, catalyst) -> new AlloyIngredient(new SizedFluidIngredient(fluid, amount), catalyst)));

        /** 客户端同步合金输入及催化标记。 */
        public static final StreamCodec<RegistryFriendlyByteBuf, AlloyIngredient> STREAM_CODEC = StreamCodec.of(
            AlloyIngredient::writeNetwork, AlloyIngredient::readNetwork
        );

        /** 从网络读取一个合金输入。 */
        private static AlloyIngredient readNetwork(RegistryFriendlyByteBuf buffer) {
            return new AlloyIngredient(SizedFluidIngredient.STREAM_CODEC.decode(buffer), buffer.readBoolean());
        }

        /** 向网络写入一个合金输入。 */
        private static void writeNetwork(RegistryFriendlyByteBuf buffer, AlloyIngredient input) {
            SizedFluidIngredient.STREAM_CODEC.encode(buffer, input.ingredient);
            buffer.writeBoolean(input.catalyst);
        }
    }
}
