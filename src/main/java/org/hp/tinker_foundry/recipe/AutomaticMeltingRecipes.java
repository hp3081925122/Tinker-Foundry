package org.hp.tinker_foundry.recipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.RecipeMap;
import org.hp.tinker_foundry.mixin.RecipeManagerAccessor;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import org.hp.tinker_foundry.TinkerFoundry;
import org.hp.tinker_foundry.registry.TFItems;
import org.hp.tinker_foundry.common.AutomaticMeltingRules;
import org.hp.tinker_foundry.common.AutomaticMeltingRules.Form;

public final class AutomaticMeltingRecipes {
    private static final Map<MinecraftServer, Set<Object>> GENERATED = new WeakHashMap<>();

    private record Metal(Fluid fluid, int ingotAmount, int temperature) { }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void starting(ServerAboutToStartEvent event) {
        rebuild(event.getServer());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void sync(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) rebuild(event.getPlayerList().getServer());
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        GENERATED.remove(event.getServer());
    }

    public static void rebuild(MinecraftServer server) {
        var manager = server.getRecipeManager();
        Set<Object> previous = GENERATED.getOrDefault(server, Set.of());
        List<RecipeHolder<?>> recipes = new ArrayList<>(manager.recipeMap().values().stream()
            .filter(holder -> !previous.contains(holder.value())).toList());
        Map<Item, Form> forms = new IdentityHashMap<>();
        Set<Item> excluded = Collections.newSetFromMap(new IdentityHashMap<>());
        List<Item> items = BuiltInRegistries.ITEM.listElements().map(holder -> holder.value())
            .sorted(Comparator.comparing(item -> itemId(item).toString())).toList();
        for (Item item : items) {
            Set<Form> tagged = new LinkedHashSet<>();
            item.builtInRegistryHolder().tags().forEach(tag -> {
                Identifier id = tag.location();
                if (id.getNamespace().equals(TinkerFoundry.MOD_ID) && id.getPath().equals("melting/automatic_blacklist")) excluded.add(item);
                Form form = AutomaticMeltingRules.fromTag(id.getNamespace(), id.getPath());
                if (form != null) tagged.add(form);
            });
            Form form = tagged.size() == 1 ? tagged.iterator().next() : null;
            if (form != null) forms.put(item, form);
        }
        Set<String> gemstones = new HashSet<>();
        forms.values().stream().filter(form -> form.kind() == AutomaticMeltingRules.Kind.GEM).forEach(form -> gemstones.add(form.material()));
        Map<String, Metal> metals = new HashMap<>();
        Map<String, Set<Fluid>> candidates = fluidCandidates();
        Set<String> ambiguous = new HashSet<>();
        candidates.forEach((material, fluids) -> { if (fluids.size() > 1) ambiguous.add(material); });
        Set<Item> covered = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<Identifier> ids = new HashSet<>();
        for (RecipeHolder<?> holder : recipes) {
            ids.add(holder.id().identifier());
            Recipe<?> recipe = holder.value();
            Ingredient input = recipe instanceof MeltingRecipe melting ? melting.ingredient()
                : recipe instanceof OreMeltingRecipe ore ? ore.ingredient()
                : recipe instanceof DamageableMeltingRecipe damageable ? damageable.ingredient() : null;
            if (input == null) continue;
            for (ItemStack stack : ingredientItems(input)) {
                covered.add(stack.getItem());
                Form form = forms.get(stack.getItem());
                if (form != null && !form.ore() && recipe instanceof MeltingRecipe melting && melting.rate().equals("none")
                    && candidates.getOrDefault(form.material(), Set.of()).contains(source(melting.result().getFluid()))) {
                    merge(metals, ambiguous, form.material(), melting.result().getFluid(),
                        AutomaticMeltingRules.ingotVolume(melting.result().getAmount(), form.amount(), 1), melting.temperature());
                }
            }
        }
        for (RecipeHolder<?> holder : recipes) {
            if (!(holder.value() instanceof CastingRecipe casting)) continue;
            Form form = forms.get(casting.result().getItem());
            if (form == null || form.ore()) continue;
            List<Fluid> fluids = castingFluids(casting).stream().map(AutomaticMeltingRecipes::source).distinct().toList();
            fluids = fluids.stream().filter(fluid -> candidates.getOrDefault(form.material(), Set.of()).contains(fluid)).toList();
            Metal existing = metals.get(form.material());
            if (existing != null && fluids.contains(existing.fluid())) fluids = List.of(existing.fluid());
            if (fluids.size() != 1) continue;
            Fluid fluid = fluids.get(0);
            merge(metals, ambiguous, form.material(), fluid,
                AutomaticMeltingRules.ingotVolume(casting.fluid().amount(), form.amount(), casting.result().getCount()),
                existing != null ? existing.temperature() : temperature(fluid));
        }
        Set<String> materials = new HashSet<>();
        forms.values().forEach(form -> materials.add(form.material()));
        for (String material : materials) {
            if (metals.containsKey(material) || ambiguous.contains(material)) continue;
            Set<Fluid> fluids = candidates.getOrDefault(material, Set.of());
            if (fluids.size() == 1) {
                Fluid fluid = fluids.iterator().next();
                metals.put(material, new Metal(fluid, gemstones.contains(material) ? 100 : 90, temperature(fluid)));
            } else if (fluids.size() > 1) {
                ambiguous.add(material);
            }
        }
        Set<Object> generated = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Item item : items) {
            Form form = forms.get(item);
            if (form == null || covered.contains(item) || excluded.contains(item) || ambiguous.contains(form.material())) continue;
            Metal metal = metals.get(form.material());
            if (metal == null) continue;
            int amount = AutomaticMeltingRules.volume(form.amount(), metal.ingotAmount());
            if (amount <= 0) continue;
            Identifier itemId = itemId(item);
            Identifier id = Identifier.fromNamespaceAndPath(TinkerFoundry.MOD_ID,
                "automatic/melting/" + itemId.getNamespace() + "/" + itemId.getPath());
            if (!ids.add(id)) continue;
            int time = Math.max(20, form.amount() * (form.ore() ? 90 : 60) / 90);
            Ingredient ingredient = Ingredient.of(item);
            FluidStack result = new FluidStack(metal.fluid(), amount);
            Recipe<?> recipe = form.ore()
                ? new OreMeltingRecipe(ingredient, result, metal.temperature(), time, gemstones.contains(form.material()) ? "gem" : "metal")
                : new MeltingRecipe(ingredient, result, metal.temperature(), time);
            recipes.add(new RecipeHolder<>(ResourceKey.create(Registries.RECIPE, id), recipe));
            generated.add(recipe);
        }
        int melting = generated.size();
        // 已有浇注配方产出的材料形态不再自动生成，避免和数据包配方重复
        Set<String> castOutputs = new HashSet<>();
        for (RecipeHolder<?> holder : recipes) {
            if (!(holder.value() instanceof CastingRecipe casting)) continue;
            Form form = forms.get(casting.result().getItem());
            if (form != null) castOutputs.add(form.material() + "/" + form.kind());
        }
        List<String> sortedMaterials = materials.stream().sorted().toList();
        for (String material : sortedMaterials) {
            if (ambiguous.contains(material)) continue;
            Metal metal = metals.get(material);
            if (metal == null) continue;
            // 锭用锭模、宝石用宝石模在浇注台产出，块在浇注盆直接产出
            addCasting(recipes, generated, ids, castOutputs, forms, excluded, items, material, metal,
                AutomaticMeltingRules.Kind.INGOT, Optional.of(Ingredient.of(TFItems.INGOT_CAST.get())), 60);
            addCasting(recipes, generated, ids, castOutputs, forms, excluded, items, material, metal,
                AutomaticMeltingRules.Kind.GEM, Optional.of(Ingredient.of(TFItems.GEM_CAST.get())), 60);
            addCasting(recipes, generated, ids, castOutputs, forms, excluded, items, material, metal,
                AutomaticMeltingRules.Kind.BLOCK, Optional.empty(), 180);
        }
        ((RecipeManagerAccessor) (Object) manager).tinkerFoundry$setRecipes(RecipeMap.create(recipes));
        manager.finalizeRecipeLoading(server.getWorldData().enabledFeatures());
        GENERATED.put(server, generated);
        TinkerFoundry.LOGGER.info("Automatic melting recipes: generated={}, casting={}, existingInputs={}, ambiguousMaterials={}",
            melting, generated.size() - melting, covered.size(), ambiguous.size());
    }

    private static void addCasting(List<RecipeHolder<?>> recipes, Set<Object> generated, Set<Identifier> ids, Set<String> castOutputs,
                                   Map<Item, Form> forms, Set<Item> excluded, List<Item> items, String material, Metal metal,
                                   AutomaticMeltingRules.Kind kind, Optional<Ingredient> mold, int time) {
        if (castOutputs.contains(material + "/" + kind)) return;
        // 产物优先选与流体同模组的物品，其次原版物品，最后按物品 ID 排序取第一个
        String fluidNamespace = BuiltInRegistries.FLUID.getKey(metal.fluid()).getNamespace();
        Item result = items.stream()
            .filter(item -> !excluded.contains(item) && new Form(material, kind).equals(forms.get(item)))
            .min(Comparator.comparingInt((Item item) -> {
                String namespace = itemId(item).getNamespace();
                return namespace.equals(fluidNamespace) ? 0 : namespace.equals("minecraft") ? 1 : 2;
            }))
            .orElse(null);
        if (result == null) return;
        int amount = AutomaticMeltingRules.volume(kind.amount, metal.ingotAmount());
        if (amount <= 0) return;
        Identifier itemId = itemId(result);
        Identifier id = Identifier.fromNamespaceAndPath(TinkerFoundry.MOD_ID,
            "automatic/casting/" + itemId.getNamespace() + "/" + itemId.getPath());
        if (!ids.add(id)) return;
        CastingRecipe recipe = new CastingRecipe(SizedFluidIngredient.of(metal.fluid(), amount), mold, new ItemStack(result), time);
        recipes.add(new RecipeHolder<>(ResourceKey.create(Registries.RECIPE, id), recipe));
        generated.add(recipe);
    }

    private static void merge(Map<String, Metal> metals, Set<String> ambiguous, String material, Fluid fluid, int amount, int temperature) {
        if (amount <= 0 || fluid == Fluids.EMPTY || ambiguous.contains(material)) return;
        fluid = source(fluid);
        Metal old = metals.get(material);
        if (old != null && old.fluid() != fluid) {
            metals.remove(material);
            ambiguous.add(material);
        } else {
            metals.put(material, new Metal(fluid, old == null ? amount : Math.min(old.ingotAmount(), amount),
                old == null ? temperature : Math.max(old.temperature(), temperature)));
        }
    }

    private static Map<String, Set<Fluid>> fluidCandidates() {
        Map<String, Set<Fluid>> candidates = new HashMap<>();
        BuiltInRegistries.FLUID.listElements().forEach(holder -> {
            Fluid fluid = source(holder.value());
            if (fluid == Fluids.EMPTY) return;
            holder.tags().forEach(tag -> {
                Identifier tagId = tag.location();
                String tagged = AutomaticMeltingRules.fromFluidTag(tagId.getNamespace(), tagId.getPath());
                if (tagged != null) addCandidate(candidates, tagged, fluid);
            });
        });
        candidates.replaceAll((material, fluids) -> {
            Set<Fluid> external = new LinkedHashSet<>();
            for (Fluid fluid : fluids) if (!BuiltInRegistries.FLUID.getKey(fluid).getNamespace().equals(TinkerFoundry.MOD_ID)) external.add(fluid);
            return external.isEmpty() ? fluids : external;
        });
        return candidates;
    }

    private static void addCandidate(Map<String, Set<Fluid>> candidates, String material, Fluid fluid) {
        candidates.computeIfAbsent(material, key -> new LinkedHashSet<>()).add(fluid);
    }

    private static Fluid source(Fluid fluid) {
        return fluid instanceof FlowingFluid flowing ? flowing.getSource() : fluid;
    }

    private static int temperature(Fluid fluid) {
        return Math.max(500, fluid.getFluidType().getTemperature() - 300);
    }

    private static List<ItemStack> ingredientItems(Ingredient ingredient) {
        return ingredient.items().map(holder -> new ItemStack(holder.value())).toList();
    }

    private static List<Fluid> castingFluids(CastingRecipe recipe) {
        return recipe.fluid().ingredient().fluids().stream().map(holder -> holder.value()).toList();
    }

    private static Identifier itemId(Item item) {
        return BuiltInRegistries.ITEM.getKey(item);
    }

    private AutomaticMeltingRecipes() { }
}
