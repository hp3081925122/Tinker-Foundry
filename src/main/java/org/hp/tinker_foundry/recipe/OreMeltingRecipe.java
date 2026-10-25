package org.hp.tinker_foundry.recipe;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.NonNullList;
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

/** 支持矿石增产倍率的熔炼配方。 */
public record OreMeltingRecipe(ResourceLocation id, Ingredient ingredient, FluidStack result, int temperature, int time, String rate,
                               List<MeltingRecipe.Byproduct> byproducts) implements Recipe<Container> {
    /** 返回 Forge 1.20.1 Recipe 接口要求的配方 ID。 */
    @Override
    public ResourceLocation getId() {
        return id;
    }

    /** 兼容只提供主产物的矿石配方。 */
    public OreMeltingRecipe(Ingredient ingredient, FluidStack result, int temperature, int time, String rate) {
        this(runtimeId(), ingredient, result, temperature, time, rate, List.of());
    }

    /** 从数据包读取矿石熔炼 JSON。 */
    public static OreMeltingRecipe fromJson(ResourceLocation id, JsonObject json) {
        return new OreMeltingRecipe(id, Ingredient.fromJson(json.get("ingredient")), FoundryRecipeData.fluid(json.getAsJsonObject("result")),
            json.get("temperature").getAsInt(), json.has("time") ? json.get("time").getAsInt() : 100,
            json.has("rate") ? json.get("rate").getAsString() : "metal", FoundryRecipeData.byproducts(json));
    }

    /** 从网络读取矿石熔炼配方。 */
    public static OreMeltingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
        Ingredient ingredient = Ingredient.fromNetwork(buffer);
        FluidStack result = FoundryRecipeData.fluid(buffer);
        int temperature = buffer.readVarInt();
        int time = buffer.readVarInt();
        String rate = buffer.readUtf(256);
        int count = buffer.readVarInt();
        if (count < 0 || count > 128) throw new IllegalArgumentException("Invalid ore byproduct count: " + count);
        List<MeltingRecipe.Byproduct> byproducts = new ArrayList<>(count);
        for (int index = 0; index < count; index++) byproducts.add(new MeltingRecipe.Byproduct(FoundryRecipeData.fluid(buffer), buffer.readUtf(256)));
        return new OreMeltingRecipe(id, ingredient, result, temperature, time, rate, byproducts);
    }

    /** 向网络写入矿石熔炼配方。 */
    public static void toNetwork(FriendlyByteBuf buffer, OreMeltingRecipe recipe) {
        recipe.ingredient.toNetwork(buffer);
        FoundryRecipeData.fluid(buffer, recipe.result);
        buffer.writeVarInt(recipe.temperature);
        buffer.writeVarInt(recipe.time);
        buffer.writeUtf(recipe.rate, 256);
        FoundryRecipeData.byproducts(buffer, recipe.byproducts);
    }

    /** 应用矿物增产倍率。 */
    public FluidStack output(boolean foundry) {
        return boosted(result, rate, foundry ? 9 : 12, foundry ? 4 : 8);
    }

    /** 计算副产物倍率。 */
    public List<FluidStack> byproductOutputs() {
        return byproducts.stream().map(value -> boosted(value.result(), value.rate().equals("default") ? rate : value.rate(), 3, 4)).toList();
    }

    private static FluidStack boosted(FluidStack stack, String rate, int metal, int gem) {
        int amount = switch (rate) {
            case "metal" -> (int) Math.min(Integer.MAX_VALUE, (long) stack.getAmount() * metal / 9);
            case "gem" -> (int) Math.min(Integer.MAX_VALUE, (long) stack.getAmount() * gem / 4);
            default -> stack.getAmount();
        };
        return amount > 0 ? new FluidStack(stack, amount) : FluidStack.EMPTY;
    }

    /** 匹配矿石输入。 */
    @Override
    public boolean matches(Container input, Level level) {
        return input.getContainerSize() > 0 && ingredient.test(input.getItem(0));
    }

    /** 熔炼配方没有物品结果。 */
    @Override
    public ItemStack assemble(Container input) {
        return ItemStack.EMPTY;
    }

    /** 熔炼配方只需要一个输入格。 */
    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    /** 配方浏览器不使用物品结果。 */
    @Override
    public ItemStack getResultItem() {
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

    /** 旧构造使用的稳定内部 ID。 */
    private static ResourceLocation runtimeId() {
        return new ResourceLocation(TinkerFoundry.MOD_ID, "ore_melting/runtime");
    }
}
