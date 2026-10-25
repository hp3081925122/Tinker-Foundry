package org.hp.tinker_foundry.recipe;

import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraftforge.fluids.FluidStack;
import com.mojang.serialization.JsonOps;

/** 为 Forge 1.20.1 配方序列化器提供稳定的物品、流体和副产物读写。 */
public final class FoundryRecipeData {
    /** 读取当前项目的 id/count 物品结果格式，同时兼容原版 item/count 写法。 */
    public static ItemStack item(JsonObject object) {
        String key = object.has("item") ? "item" : "id";
        ResourceLocation id = ResourceLocation.tryParse(object.get(key).getAsString());
        Item item = BuiltInRegistries.ITEM.getOptional(id).orElseThrow(() -> new IllegalArgumentException("Unknown item: " + id));
        int count = object.has("count") ? object.get("count").getAsInt() : 1;
        if (count <= 0 || count > 99) throw new IllegalArgumentException("Invalid item count: " + count);
        ItemStack stack = new ItemStack(item, count);
        CompoundTag tag = legacyTag(object);
        if (!tag.isEmpty()) stack.setTag(tag);
        return stack;
    }

    /** 读取 id/amount 形式的流体，并把额外 JSON 保留到 Forge 流体 NBT。 */
    public static FluidStack fluid(JsonObject object) {
        ResourceLocation id = ResourceLocation.tryParse(object.get("id").getAsString());
        Fluid fluid = BuiltInRegistries.FLUID.getOptional(id).orElseThrow(() -> new IllegalArgumentException("Unknown fluid: " + id));
        FluidStack stack = new FluidStack(fluid, object.get("amount").getAsInt());
        CompoundTag tag = legacyTag(object);
        if (!tag.isEmpty()) stack.setTag(tag);
        return stack;
    }

    /** 把 1.21.1 组件写法转换为 1.20.1 ItemStack 使用的传统 NBT。 */
    private static CompoundTag legacyTag(JsonObject object) {
        if (object.has("tag")) {
            return (CompoundTag) JsonOps.INSTANCE.convertTo(NbtOps.INSTANCE, object.get("tag"));
        }
        if (!object.has("components")) {
            return new CompoundTag();
        }
        CompoundTag components = (CompoundTag) JsonOps.INSTANCE.convertTo(NbtOps.INSTANCE, object.get("components"));
        CompoundTag tag = new CompoundTag();
        if (components.contains("minecraft:potion_contents", Tag.TAG_COMPOUND)) {
            CompoundTag potion = components.getCompound("minecraft:potion_contents");
            if (potion.contains("potion", Tag.TAG_STRING)) {
                tag.putString("Potion", potion.getString("potion"));
            }
            if (potion.contains("custom_color", Tag.TAG_INT)) {
                tag.putInt("CustomPotionColor", potion.getInt("custom_color"));
            }
            if (potion.contains("custom_effects", Tag.TAG_LIST)) {
                tag.put("CustomPotionEffects", potion.get("custom_effects").copy());
            }
        }
        return tag;
    }

    /** Forge 1.20.1 流体包编码不携带空栈，因此直接复用 FluidStack packet API。 */
    public static FluidStack fluid(FriendlyByteBuf buffer) {
        return FluidStack.readFromPacket(buffer);
    }

    /** 写入流体包。 */
    public static void fluid(FriendlyByteBuf buffer, FluidStack stack) {
        stack.writeToPacket(buffer);
    }

    /** 读取有限长度的副产物列表。 */
    public static java.util.List<MeltingRecipe.Byproduct> byproducts(JsonObject object) {
        java.util.List<MeltingRecipe.Byproduct> values = new java.util.ArrayList<>();
        if (!object.has("byproducts")) return values;
        for (var element : object.getAsJsonArray("byproducts")) {
            JsonObject byproduct = element.getAsJsonObject();
            values.add(new MeltingRecipe.Byproduct(fluid(byproduct.getAsJsonObject("result")),
                byproduct.has("rate") ? byproduct.get("rate").getAsString() : "default"));
        }
        if (values.size() > 128) throw new IllegalArgumentException("Too many byproducts");
        return values;
    }

    /** 写入有限长度的副产物列表。 */
    public static void byproducts(FriendlyByteBuf buffer, java.util.List<MeltingRecipe.Byproduct> values) {
        if (values.size() > 128) throw new IllegalArgumentException("Too many byproducts");
        buffer.writeVarInt(values.size());
        for (MeltingRecipe.Byproduct value : values) {
            fluid(buffer, value.result());
            buffer.writeUtf(value.rate(), 256);
        }
    }

    private FoundryRecipeData() {
    }
}
