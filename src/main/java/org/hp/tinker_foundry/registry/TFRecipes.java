package org.hp.tinker_foundry.registry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.hp.tinker_foundry.TinkerFoundry;
import org.hp.tinker_foundry.recipe.AlloyingRecipe;
import org.hp.tinker_foundry.recipe.CastingRecipe;
import org.hp.tinker_foundry.recipe.FoundryRecipeSerializer;
import org.hp.tinker_foundry.recipe.FuelRecipe;
import org.hp.tinker_foundry.recipe.MeltingRecipe;
import org.hp.tinker_foundry.recipe.MoldingRecipe;

/** 五类公开配方 API 的类型和序列化器注册表。 */
public final class TFRecipes {
    /** 熔炼配方类型。 */
    public static final DeferredHolder<RecipeType<?>, RecipeType<MeltingRecipe>> MELTING = type("melting");
    /** 实体熔炼使用独立配方类型，支持数据包重载。 */
    public static final DeferredHolder<RecipeType<?>, RecipeType<org.hp.tinker_foundry.recipe.EntityMeltingRecipe>> ENTITY_MELTING = type("entity_melting");
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<org.hp.tinker_foundry.recipe.EntityMeltingRecipe>> ENTITY_MELTING_SERIALIZER =
        TinkerFoundry.RECIPE_SERIALIZERS.register("entity_melting", () -> new FoundryRecipeSerializer<>(
            org.hp.tinker_foundry.recipe.EntityMeltingRecipe.CODEC, org.hp.tinker_foundry.recipe.EntityMeltingRecipe.STREAM_CODEC));
    /** 合金配方类型。 */
    public static final DeferredHolder<RecipeType<?>, RecipeType<AlloyingRecipe>> ALLOYING = type("alloying");
    /** 浇注配方类型。 */
    public static final DeferredHolder<RecipeType<?>, RecipeType<CastingRecipe>> CASTING = type("casting");
    /** 模具配方类型。 */
    public static final DeferredHolder<RecipeType<?>, RecipeType<MoldingRecipe>> MOLDING = type("molding");
    /** 燃料配方类型。 */
    public static final DeferredHolder<RecipeType<?>, RecipeType<FuelRecipe>> FUEL = type("fuel");

    /** 熔炼配方序列化器。 */
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MeltingRecipe>> MELTING_SERIALIZER = TinkerFoundry.RECIPE_SERIALIZERS.register("melting", () -> new FoundryRecipeSerializer<>(MeltingRecipe.CODEC, MeltingRecipe.STREAM_CODEC));
    /** 合金配方序列化器。 */
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AlloyingRecipe>> ALLOYING_SERIALIZER = TinkerFoundry.RECIPE_SERIALIZERS.register("alloying", () -> new FoundryRecipeSerializer<>(AlloyingRecipe.CODEC, AlloyingRecipe.STREAM_CODEC));
    /** 浇注配方序列化器。 */
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CastingRecipe>> CASTING_SERIALIZER = TinkerFoundry.RECIPE_SERIALIZERS.register("casting", () -> new FoundryRecipeSerializer<>(CastingRecipe.CODEC, CastingRecipe.STREAM_CODEC));
    /** 模具配方序列化器。 */
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MoldingRecipe>> MOLDING_SERIALIZER = TinkerFoundry.RECIPE_SERIALIZERS.register("molding", () -> new FoundryRecipeSerializer<>(MoldingRecipe.CODEC, MoldingRecipe.STREAM_CODEC));
    /** 燃料配方序列化器。 */
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FuelRecipe>> FUEL_SERIALIZER = TinkerFoundry.RECIPE_SERIALIZERS.register("fuel", () -> new FoundryRecipeSerializer<>(FuelRecipe.CODEC, FuelRecipe.STREAM_CODEC));

    /** 创建带命名空间的配方类型。 */
    private static <T extends net.minecraft.world.item.crafting.Recipe<?>> DeferredHolder<RecipeType<?>, RecipeType<T>> type(String name) {
        return TinkerFoundry.RECIPE_TYPES.register(name, () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(TinkerFoundry.MOD_ID, name)));
    }

    private TFRecipes() {
    }
}
