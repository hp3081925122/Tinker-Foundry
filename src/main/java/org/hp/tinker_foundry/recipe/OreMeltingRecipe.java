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
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidStackTemplate;
import org.hp.tinker_foundry.registry.TFRecipes;

/** 支持矿石增产倍率的熔炼配方。 */
public record OreMeltingRecipe(Ingredient ingredient, FluidStackTemplate resultTemplate, int temperature, int time, String rate,
                               List<MeltingRecipe.Byproduct> byproducts) implements FoundryRecipe<SingleRecipeInput> {
    /** 保留运行时构造方式，并在进入配方记录时转换为未绑定组件模板。 */
    public OreMeltingRecipe(Ingredient ingredient, FluidStack result, int temperature, int time, String rate,
                            List<MeltingRecipe.Byproduct> byproducts) {
        this(ingredient, FluidStackTemplate.fromNonEmptyStack(result), temperature, time, rate, byproducts);
    }

    /** 兼容只提供主产物的矿石配方。 */
    public OreMeltingRecipe(Ingredient ingredient, FluidStack result, int temperature, int time, String rate) {
        this(ingredient, result, temperature, time, rate, List.of());
    }

    /** 矿石配方的 JSON 编解码器。 */
    public static final MapCodec<OreMeltingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Ingredient.CODEC.fieldOf("ingredient").forGetter(OreMeltingRecipe::ingredient),
        FluidStackTemplate.CODEC.fieldOf("result").forGetter(OreMeltingRecipe::resultTemplate),
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
        FluidStackTemplate result = FluidStackTemplate.STREAM_CODEC.decode(buffer);
        int temperature = buffer.readVarInt();
        int time = buffer.readVarInt();
        String rate = buffer.readUtf();
        int count = buffer.readVarInt();
        if (count < 0 || count > 128) throw new IllegalArgumentException("Invalid ore byproduct count: " + count);
        java.util.ArrayList<MeltingRecipe.Byproduct> byproducts = new java.util.ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            byproducts.add(new MeltingRecipe.Byproduct(FluidStackTemplate.STREAM_CODEC.decode(buffer), buffer.readUtf()));
        }
        return new OreMeltingRecipe(ingredient, result, temperature, time, rate, byproducts);
    }

    /** 写入网络配方。 */
    private static void writeNetwork(RegistryFriendlyByteBuf buffer, OreMeltingRecipe recipe) {
        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.ingredient);
        FluidStackTemplate.STREAM_CODEC.encode(buffer, recipe.resultTemplate);
        buffer.writeVarInt(recipe.temperature);
        buffer.writeVarInt(recipe.time);
        buffer.writeUtf(recipe.rate);
        buffer.writeVarInt(recipe.byproducts.size());
        for (MeltingRecipe.Byproduct byproduct : recipe.byproducts) {
            FluidStackTemplate.STREAM_CODEC.encode(buffer, byproduct.resultTemplate());
            buffer.writeUtf(byproduct.rate());
        }
    }

    /** 应用矿物增产倍率；铸造炉保留完整九粒或四片结果。 */
    public FluidStack output(boolean foundry) {
        FluidStack result = result();
        return boosted(result, rate, foundry ? 9 : 12, foundry ? 4 : 8);
    }

    /** 在注册表已经绑定的运行时创建主产物流体栈。 */
    public FluidStack result() {
        return resultTemplate.create();
    }

    /** 计算副产物倍率。 */
    public List<FluidStack> byproductOutputs() {
        return byproducts.stream().map(byproduct -> boosted(byproduct.result(),
            byproduct.rate().equals("default") ? rate : byproduct.rate(), 3, 4)).toList();
    }

    private static FluidStack boosted(FluidStack stack, String rate, int metal, int gem) {
        int amount = switch (rate) {
            case "metal" -> (int) Math.min(Integer.MAX_VALUE, (long) stack.getAmount() * metal / 9);
            case "gem" -> (int) Math.min(Integer.MAX_VALUE, (long) stack.getAmount() * gem / 4);
            default -> stack.getAmount();
        };
        return amount > 0 ? stack.copyWithAmount(amount) : FluidStack.EMPTY;
    }

    /** 匹配矿石输入。 */
    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return ingredient.test(input.item());
    }

    /** 熔炼配方没有物品结果。 */
    @Override
    public ItemStack assemble(SingleRecipeInput input) {
        return ItemStack.EMPTY;
    }

    /** 熔炼配方只需要一个输入格。 */
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    /** 配方浏览器不使用物品结果。 */
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
    public RecipeSerializer<? extends OreMeltingRecipe> getSerializer() {
        return TFRecipes.ORE_MELTING_SERIALIZER.get();
    }

    /** 返回矿石熔炼类型。 */
    @Override
    public RecipeType<? extends OreMeltingRecipe> getType() {
        return TFRecipes.ORE_MELTING.get();
    }

    /** 返回输入列表。 */
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.copyOf(java.util.List.of(ingredient));
    }
}
