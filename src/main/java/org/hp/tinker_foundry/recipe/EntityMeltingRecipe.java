package org.hp.tinker_foundry.recipe;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import com.google.gson.JsonObject;
import org.hp.tinker_foundry.TinkerFoundry;
import org.hp.tinker_foundry.registry.TFRecipes;

/** 数据包实体熔炼配方，支持实体列表、实体标签、伤害和流体输出。 */
public record EntityMeltingRecipe(ResourceLocation id, List<String> entities, String entityTag, FluidStack result, int damage)
    implements Recipe<Container> {
    /** 返回 Forge 1.20.1 Recipe 接口要求的配方 ID。 */
    @Override
    public ResourceLocation getId() {
        return id;
    }

    /** 从数据包读取实体熔炼 JSON。 */
    public static EntityMeltingRecipe fromJson(ResourceLocation id, JsonObject json) {
        List<String> entities = new ArrayList<>();
        if (json.has("entities")) for (var value : json.getAsJsonArray("entities")) entities.add(value.getAsString());
        return new EntityMeltingRecipe(id, entities, json.has("entity_tag") ? json.get("entity_tag").getAsString() : "",
            FoundryRecipeData.fluid(json.getAsJsonObject("result")), json.has("damage") ? json.get("damage").getAsInt() : 2);
    }

    /** 从网络读取实体熔炼配方。 */
    public static EntityMeltingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        if (count < 0 || count > 4096) throw new IllegalArgumentException("Invalid entity ingredient size: " + count);
        List<String> entities = new ArrayList<>(count);
        for (int index = 0; index < count; index++) entities.add(buffer.readUtf(256));
        return new EntityMeltingRecipe(id, entities, buffer.readUtf(256), FoundryRecipeData.fluid(buffer), buffer.readVarInt());
    }

    /** 向网络写入实体条件、输出和伤害值。 */
    public static void toNetwork(FriendlyByteBuf buffer, EntityMeltingRecipe recipe) {
        buffer.writeVarInt(recipe.entities.size());
        recipe.entities.forEach(value -> buffer.writeUtf(value, 256));
        buffer.writeUtf(recipe.entityTag, 256);
        FoundryRecipeData.fluid(buffer, recipe.result);
        buffer.writeVarInt(recipe.damage);
    }

    /** 保留旧构造签名。 */
    public EntityMeltingRecipe(List<String> entities, String entityTag, FluidStack result, int damage) {
        this(runtimeId(), entities, entityTag, result, damage);
    }

    /** 直接匹配当前注册表的实体类型，标签可由数据包扩展。 */
    public boolean matchesEntity(EntityType<?> type) {
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (entityId != null && entities.contains(entityId.toString())) return true;
        ResourceLocation tagId = ResourceLocation.tryParse(entityTag);
        return !entityTag.isEmpty() && tagId != null && type.is(TagKey.create(Registries.ENTITY_TYPE, tagId));
    }

    /** 不接受普通物品配方匹配。 */
    @Override
    public boolean matches(Container input, Level level) {
        return false;
    }

    /** 实体配方没有物品产物。 */
    @Override
    public ItemStack assemble(Container input, RegistryAccess registries) {
        return ItemStack.EMPTY;
    }

    /** 不参与合成台配方。 */
    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return false;
    }

    /** 配方结果是流体而非物品。 */
    @Override
    public ItemStack getResultItem(RegistryAccess registries) {
        return ItemStack.EMPTY;
    }

    /** 禁止加入原版配方书。 */
    @Override
    public boolean isSpecial() {
        return true;
    }

    /** 返回独立序列化器。 */
    @Override
    public RecipeSerializer<?> getSerializer() {
        return TFRecipes.ENTITY_MELTING_SERIALIZER.get();
    }

    /** 返回实体熔炼配方类型。 */
    @Override
    public RecipeType<?> getType() {
        return TFRecipes.ENTITY_MELTING.get();
    }

    /** 旧构造使用的稳定内部 ID。 */
    private static ResourceLocation runtimeId() {
        return new ResourceLocation(TinkerFoundry.MOD_ID, "entity_melting/runtime");
    }
}
