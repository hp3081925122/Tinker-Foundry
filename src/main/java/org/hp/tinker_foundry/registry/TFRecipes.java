package org.hp.tinker_foundry.registry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.RegistryObject;
import org.hp.tinker_foundry.TinkerFoundry;
import org.hp.tinker_foundry.recipe.AlloyingRecipe;
import org.hp.tinker_foundry.recipe.CastingRecipe;
import org.hp.tinker_foundry.recipe.FoundryRecipeSerializer;
import org.hp.tinker_foundry.recipe.FuelRecipe;
import org.hp.tinker_foundry.recipe.MeltingRecipe;
import org.hp.tinker_foundry.recipe.MoldingRecipe;
import org.hp.tinker_foundry.recipe.OreMeltingRecipe;
import org.hp.tinker_foundry.recipe.DamageableMeltingRecipe;

/** 八类公开自定义配方 API 的类型和序列化器注册表。 */
public final class TFRecipes {
    /** 熔炼配方类型。 */
    public static final RegistryObject<RecipeType<MeltingRecipe>> MELTING = type("melting");
    /** 矿石增产熔炼配方类型。 */
    public static final RegistryObject<RecipeType<OreMeltingRecipe>> ORE_MELTING = type("ore_melting");
    /** 耐久物品熔炼配方类型。 */
    public static final RegistryObject<RecipeType<DamageableMeltingRecipe>> DAMAGEABLE_MELTING = type("damageable_melting");
    /** 实体熔炼使用独立配方类型，支持数据包重载。 */
    public static final RegistryObject<RecipeType<org.hp.tinker_foundry.recipe.EntityMeltingRecipe>> ENTITY_MELTING = type("entity_melting");
    public static final RegistryObject<RecipeSerializer<org.hp.tinker_foundry.recipe.EntityMeltingRecipe>> ENTITY_MELTING_SERIALIZER =
        TinkerFoundry.RECIPE_SERIALIZERS.register("entity_melting", () -> new FoundryRecipeSerializer<>(
            org.hp.tinker_foundry.recipe.EntityMeltingRecipe::fromJson, org.hp.tinker_foundry.recipe.EntityMeltingRecipe::fromNetwork,
            org.hp.tinker_foundry.recipe.EntityMeltingRecipe::toNetwork));
    /** 合金配方类型。 */
    public static final RegistryObject<RecipeType<AlloyingRecipe>> ALLOYING = type("alloying");
    /** 浇注配方类型。 */
    public static final RegistryObject<RecipeType<CastingRecipe>> CASTING = type("casting");
    /** 模具配方类型。 */
    public static final RegistryObject<RecipeType<MoldingRecipe>> MOLDING = type("molding");
    /** 燃料配方类型。 */
    public static final RegistryObject<RecipeType<FuelRecipe>> FUEL = type("fuel");

    /** 熔炼配方序列化器。 */
    public static final RegistryObject<RecipeSerializer<MeltingRecipe>> MELTING_SERIALIZER = TinkerFoundry.RECIPE_SERIALIZERS.register("melting", () -> new FoundryRecipeSerializer<>(MeltingRecipe::fromJson, MeltingRecipe::fromNetwork, MeltingRecipe::toNetwork));
    /** 矿石增产熔炼序列化器。 */
    public static final RegistryObject<RecipeSerializer<OreMeltingRecipe>> ORE_MELTING_SERIALIZER = TinkerFoundry.RECIPE_SERIALIZERS.register("ore_melting", () -> new FoundryRecipeSerializer<>(OreMeltingRecipe::fromJson, OreMeltingRecipe::fromNetwork, OreMeltingRecipe::toNetwork));
    /** 耐久物品熔炼序列化器。 */
    public static final RegistryObject<RecipeSerializer<DamageableMeltingRecipe>> DAMAGEABLE_MELTING_SERIALIZER = TinkerFoundry.RECIPE_SERIALIZERS.register("damageable_melting", () -> new FoundryRecipeSerializer<>(DamageableMeltingRecipe::fromJson, DamageableMeltingRecipe::fromNetwork, DamageableMeltingRecipe::toNetwork));
    /** 合金配方序列化器。 */
    public static final RegistryObject<RecipeSerializer<AlloyingRecipe>> ALLOYING_SERIALIZER = TinkerFoundry.RECIPE_SERIALIZERS.register("alloying", () -> new FoundryRecipeSerializer<>(AlloyingRecipe::fromJson, AlloyingRecipe::fromNetwork, AlloyingRecipe::toNetwork));
    /** 浇注配方序列化器。 */
    public static final RegistryObject<RecipeSerializer<CastingRecipe>> CASTING_SERIALIZER = TinkerFoundry.RECIPE_SERIALIZERS.register("casting", () -> new FoundryRecipeSerializer<>(CastingRecipe::fromJson, CastingRecipe::fromNetwork, CastingRecipe::toNetwork));
    /** 模具配方序列化器。 */
    public static final RegistryObject<RecipeSerializer<MoldingRecipe>> MOLDING_SERIALIZER = TinkerFoundry.RECIPE_SERIALIZERS.register("molding", () -> new FoundryRecipeSerializer<>(MoldingRecipe::fromJson, MoldingRecipe::fromNetwork, MoldingRecipe::toNetwork));
    /** 燃料配方序列化器。 */
    public static final RegistryObject<RecipeSerializer<FuelRecipe>> FUEL_SERIALIZER = TinkerFoundry.RECIPE_SERIALIZERS.register("fuel", () -> new FoundryRecipeSerializer<>(FuelRecipe::fromJson, FuelRecipe::fromNetwork, FuelRecipe::toNetwork));

    /** 创建带命名空间的配方类型。 */
    private static <T extends net.minecraft.world.item.crafting.Recipe<?>> RegistryObject<RecipeType<T>> type(String name) {
        return TinkerFoundry.RECIPE_TYPES.register(name, () -> RecipeType.simple(new ResourceLocation(TinkerFoundry.MOD_ID, name)));
    }

    private TFRecipes() {
    }
}
