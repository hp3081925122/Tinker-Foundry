package org.hp.tinker_foundry.recipe;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

/** 统一保存 1.21.1 NeoForge 所需的 MapCodec 和网络 StreamCodec。 */
public record FoundryRecipeSerializer<T extends Recipe<?>>(MapCodec<T> codec, StreamCodec<RegistryFriendlyByteBuf, T> streamCodec) implements RecipeSerializer<T> {
}
