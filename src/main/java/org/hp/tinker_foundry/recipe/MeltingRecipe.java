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

/** 物品到熔融流体的熔炼配方。 */
public record MeltingRecipe(ResourceLocation id, Ingredient ingredient, FluidStack result, int temperature, int time,
                            String rate, List<Byproduct> byproducts) implements Recipe<Container> {
    /** 返回 Forge 1.20.1 Recipe 接口要求的配方 ID。 */
    @Override
    public ResourceLocation getId() {
        return id;
    }

    /** 副产物保留独立倍率字段，默认继承主产物倍率。 */
    public record Byproduct(FluidStack result, String rate) {
    }

    /** 保留旧构造签名，供静态测试和内部调用使用。 */
    public MeltingRecipe(Ingredient ingredient, FluidStack result, int temperature, int time) {
        this(runtimeId("melting"), ingredient, result, temperature, time, "none", List.of());
    }

    /** 保留旧构造签名。 */
    public MeltingRecipe(Ingredient ingredient, FluidStack result, int temperature, int time, String rate, List<Byproduct> byproducts) {
        this(runtimeId("melting"), ingredient, result, temperature, time, rate, byproducts);
    }

    /** 从数据包读取 Forge 1.20.1 JSON。 */
    public static MeltingRecipe fromJson(ResourceLocation id, JsonObject json) {
        return new MeltingRecipe(id, Ingredient.fromJson(json.get("ingredient")), FoundryRecipeData.fluid(json.getAsJsonObject("result")),
            json.get("temperature").getAsInt(), json.has("time") ? json.get("time").getAsInt() : 100,
            json.has("rate") ? json.get("rate").getAsString() : "none", FoundryRecipeData.byproducts(json));
    }

    /** 从 Forge FriendlyByteBuf 读取客户端同步配方。 */
    public static MeltingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
        Ingredient ingredient = Ingredient.fromNetwork(buffer);
        FluidStack result = FoundryRecipeData.fluid(buffer);
        int temperature = buffer.readVarInt();
        int time = buffer.readVarInt();
        String rate = buffer.readUtf(256);
        int count = buffer.readVarInt();
        if (count < 0 || count > 128) throw new IllegalArgumentException("Invalid byproduct count: " + count);
        List<Byproduct> byproducts = new ArrayList<>(count);
        for (int index = 0; index < count; index++) byproducts.add(new Byproduct(FoundryRecipeData.fluid(buffer), buffer.readUtf(256)));
        return new MeltingRecipe(id, ingredient, result, temperature, time, rate, byproducts);
    }

    /** 向 Forge FriendlyByteBuf 写入客户端同步配方。 */
    public static void toNetwork(FriendlyByteBuf buffer, MeltingRecipe recipe) {
        recipe.ingredient.toNetwork(buffer);
        FoundryRecipeData.fluid(buffer, recipe.result);
        buffer.writeVarInt(recipe.temperature);
        buffer.writeVarInt(recipe.time);
        buffer.writeUtf(recipe.rate, 256);
        FoundryRecipeData.byproducts(buffer, recipe.byproducts);
    }

    /** 使用匠魂原有金属粒和宝石碎片倍率。 */
    public FluidStack output(boolean foundry) {
        return boosted(result, rate, foundry ? 9 : 12, foundry ? 4 : 8);
    }

    /** 计算副产物输出。 */
    public List<FluidStack> byproductOutputs() {
        return byproducts.stream().map(value -> boosted(value.result(), value.rate().equals("default") ? rate : value.rate(), 3, 4)).toList();
    }

    /** 按倍率缩放流体并保留流体标签。 */
    private static FluidStack boosted(FluidStack stack, String rate, int metal, int gem) {
        int amount = switch (rate) {
            case "metal" -> (int) Math.min(Integer.MAX_VALUE, (long) stack.getAmount() * metal / 9);
            case "gem" -> (int) Math.min(Integer.MAX_VALUE, (long) stack.getAmount() * gem / 4);
            default -> stack.getAmount();
        };
        return amount > 0 ? new FluidStack(stack, amount) : FluidStack.EMPTY;
    }

    /** 检查单物品输入。 */
    @Override
    public boolean matches(Container input, Level level) {
        return input.getContainerSize() > 0 && ingredient.test(input.getItem(0));
    }

    /** 熔炼配方没有物品输出。 */
    @Override
    public ItemStack assemble(Container input) {
        return ItemStack.EMPTY;
    }

    /** 熔炼只占用一个逻辑槽位。 */
    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    /** 配方浏览器通过空物品结果识别流体输出。 */
    @Override
    public ItemStack getResultItem() {
        return ItemStack.EMPTY;
    }

    /** 熔炼配方不进入原版配方书。 */
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

    /** 旧构造方​​法使用的稳定非数据包 ID。 */
    private static ResourceLocation runtimeId(String path) {
        return new ResourceLocation(TinkerFoundry.MOD_ID, path + "/runtime");
    }
}
