package org.hp.tinker_foundry.recipe;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraftforge.fluids.FluidStack;
import net.minecraft.world.level.material.Fluid;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** 1.20.1 Forge 的流体类型和流体标签匹配器。 */
public record FoundryFluidIngredient(List<Fluid> fluids, TagKey<Fluid> tag) {
    /** 单个流体输入允许携带的显式流体数量，和网络协议长度上限一致。 */
    public static final int MAX_EXPLICIT_FLUIDS = 128;
    /** 网络中的资源位置字符串最大 UTF-8 长度，避免恶意配方扩大缓冲区。 */
    public static final int MAX_RESOURCE_ID_LENGTH = 256;

    /** 从当前数据包的 fluid 或 tag 对象读取流体输入。 */
    public static FoundryFluidIngredient fromJson(JsonElement element) {
        List<Fluid> fluids = new ArrayList<>();
        TagKey<Fluid> tag = null;
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement entry : array) {
                FoundryFluidIngredient parsed = fromJson(entry);
                fluids.addAll(parsed.fluids());
                if (fluids.size() > MAX_EXPLICIT_FLUIDS) {
                    throw new IllegalArgumentException("Too many explicit fluid ingredients: " + fluids.size());
                }
                if (parsed.tag() != null) tag = parsed.tag();
            }
        } else {
            JsonObject object = element.getAsJsonObject();
            if (object.has("fluid")) {
                Fluid fluid = Registry.FLUID.getOptional(ResourceLocation.tryParse(object.get("fluid").getAsString())).orElse(null);
                if (fluid != null) fluids.add(fluid);
            }
            if (object.has("tag")) {
                ResourceLocation id = ResourceLocation.tryParse(object.get("tag").getAsString());
                if (id != null) tag = TagKey.create(Registry.FLUID_REGISTRY, id);
            }
        }
        if (fluids.isEmpty() && tag == null) throw new IllegalArgumentException("Foundry fluid ingredient is empty");
        if (fluids.size() > MAX_EXPLICIT_FLUIDS) {
            throw new IllegalArgumentException("Too many explicit fluid ingredients: " + fluids.size());
        }
        return new FoundryFluidIngredient(List.copyOf(fluids), tag);
    }

    /** 从网络读取显式流体或流体标签。 */
    public static FoundryFluidIngredient fromNetwork(FriendlyByteBuf buffer) {
        boolean hasTag = buffer.readBoolean();
        String tagId = hasTag ? buffer.readUtf(MAX_RESOURCE_ID_LENGTH) : "";
        int size = buffer.readVarInt();
        if (size < 0 || size > MAX_EXPLICIT_FLUIDS) {
            throw new IllegalArgumentException("Invalid fluid ingredient size: " + size);
        }
        List<Fluid> fluids = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            ResourceLocation id = ResourceLocation.tryParse(buffer.readUtf(MAX_RESOURCE_ID_LENGTH));
            if (id != null) Registry.FLUID.getOptional(id).ifPresent(fluids::add);
        }
        ResourceLocation parsedTag = hasTag ? ResourceLocation.tryParse(tagId) : null;
        if (hasTag && parsedTag == null) {
            throw new IllegalArgumentException("Invalid fluid ingredient tag: " + tagId);
        }
        TagKey<Fluid> tag = parsedTag == null ? null : TagKey.create(Registry.FLUID_REGISTRY, parsedTag);
        if (fluids.isEmpty() && tag == null) {
            throw new IllegalArgumentException("Foundry fluid ingredient is empty");
        }
        return new FoundryFluidIngredient(List.copyOf(fluids), tag);
    }

    /** 向网络写入显式流体或流体标签。 */
    public void toNetwork(FriendlyByteBuf buffer) {
        if (fluids.size() > MAX_EXPLICIT_FLUIDS) {
            throw new IllegalArgumentException("Too many explicit fluid ingredients: " + fluids.size());
        }
        buffer.writeBoolean(tag != null);
        if (tag != null) buffer.writeUtf(tag.location().toString(), MAX_RESOURCE_ID_LENGTH);
        buffer.writeVarInt(fluids.size());
        for (Fluid fluid : fluids) {
            ResourceLocation id = Registry.FLUID.getKey(fluid);
            if (id == null) throw new IllegalArgumentException("Unregistered fluid ingredient");
            buffer.writeUtf(id.toString(), MAX_RESOURCE_ID_LENGTH);
        }
    }

    /** 检查流体类型；FluidStack 的附加 NBT 不影响默认材料匹配。 */
    public boolean test(FluidStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (fluids.contains(stack.getFluid())) return true;
        if (tag == null) return false;
        for (Holder<Fluid> holder : Registry.FLUID.getTagOrEmpty(tag)) {
            if (holder.value() == stack.getFluid()) return true;
        }
        return false;
    }
}
