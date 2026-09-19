package org.hp.tinker_foundry.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import org.hp.tinker_foundry.registry.TFRecipes;

/** 数据包实体熔炼配方，支持实体列表、实体标签、伤害和带组件的流体输出。 */
public record EntityMeltingRecipe(List<String> entities, String entityTag, FluidStack result, int damage) implements Recipe<SingleRecipeInput> {
    /** 实体匹配和普通物品配方分离，避免错误进入原版物品配方匹配。 */
    public static final MapCodec<EntityMeltingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.STRING.listOf().optionalFieldOf("entities", List.of()).forGetter(EntityMeltingRecipe::entities),
        Codec.STRING.optionalFieldOf("entity_tag", "").forGetter(EntityMeltingRecipe::entityTag),
        FluidStack.CODEC.fieldOf("result").forGetter(EntityMeltingRecipe::result),
        Codec.intRange(1, 1024).optionalFieldOf("damage", 2).forGetter(EntityMeltingRecipe::damage)
    ).apply(instance, EntityMeltingRecipe::new));
    /** 客户端配方同步使用当前版本流体编解码器，输出组件由FluidStack处理。 */
    public static final StreamCodec<RegistryFriendlyByteBuf, EntityMeltingRecipe> STREAM_CODEC = StreamCodec.of(
        EntityMeltingRecipe::write, EntityMeltingRecipe::read);

    /** 限制网络列表大小，避免不可信载荷产生无界分配。 */
    private static EntityMeltingRecipe read(RegistryFriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        if (count < 0 || count > 4096) throw new IllegalArgumentException("Invalid entity ingredient size: " + count);
        java.util.ArrayList<String> entities = new java.util.ArrayList<>(count);
        for (int index = 0; index < count; index++) entities.add(buffer.readUtf());
        return new EntityMeltingRecipe(entities, buffer.readUtf(), FluidStack.STREAM_CODEC.decode(buffer), buffer.readVarInt());
    }

    /** 写入实体条件、输出和伤害值。 */
    private static void write(RegistryFriendlyByteBuf buffer, EntityMeltingRecipe recipe) {
        buffer.writeVarInt(recipe.entities.size());
        recipe.entities.forEach(buffer::writeUtf);
        buffer.writeUtf(recipe.entityTag);
        FluidStack.STREAM_CODEC.encode(buffer, recipe.result);
        buffer.writeVarInt(recipe.damage);
    }

    /** 直接匹配当前注册表的实体类型，标签可由数据包扩展。 */
    public boolean matchesEntity(EntityType<?> type) {
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (entityId != null && entities.contains(entityId.toString())) return true;
        ResourceLocation tag = ResourceLocation.tryParse(entityTag);
        return !entityTag.isEmpty() && tag != null && type.is(TagKey.create(Registries.ENTITY_TYPE, tag));
    }

    /** 不接受普通物品配方匹配。 */
    @Override public boolean matches(SingleRecipeInput input, Level level) { return false; }
    /** 实体配方没有物品产物。 */
    @Override public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) { return ItemStack.EMPTY; }
    /** 不参与合成台配方。 */
    @Override public boolean canCraftInDimensions(int width, int height) { return false; }
    /** 配方结果是流体而非物品。 */
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return ItemStack.EMPTY; }
    /** 禁止加入原版配方书。 */
    @Override public boolean isSpecial() { return true; }
    /** 返回独立序列化器。 */
    @Override public RecipeSerializer<?> getSerializer() { return TFRecipes.ENTITY_MELTING_SERIALIZER.get(); }
    /** 返回实体熔炼配方类型。 */
    @Override public RecipeType<?> getType() { return TFRecipes.ENTITY_MELTING.get(); }
}
