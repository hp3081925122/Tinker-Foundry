package org.hp.tinker_foundry.recipe;

import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
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

/** 流体经过可选模具冷却后产出物品。 */
public record CastingRecipe(ResourceLocation id, FoundryFluidAmount fluid, Optional<Ingredient> mold, ItemStack result, int time,
                            boolean castConsumed, boolean switchSlots, boolean copyPotionContents) implements Recipe<FluidRecipeInput> {
    /** 返回 Forge 1.20.1 Recipe 接口要求的配方 ID。 */
    @Override
    public ResourceLocation getId() {
        return id;
    }

    /** 保留旧构造签名，默认使用可重复铸模且不切换槽位。 */
    public CastingRecipe(FoundryFluidAmount fluid, Optional<Ingredient> mold, ItemStack result, int time) {
        this(runtimeId(), fluid, mold, result, time, false, false, false);
    }

    /** 从数据包读取浇注配方。 */
    public static CastingRecipe fromJson(ResourceLocation id, JsonObject json) {
        Optional<Ingredient> mold = json.has("mold") ? Optional.of(Ingredient.fromJson(json.get("mold"))) : Optional.empty();
        return new CastingRecipe(id, FoundryFluidAmount.fromJson(json.getAsJsonObject("fluid")), mold,
            FoundryRecipeData.item(json.getAsJsonObject("result")), json.has("time") ? json.get("time").getAsInt() : 60,
            json.has("cast_consumed") && json.get("cast_consumed").getAsBoolean(),
            json.has("switch_slots") && json.get("switch_slots").getAsBoolean(),
            json.has("copy_potion_contents") && json.get("copy_potion_contents").getAsBoolean());
    }

    /** 从网络读取浇注配方。 */
    public static CastingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
        Optional<Ingredient> mold = buffer.readBoolean() ? Optional.of(Ingredient.fromNetwork(buffer)) : Optional.empty();
        return new CastingRecipe(id, FoundryFluidAmount.fromNetwork(buffer), mold, buffer.readItem(), buffer.readVarInt(),
            buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean());
    }

    /** 向网络写入浇注配方。 */
    public static void toNetwork(FriendlyByteBuf buffer, CastingRecipe recipe) {
        buffer.writeBoolean(recipe.mold.isPresent());
        recipe.mold.ifPresent(value -> value.toNetwork(buffer));
        recipe.fluid.toNetwork(buffer);
        buffer.writeItem(recipe.result);
        buffer.writeVarInt(recipe.time);
        buffer.writeBoolean(recipe.castConsumed);
        buffer.writeBoolean(recipe.switchSlots);
        buffer.writeBoolean(recipe.copyPotionContents);
    }

    /** 校验流体和可选模具。 */
    @Override
    public boolean matches(FluidRecipeInput input, Level level) {
        if (input.requireMold() != mold.isPresent()) return false;
        boolean moldMatches = mold.map(value -> !input.getItem(0).isEmpty() && value.test(input.getItem(0))).orElse(input.getItem(0).isEmpty());
        return !input.fluids().isEmpty() && fluid.test(input.fluids().get(0)) && moldMatches;
    }

    /** 返回浇注物品副本，并保留旧版流体附加 NBT 的移交语义。 */
    @Override
    public ItemStack assemble(FluidRecipeInput input) {
        ItemStack assembled = result.copy();
        if (copyPotionContents && !input.fluids().isEmpty() && input.fluids().get(0).hasTag()) {
            CompoundTag fluidTag = input.fluids().get(0).getTag();
            CompoundTag itemTag = assembled.getOrCreateTag();
            if (fluidTag.contains("Potion", Tag.TAG_STRING)) {
                itemTag.putString("Potion", fluidTag.getString("Potion"));
            }
            if (fluidTag.contains("CustomPotionColor", Tag.TAG_INT)) {
                itemTag.putInt("CustomPotionColor", fluidTag.getInt("CustomPotionColor"));
            }
            if (fluidTag.contains("CustomPotionEffects", Tag.TAG_LIST)) {
                itemTag.put("CustomPotionEffects", fluidTag.get("CustomPotionEffects").copy());
            }
        }
        return assembled;
    }

    /** 浇注使用一个工作单元。 */
    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    /** 返回浇注结果。 */
    @Override
    public ItemStack getResultItem() {
        return result.copy();
    }

    /** 浇注配方不进入原版配方书。 */
    @Override
    public boolean isSpecial() {
        return true;
    }

    /** 返回浇注配方序列化器。 */
    @Override
    public RecipeSerializer<?> getSerializer() {
        return TFRecipes.CASTING_SERIALIZER.get();
    }

    /** 返回独立 casting 配方类型。 */
    @Override
    public RecipeType<?> getType() {
        return TFRecipes.CASTING.get();
    }

    /** 旧构造使用的稳定内部 ID。 */
    private static ResourceLocation runtimeId() {
        return new ResourceLocation(TinkerFoundry.MOD_ID, "casting/runtime");
    }
}
