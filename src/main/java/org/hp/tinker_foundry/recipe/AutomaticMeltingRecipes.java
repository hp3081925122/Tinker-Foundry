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
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.fluids.FluidStack;
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
        List<Recipe<?>> recipes = new ArrayList<>(manager.getRecipes().stream()
            .filter(value -> !previous.contains(value)).toList());
        Map<Item, Form> forms = new IdentityHashMap<>();
        Set<Item> excluded = Collections.newSetFromMap(new IdentityHashMap<>());
        List<Item> items = ForgeRegistries.ITEMS.getValues().stream()
            .sorted(Comparator.comparing(item -> itemId(item).toString())).toList();
        for (Item item : items) {
            Set<Form> tagged = new LinkedHashSet<>();
            item.getDefaultInstance().getTags().forEach(tag -> {
                ResourceLocation id = tag.location();
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
        Set<ResourceLocation> ids = new HashSet<>();
        for (Recipe<?> recipe : recipes) {
            ids.add(recipe.getId());
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
        for (Recipe<?> recipe : recipes) {
            if (!(recipe instanceof CastingRecipe casting)) continue;
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
            ResourceLocation itemId = itemId(item);
            ResourceLocation id = new ResourceLocation(TinkerFoundry.MOD_ID,
                "automatic/melting/" + itemId.getNamespace() + "/" + itemId.getPath());
            if (!ids.add(id)) continue;
            int time = Math.max(20, form.amount() * (form.ore() ? 90 : 60) / 90);
            Ingredient ingredient = Ingredient.of(item);
            FluidStack result = new FluidStack(metal.fluid(), amount);
            Recipe<?> recipe = form.ore()
                ? new OreMeltingRecipe(id, ingredient, result, metal.temperature(), time, gemstones.contains(form.material()) ? "gem" : "metal", List.of())
                : new MeltingRecipe(id, ingredient, result, metal.temperature(), time, "none", List.of());
            recipes.add(recipe);
            generated.add(recipe);
        }
        int melting = generated.size();
        // 材料全部可熔炼的合成配方折算熔炼配方
        int crafting = CraftingMeltingRecipes.generate(server, recipes, ids, excluded, generated);
        // 已有浇注配方产出的材料形态不再自动生成，避免和数据包配方重复
        Set<String> castOutputs = new HashSet<>();
        for (Recipe<?> recipe : recipes) {
            if (!(recipe instanceof CastingRecipe casting)) continue;
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
        manager.replaceRecipes(recipes);
        GENERATED.put(server, generated);
        TinkerFoundry.LOGGER.info("Automatic melting recipes: generated={}, crafting={}, casting={}, existingInputs={}, ambiguousMaterials={}",
            melting, crafting, generated.size() - melting - crafting, covered.size(), ambiguous.size());
    }

    private static void addCasting(List<Recipe<?>> recipes, Set<Object> generated, Set<ResourceLocation> ids, Set<String> castOutputs,
                                   Map<Item, Form> forms, Set<Item> excluded, List<Item> items, String material, Metal metal,
                                   AutomaticMeltingRules.Kind kind, Optional<Ingredient> mold, int time) {
        if (castOutputs.contains(material + "/" + kind)) return;
        // 产物优先选与流体同模组的物品，其次原版物品，最后按物品 ID 排序取第一个
        String fluidNamespace = ForgeRegistries.FLUIDS.getKey(metal.fluid()).getNamespace();
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
        ResourceLocation itemId = itemId(result);
        ResourceLocation id = new ResourceLocation(TinkerFoundry.MOD_ID,
            "automatic/casting/" + itemId.getNamespace() + "/" + itemId.getPath());
        if (!ids.add(id)) return;
        CastingRecipe recipe = new CastingRecipe(id, new FoundryFluidAmount(new FoundryFluidIngredient(List.of(metal.fluid()), null), amount),
            mold, new ItemStack(result), time, false, false, false, Optional.empty());
        recipes.add(recipe);
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
        ForgeRegistries.FLUIDS.tags().getTagNames().forEach(tag -> {
            ResourceLocation id = tag.location();
            String material = AutomaticMeltingRules.fromFluidTag(id.getNamespace(), id.getPath());
            if (material != null) ForgeRegistries.FLUIDS.tags().getTag(tag).forEach(fluid -> addCandidate(candidates, material, source(fluid)));
        });
        candidates.replaceAll((material, fluids) -> {
            Set<Fluid> external = new LinkedHashSet<>();
            for (Fluid fluid : fluids) if (!ForgeRegistries.FLUIDS.getKey(fluid).getNamespace().equals(TinkerFoundry.MOD_ID)) external.add(fluid);
            return external.isEmpty() ? fluids : external;
        });
        return candidates;
    }

    private static void addCandidate(Map<String, Set<Fluid>> candidates, String material, Fluid fluid) {
        candidates.computeIfAbsent(material, key -> new LinkedHashSet<>()).add(fluid);
    }

    static Fluid source(Fluid fluid) {
        return fluid instanceof FlowingFluid flowing ? flowing.getSource() : fluid;
    }

    private static int temperature(Fluid fluid) {
        return Math.max(500, fluid.getFluidType().getTemperature() - 300);
    }

    private static List<ItemStack> ingredientItems(Ingredient ingredient) {
        return List.of(ingredient.getItems());
    }

    private static List<Fluid> castingFluids(CastingRecipe recipe) {
        List<Fluid> fluids = new ArrayList<>(recipe.fluid().ingredient().fluids());
        var tag = recipe.fluid().ingredient().tag();
        if (tag != null) ForgeRegistries.FLUIDS.tags().getTag(tag).forEach(fluids::add);
        return fluids;
    }

    private static ResourceLocation itemId(Item item) {
        return ForgeRegistries.ITEMS.getKey(item);
    }

    private AutomaticMeltingRecipes() { }
}
