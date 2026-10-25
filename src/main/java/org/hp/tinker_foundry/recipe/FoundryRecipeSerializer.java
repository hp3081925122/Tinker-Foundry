package org.hp.tinker_foundry.recipe;

import com.google.gson.JsonObject;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

/** 统一实现 Forge 1.20.1 RecipeSerializer 的 JSON 和 FriendlyByteBuf 三个入口。 */
public record FoundryRecipeSerializer<T extends Recipe<?>>(BiFunction<ResourceLocation, JsonObject, T> jsonReader,
                                                            BiFunction<ResourceLocation, FriendlyByteBuf, T> networkReader,
                                                            BiConsumer<FriendlyByteBuf, T> networkWriter) implements RecipeSerializer<T> {
    /** 从数据包 JSON 构造配方。 */
    @Override
    public T fromJson(ResourceLocation id, JsonObject json) {
        return jsonReader.apply(id, org.hp.tinker_foundry.registry.TFFluids.remapRecipe(json));
    }

    /** 从 Forge FriendlyByteBuf 构造配方。 */
    @Override
    public T fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
        return networkReader.apply(id, buffer);
    }

    /** 向 Forge FriendlyByteBuf 写入配方。 */
    @Override
    public void toNetwork(FriendlyByteBuf buffer, T recipe) {
        networkWriter.accept(buffer, recipe);
    }
}
