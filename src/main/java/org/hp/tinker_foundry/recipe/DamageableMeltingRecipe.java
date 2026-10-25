package org.hp.tinker_foundry.recipe;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import com.google.gson.JsonObject;
import org.hp.tinker_foundry.TinkerFoundry;
import org.hp.tinker_foundry.registry.TFRecipes;

/** 按工具剩余耐久比例产出流体的熔炼配方。 */
public record DamageableMeltingRecipe(ResourceLocation id, Ingredient ingredient, FluidStack result, int temperature, int time, int unitSize,
                                      List<MeltingRecipe.Byproduct> byproducts) implements Recipe<Container> {
    /** 返回 Forge 1.20.1 Recipe 接口要求的配方 ID。 */
    @Override
    public ResourceLocation getId() {
        return id;
    }

    /** 配方默认每一个单位至少产出一个单位流体。 */
    public DamageableMeltingRecipe(Ingredient ingredient, FluidStack result, int temperature, int time) {
        this(runtimeId(), ingredient, result, temperature, time, 1, List.of());
    }

    /** 从数据包读取耐久熔炼 JSON。 */
    public static DamageableMeltingRecipe fromJson(ResourceLocation id, JsonObject json) {
        return new DamageableMeltingRecipe(id, Ingredient.fromJson(json.get("ingredient")), FoundryRecipeData.fluid(json.getAsJsonObject("result")),
            json.get("temperature").getAsInt(), json.has("time") ? json.get("time").getAsInt() : 100,
            json.has("unit_size") ? json.get("unit_size").getAsInt() : 1, FoundryRecipeData.byproducts(json));
    }

    /** 从网络读取耐久熔炼配方。 */
    public static DamageableMeltingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
        Ingredient ingredient = Ingredient.fromNetwork(buffer);
        FluidStack result = FoundryRecipeData.fluid(buffer);
        int temperature = buffer.readVarInt();
        int time = buffer.readVarInt();
        int unitSize = buffer.readVarInt();
        int count = buffer.readVarInt();
        if (count < 0 || count > 128) throw new IllegalArgumentException("Invalid damageable byproduct count: " + count);
        List<MeltingRecipe.Byproduct> byproducts = new ArrayList<>(count);
        for (int index = 0; index < count; index++) byproducts.add(new MeltingRecipe.Byproduct(FoundryRecipeData.fluid(buffer), buffer.readUtf(256)));
        return new DamageableMeltingRecipe(id, ingredient, result, temperature, time, unitSize, byproducts);
    }

    /** 向网络写入耐久熔炼配方。 */
    public static void toNetwork(FriendlyByteBuf buffer, DamageableMeltingRecipe recipe) {
        recipe.ingredient.toNetwork(buffer);
        FoundryRecipeData.fluid(buffer, recipe.result);
        buffer.writeVarInt(recipe.temperature);
        buffer.writeVarInt(recipe.time);
        buffer.writeVarInt(recipe.unitSize);
        FoundryRecipeData.byproducts(buffer, recipe.byproducts);
    }

    /** 按未损坏耐久比例缩放产物，且至少保留一个单位。 */
    public FluidStack output(ItemStack input) {
        int maxDamage = input.getMaxDamage();
        if (maxDamage <= 0) return result.copy();
        int amount = result.getAmount() * Math.max(0, maxDamage - input.getDamageValue()) / maxDamage;
        amount = amount <= unitSize ? Math.max(1, unitSize) : amount - amount % Math.max(1, unitSize);
        return new FluidStack(result, Math.max(1, amount));
    }

    /** 耐久配方的副产物也按同一比例缩放。 */
    public List<FluidStack> byproductOutputs(ItemStack input) {
        int maxDamage = input.getMaxDamage();
        if (maxDamage <= 0) return byproducts.stream().map(value -> value.result().copy()).toList();
        int remaining = Math.max(0, maxDamage - input.getDamageValue());
        return byproducts.stream().map(value -> new FluidStack(value.result(), Math.max(1, value.result().getAmount() * remaining / maxDamage))).toList();
    }

    /** 匹配耐久物品。 */
    @Override
    public boolean matches(Container input, Level level) {
        return input.getContainerSize() > 0 && ingredient.test(input.getItem(0)) && input.getItem(0).getMaxDamage() > 0;
    }

    /** 熔炼配方没有物品结果。 */
    @Override
    public ItemStack assemble(Container input, RegistryAccess registries) {
        return ItemStack.EMPTY;
    }

    /** 熔炼配方只需要一个输入格。 */
    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    /** 配方浏览器不使用物品结果。 */
    @Override
    public ItemStack getResultItem(RegistryAccess registries) {
        return ItemStack.EMPTY;
    }

    /** 耐久熔炼由冶炼界面和配方浏览器展示。 */
    @Override
    public boolean isSpecial() {
        return true;
    }

    /** 返回耐久熔炼序列化器。 */
    @Override
    public RecipeSerializer<?> getSerializer() {
        return TFRecipes.DAMAGEABLE_MELTING_SERIALIZER.get();
    }

    /** 返回耐久熔炼类型。 */
    @Override
    public RecipeType<?> getType() {
        return TFRecipes.DAMAGEABLE_MELTING.get();
    }

    /** 返回输入列表。 */
    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, ingredient);
    }

    /** 旧构造使用的稳定内部 ID。 */
    private static ResourceLocation runtimeId() {
        return new ResourceLocation(TinkerFoundry.MOD_ID, "damageable_melting/runtime");
    }
}
