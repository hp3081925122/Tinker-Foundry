package org.hp.tinker_foundry.recipe;

import java.util.Optional;
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

/** 固体或流体燃料定义，统一描述持续时间、温度和消耗速率。 */
public record FuelRecipe(ResourceLocation id, Optional<Ingredient> itemFuel, Optional<FoundryFluidIngredient> fluidFuel,
                         int duration, int temperature, int consumption, int rate) implements Recipe<Container> {
    /** 返回 Forge 1.20.1 Recipe 接口要求的配方 ID。 */
    @Override
    public ResourceLocation getId() {
        return id;
    }

    /** 从数据包读取固体或流体燃料。 */
    public static FuelRecipe fromJson(ResourceLocation id, JsonObject json) {
        Optional<Ingredient> item = json.has("item") ? Optional.of(Ingredient.fromJson(json.get("item"))) : Optional.empty();
        Optional<FoundryFluidIngredient> fluid = json.has("fluid") ? Optional.of(FoundryFluidIngredient.fromJson(json.get("fluid"))) : Optional.empty();
        return new FuelRecipe(id, item, fluid, json.get("duration").getAsInt(), json.get("temperature").getAsInt(),
            json.has("consumption") ? json.get("consumption").getAsInt() : 1, json.has("rate") ? json.get("rate").getAsInt() : 10);
    }

    /** 从网络读取燃料配方。 */
    public static FuelRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
        Optional<Ingredient> item = buffer.readBoolean() ? Optional.of(Ingredient.fromNetwork(buffer)) : Optional.empty();
        Optional<FoundryFluidIngredient> fluid = buffer.readBoolean() ? Optional.of(FoundryFluidIngredient.fromNetwork(buffer)) : Optional.empty();
        return new FuelRecipe(id, item, fluid, buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
    }

    /** 向网络写入燃料配方。 */
    public static void toNetwork(FriendlyByteBuf buffer, FuelRecipe recipe) {
        buffer.writeBoolean(recipe.itemFuel.isPresent());
        recipe.itemFuel.ifPresent(value -> value.toNetwork(buffer));
        buffer.writeBoolean(recipe.fluidFuel.isPresent());
        recipe.fluidFuel.ifPresent(value -> value.toNetwork(buffer));
        buffer.writeVarInt(recipe.duration);
        buffer.writeVarInt(recipe.temperature);
        buffer.writeVarInt(recipe.consumption);
        buffer.writeVarInt(recipe.rate);
    }

    /** 保留旧构造签名。 */
    public FuelRecipe(Optional<Ingredient> itemFuel, Optional<FoundryFluidIngredient> fluidFuel, int duration, int temperature, int consumption, int rate) {
        this(runtimeId(), itemFuel, fluidFuel, duration, temperature, consumption, rate);
    }

    /** 匹配固体燃料。 */
    @Override
    public boolean matches(Container input, Level level) {
        return input.getContainerSize() > 0 && itemFuel.map(value -> value.test(input.getItem(0))).orElse(false);
    }

    /** 匹配流体燃料。 */
    public boolean matchesFluid(FluidStack input) {
        return fluidFuel.map(value -> value.test(input)).orElse(false);
    }

    /** 燃料配方不产生物品。 */
    @Override
    public ItemStack assemble(Container input, RegistryAccess registries) {
        return ItemStack.EMPTY;
    }

    /** 燃料配方占用一个逻辑工作单元。 */
    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    /** 返回空物品结果。 */
    @Override
    public ItemStack getResultItem(RegistryAccess registries) {
        return ItemStack.EMPTY;
    }

    /** 燃料配方不进入原版配方书。 */
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

    /** 旧构造使用的稳定内部 ID。 */
    private static ResourceLocation runtimeId() {
        return new ResourceLocation(TinkerFoundry.MOD_ID, "fuel/runtime");
    }
}
