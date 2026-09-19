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
public record MeltingRecipe(Ingredient ingredient, FluidStack result, int temperature, int time,
                            String rate, java.util.List<Byproduct> byproducts) implements Recipe<SingleRecipeInput> {
    /** 副产物的倍率默认继承主产物类型，允许数据包单独指定。 */
    public record Byproduct(FluidStack result, String rate) {
        public static final com.mojang.serialization.Codec<Byproduct> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FluidStack.CODEC.fieldOf("result").forGetter(Byproduct::result),
            Codec.STRING.optionalFieldOf("rate", "default").forGetter(Byproduct::rate)
        ).apply(instance, Byproduct::new));
    }

    /** 普通熔炼配方不使用矿物增产。 */
    public MeltingRecipe(Ingredient ingredient, FluidStack result, int temperature, int time) {
        this(ingredient, result, temperature, time, "none", java.util.List.of());
    }

    /** 使用上游默认金属粒和宝石碎片倍率，保持整数截断语义。 */
    public FluidStack output(boolean foundry) {
        return boosted(result, rate, foundry ? 9 : 12, foundry ? 4 : 8);
    }

    /** 铸造炉依次注入副产物，容量不足时剩余副产物不阻止主产物完成。 */
    public java.util.List<FluidStack> byproductOutputs() {
        return byproducts.stream().map(value -> boosted(value.result(),
            value.rate().equals("default") ? rate : value.rate(), 3, 4)).toList();
    }

    /** 金属倍率以九粒一锭、宝石倍率以四片一宝石为基数。 */
    private static FluidStack boosted(FluidStack stack, String rate, int metal, int gem) {
        int amount = switch (rate) {
            case "metal" -> (int) Math.min(Integer.MAX_VALUE, (long) stack.getAmount() * metal / 9);
            case "gem" -> (int) Math.min(Integer.MAX_VALUE, (long) stack.getAmount() * gem / 4);
            default -> stack.getAmount();
        };
        return amount > 0 ? stack.copyWithAmount(amount) : FluidStack.EMPTY;
    }
    /** 数据包配方编解码器，支持标签或精确物品输入。 */
    public static final MapCodec<MeltingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(MeltingRecipe::ingredient),
        FluidStack.CODEC.fieldOf("result").forGetter(MeltingRecipe::result),
        Codec.INT.fieldOf("temperature").forGetter(MeltingRecipe::temperature),
        Codec.INT.fieldOf("time").orElse(100).forGetter(MeltingRecipe::time),
        Codec.STRING.optionalFieldOf("rate", "none").forGetter(MeltingRecipe::rate),
        Byproduct.CODEC.listOf().optionalFieldOf("byproducts", java.util.List.of()).forGetter(MeltingRecipe::byproducts)
    ).apply(instance, MeltingRecipe::new));

    /** 客户端同步配方内容。 */
    public static final StreamCodec<RegistryFriendlyByteBuf, MeltingRecipe> STREAM_CODEC = StreamCodec.of(MeltingRecipe::writeNetwork, MeltingRecipe::readNetwork);

    /** 从网络读取熔炼配方。 */
    private static MeltingRecipe readNetwork(RegistryFriendlyByteBuf buffer) {
        Ingredient ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
        FluidStack result = FluidStack.STREAM_CODEC.decode(buffer);
        int temperature = buffer.readVarInt();
        int time = buffer.readVarInt();
        String rate = buffer.readUtf();
        int count = buffer.readVarInt();
        if (count < 0 || count > 128) throw new IllegalArgumentException("Invalid byproduct count: " + count);
        java.util.List<Byproduct> byproducts = new java.util.ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            byproducts.add(new Byproduct(FluidStack.STREAM_CODEC.decode(buffer), buffer.readUtf()));
        }
        return new MeltingRecipe(ingredient, result, temperature, time, rate, byproducts);
    }

    /** 向网络写入熔炼配方。 */
    private static void writeNetwork(RegistryFriendlyByteBuf buffer, MeltingRecipe recipe) {
        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.ingredient);
        FluidStack.STREAM_CODEC.encode(buffer, recipe.result);
        buffer.writeVarInt(recipe.temperature);
        buffer.writeVarInt(recipe.time);
        buffer.writeUtf(recipe.rate);
        buffer.writeVarInt(recipe.byproducts.size());
        for (Byproduct byproduct : recipe.byproducts) {
            FluidStack.STREAM_CODEC.encode(buffer, byproduct.result());
            buffer.writeUtf(byproduct.rate());
        }
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
