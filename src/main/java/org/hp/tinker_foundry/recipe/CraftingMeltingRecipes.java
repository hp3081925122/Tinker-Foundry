package org.hp.tinker_foundry.recipe;

import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import org.hp.tinker_foundry.TinkerFoundry;

/** 按合成台配方折算熔炼配方：材料全部可熔炼时，产物按材料总量除以产物数量向下取整熔炼。 */
final class CraftingMeltingRecipes {
    /** 多轮推导上限，允许由已推导物品继续合成的物品获得熔炼配方。 */
    private static final int MAX_PASSES = 4;

    /** 物品熔炼后的流体组成和温度。 */
    private record Melt(Map<Fluid, Integer> fluids, int temperature) { }

    /** 单个合成配方折算出的每个产物熔炼结果。 */
    private record Candidate(Item item, Map<Fluid, Integer> fluids, int temperature, int total) { }

    static int generate(MinecraftServer server, List<RecipeHolder<?>> recipes, Set<Identifier> ids, Set<Item> excluded, Set<Object> generated) {
        // 收集已有熔炼信息：只有普通熔炼可作为材料折算来源，矿石增产不计入
        Map<Item, Melt> melts = new IdentityHashMap<>();
        Set<Item> melted = Collections.newSetFromMap(new IdentityHashMap<>());
        for (RecipeHolder<?> holder : recipes) {
            Recipe<?> recipe = holder.value();
            if (recipe instanceof MeltingRecipe melting) {
                for (ItemStack stack : AutomaticMeltingRecipes.ingredientItems(melting.ingredient())) {
                    melted.add(stack.getItem());
                    if (melting.rate().equals("none")) melts.putIfAbsent(stack.getItem(), melt(melting.result(), melting.byproducts(), melting.temperature()));
                }
            } else if (recipe instanceof OreMeltingRecipe ore) {
                for (ItemStack stack : AutomaticMeltingRecipes.ingredientItems(ore.ingredient())) melted.add(stack.getItem());
            } else if (recipe instanceof DamageableMeltingRecipe damageable) {
                for (ItemStack stack : AutomaticMeltingRecipes.ingredientItems(damageable.ingredient())) melted.add(stack.getItem());
            }
        }
        List<RecipeHolder<?>> crafting = recipes.stream()
            .filter(holder -> holder.value() instanceof ShapedRecipe || holder.value() instanceof ShapelessRecipe)
            .sorted(Comparator.comparing(holder -> holder.id().identifier().toString())).toList();
        int count = 0;
        for (int pass = 0; pass < MAX_PASSES; pass++) {
            // 同一产物有多个合成配方时取熔出总量最少的，避免借便宜配方刷流体
            Map<Item, Candidate> best = new IdentityHashMap<>();
            for (RecipeHolder<?> holder : crafting) {
                Candidate candidate = candidate((CraftingRecipe) holder.value(), melts);
                if (candidate == null || melted.contains(candidate.item()) || excluded.contains(candidate.item())) continue;
                Candidate old = best.get(candidate.item());
                if (old == null || candidate.total() < old.total()) best.put(candidate.item(), candidate);
            }
            if (best.isEmpty()) break;
            List<Candidate> ordered = best.values().stream()
                .sorted(Comparator.comparing(candidate -> BuiltInRegistries.ITEM.getKey(candidate.item()).toString())).toList();
            for (Candidate candidate : ordered) {
                Identifier itemId = BuiltInRegistries.ITEM.getKey(candidate.item());
                Identifier id = Identifier.fromNamespaceAndPath(TinkerFoundry.MOD_ID,
                    "automatic/crafting/" + itemId.getNamespace() + "/" + itemId.getPath());
                if (!ids.add(id)) continue;
                Recipe<?> recipe = build(candidate);
                recipes.add(new RecipeHolder<>(ResourceKey.create(Registries.RECIPE, id), recipe));
                generated.add(recipe);
                melted.add(candidate.item());
                melts.put(candidate.item(), new Melt(candidate.fluids(), candidate.temperature()));
                count++;
            }
        }
        return count;
    }

