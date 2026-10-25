package org.hp.tinker_foundry.recipe;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fluids.FluidStack;

/** 带数量边界的 Forge 流体输入，替代 1.21 SizedFluidIngredient。 */
public record FoundryFluidAmount(FoundryFluidIngredient ingredient, int amount) {
    /** 从旧数据包保留的 ingredient/amount 对象读取。 */
    public static FoundryFluidAmount fromJson(JsonObject object) {
        int amount = object.get("amount").getAsInt();
        if (amount <= 0) throw new IllegalArgumentException("Fluid ingredient amount must be positive");
        return new FoundryFluidAmount(FoundryFluidIngredient.fromJson(object.get("ingredient")), amount);
    }

    /** 从网络读取流体输入及容量。 */
    public static FoundryFluidAmount fromNetwork(FriendlyByteBuf buffer) {
        int amount = buffer.readVarInt();
        if (amount <= 0) throw new IllegalArgumentException("Invalid fluid ingredient amount: " + amount);
        return new FoundryFluidAmount(FoundryFluidIngredient.fromNetwork(buffer), amount);
    }

    /** 向网络写入流体输入及容量。 */
    public void toNetwork(FriendlyByteBuf buffer) {
        buffer.writeVarInt(amount);
        ingredient.toNetwork(buffer);
    }

    /** 检查类型与数量。 */
    public boolean test(FluidStack stack) {
        return stack != null && stack.getAmount() >= amount && ingredient.test(stack);
    }
}
