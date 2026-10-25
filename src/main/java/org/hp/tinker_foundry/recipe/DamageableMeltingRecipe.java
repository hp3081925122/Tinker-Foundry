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

/** 按工具剩余耐久比例产出流体的熔炼配方。 */
public record DamageableMeltingRecipe(Ingredient ingredient, FluidStackTemplate resultTemplate, int temperature, int time, int unitSize,
                                      List<MeltingRecipe.Byproduct> byproducts) implements FoundryRecipe<SingleRecipeInput> {
    /** 保留运行时构造方式，并在进入配方记录时转换为未绑定组件模板。 */
    public DamageableMeltingRecipe(Ingredient ingredient, FluidStack result, int temperature, int time, int unitSize,
                                   List<MeltingRecipe.Byproduct> byproducts) {
        this(ingredient, FluidStackTemplate.fromNonEmptyStack(result), temperature, time, unitSize, byproducts);
    }

    /** 配方默认每一个单位至少产出一个单位流体。 */
    public DamageableMeltingRecipe(Ingredient ingredient, FluidStack result, int temperature, int time) {
        this(ingredient, result, temperature, time, 1, List.of());
    }

    /** 耐久物品熔炼配方编解码器。 */
    public static final MapCodec<DamageableMeltingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Ingredient.CODEC.fieldOf("ingredient").forGetter(DamageableMeltingRecipe::ingredient),
        FluidStackTemplate.CODEC.fieldOf("result").forGetter(DamageableMeltingRecipe::resultTemplate),
        Codec.INT.fieldOf("temperature").forGetter(DamageableMeltingRecipe::temperature),
        Codec.INT.fieldOf("time").orElse(100).forGetter(DamageableMeltingRecipe::time),
        Codec.INT.optionalFieldOf("unit_size", 1).forGetter(DamageableMeltingRecipe::unitSize),
        MeltingRecipe.Byproduct.CODEC.listOf().optionalFieldOf("byproducts", List.of()).forGetter(DamageableMeltingRecipe::byproducts)
    ).apply(instance, DamageableMeltingRecipe::new));

    /** 客户端同步耐久配方。 */
    public static final StreamCodec<RegistryFriendlyByteBuf, DamageableMeltingRecipe> STREAM_CODEC = StreamCodec.of(
        DamageableMeltingRecipe::writeNetwork, DamageableMeltingRecipe::readNetwork);

    /** 读取网络配方。 */
    private static DamageableMeltingRecipe readNetwork(RegistryFriendlyByteBuf buffer) {
        Ingredient ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
        FluidStackTemplate result = FluidStackTemplate.STREAM_CODEC.decode(buffer);
        int temperature = buffer.readVarInt();
        int time = buffer.readVarInt();
        int unitSize = buffer.readVarInt();
        int count = buffer.readVarInt();
        if (count < 0 || count > 128) throw new IllegalArgumentException("Invalid damageable byproduct count: " + count);
        java.util.ArrayList<MeltingRecipe.Byproduct> byproducts = new java.util.ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            byproducts.add(new MeltingRecipe.Byproduct(FluidStackTemplate.STREAM_CODEC.decode(buffer), buffer.readUtf()));
        }
        return new DamageableMeltingRecipe(ingredient, result, temperature, time, unitSize, byproducts);
    }

    /** 写入网络配方。 */
    private static void writeNetwork(RegistryFriendlyByteBuf buffer, DamageableMeltingRecipe recipe) {
        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.ingredient);
        FluidStackTemplate.STREAM_CODEC.encode(buffer, recipe.resultTemplate);
        buffer.writeVarInt(recipe.temperature);
        buffer.writeVarInt(recipe.time);
        buffer.writeVarInt(recipe.unitSize);
        buffer.writeVarInt(recipe.byproducts.size());
        for (MeltingRecipe.Byproduct byproduct : recipe.byproducts) {
            FluidStackTemplate.STREAM_CODEC.encode(buffer, byproduct.resultTemplate());
            buffer.writeUtf(byproduct.rate());
        }
    }

    /** 按未损坏耐久比例缩放产物，且至少保留一个单位。 */
    public FluidStack output(ItemStack input) {
        FluidStack result = result();
        int maxDamage = input.getMaxDamage();
        if (maxDamage <= 0) return result.copy();
        int amount = result.getAmount() * Math.max(0, maxDamage - input.getDamageValue()) / maxDamage;
        amount = amount <= unitSize ? Math.max(1, unitSize) : amount - amount % Math.max(1, unitSize);
        return result.copyWithAmount(Math.max(1, amount));
    }

    /** 在注册表已经绑定的运行时创建主产物流体栈。 */
    public FluidStack result() {
        return resultTemplate.create();
    }

    /** 耐久配方的副产物也按同一比例缩放。 */
    public List<FluidStack> byproductOutputs(ItemStack input) {
        int maxDamage = input.getMaxDamage();
        if (maxDamage <= 0) return byproducts.stream().map(value -> value.result().copy()).toList();
        int remaining = Math.max(0, maxDamage - input.getDamageValue());
        return byproducts.stream().map(value -> {
            int amount = value.result().getAmount() * remaining / maxDamage;
            return value.result().copyWithAmount(Math.max(1, amount));
        }).toList();
    }

    /** 匹配耐久物品。 */
    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return ingredient.test(input.item()) && input.item().getMaxDamage() > 0;
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

    /** 耐久熔炼由冶炼界面和配方浏览器展示。 */
    @Override
    public boolean isSpecial() {
        return true;
    }

    /** 返回耐久熔炼序列化器。 */
    @Override
    public RecipeSerializer<? extends DamageableMeltingRecipe> getSerializer() {
        return TFRecipes.DAMAGEABLE_MELTING_SERIALIZER.get();
    }

    /** 返回耐久熔炼类型。 */
    @Override
    public RecipeType<? extends DamageableMeltingRecipe> getType() {
        return TFRecipes.DAMAGEABLE_MELTING.get();
    }

    /** 返回输入列表。 */
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.copyOf(java.util.List.of(ingredient));
    }
}