    /** 折算一个合成配方；任一材料不可熔炼、带返还物品或产物带组件时返回 null。 */
    private static Candidate candidate(CraftingRecipe recipe, Map<Item, Melt> melts) {
        if (recipe.isSpecial()) return null;
        // 有序与无序合成的产物都是固定模板，组装时不读取输入
        ItemStack result;
        try {
            result = recipe.assemble(CraftingInput.EMPTY);
        } catch (RuntimeException exception) {
            // 子类动态产物需要真实输入，无法静态折算
            return null;
        }
        if (result.isEmpty() || !result.getComponentsPatch().isEmpty()) return null;
        Map<Fluid, Integer> total = new LinkedHashMap<>();
        int temperature = 0;
        boolean any = false;
        for (Ingredient ingredient : recipe.placementInfo().ingredients()) {
            List<ItemStack> options = AutomaticMeltingRecipes.ingredientItems(ingredient);
            if (options.isEmpty()) return null;
            // 同一格的可选物品必须熔出相同流体，用量取最小值
            Map<Fluid, Integer> common = null;
            int slotTemperature = 0;
            for (ItemStack option : options) {
                if (option.getItem().getCraftingRemainder(option) != null) return null;
                Melt melt = melts.get(option.getItem());
                if (melt == null) return null;
                if (common == null) {
                    common = new LinkedHashMap<>(melt.fluids());
                } else if (!common.keySet().equals(melt.fluids().keySet())) {
                    return null;
                } else {
                    for (Map.Entry<Fluid, Integer> entry : melt.fluids().entrySet()) common.merge(entry.getKey(), entry.getValue(), Math::min);
                }
                slotTemperature = Math.max(slotTemperature, melt.temperature());
            }
            common.forEach((fluid, amount) -> total.merge(fluid, amount, Integer::sum));
            temperature = Math.max(temperature, slotTemperature);
            any = true;
        }
        if (!any) return null;
        // 流体量只支持整数，按产物数量平分后向下取整
        Map<Fluid, Integer> perItem = new LinkedHashMap<>();
        int sum = 0;
        for (Map.Entry<Fluid, Integer> entry : total.entrySet()) {
            int amount = entry.getValue() / result.getCount();
            if (amount <= 0) continue;
            perItem.put(entry.getKey(), amount);
            sum += amount;
        }
        return perItem.isEmpty() ? null : new Candidate(result.getItem(), perItem, temperature, sum);
    }

    /** 量最多的流体作为主产物，其余作为副产物；耐久物品按剩余耐久折算。 */
    private static Recipe<?> build(Candidate candidate) {
        Fluid main = candidate.fluids().entrySet().stream()
            .max(Comparator.<Map.Entry<Fluid, Integer>>comparingInt(Map.Entry::getValue)
                .thenComparing(entry -> BuiltInRegistries.FLUID.getKey(entry.getKey()).toString(), Comparator.reverseOrder()))
            .orElseThrow().getKey();
        FluidStack result = new FluidStack(main, candidate.fluids().get(main));
        List<MeltingRecipe.Byproduct> byproducts = candidate.fluids().entrySet().stream()
            .filter(entry -> entry.getKey() != main)
            .sorted(Comparator.comparing(entry -> BuiltInRegistries.FLUID.getKey(entry.getKey()).toString()))
            .map(entry -> new MeltingRecipe.Byproduct(new FluidStack(entry.getKey(), entry.getValue()), "none")).toList();
        int time = Math.max(20, candidate.total() * 60 / 90);
        Ingredient ingredient = Ingredient.of(candidate.item());
        if (new ItemStack(candidate.item()).isDamageableItem()) {
            return new DamageableMeltingRecipe(ingredient, result, candidate.temperature(), time, result.getAmount() >= 90 ? 10 : 1, byproducts);
        }
        return new MeltingRecipe(ingredient, result, candidate.temperature(), time, "none", byproducts);
    }

    /** 把熔炼配方的主产物和副产物合并为流体组成。 */
    private static Melt melt(FluidStack result, List<MeltingRecipe.Byproduct> byproducts, int temperature) {
        Map<Fluid, Integer> fluids = new LinkedHashMap<>();
        if (!result.isEmpty()) fluids.merge(AutomaticMeltingRecipes.source(result.getFluid()), result.getAmount(), Integer::sum);
        for (MeltingRecipe.Byproduct byproduct : byproducts) {
            if (!byproduct.result().isEmpty()) fluids.merge(AutomaticMeltingRecipes.source(byproduct.result().getFluid()), byproduct.result().getAmount(), Integer::sum);
        }
        return new Melt(fluids, temperature);
    }

    private CraftingMeltingRecipes() { }
}
