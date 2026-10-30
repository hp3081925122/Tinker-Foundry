package org.hp.tinker_foundry.compat;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated.StartDirection;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.gui.placement.HorizontalAlignment;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.mojang.datafixers.util.Either;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import org.hp.tinker_foundry.TinkerFoundry;
import org.hp.tinker_foundry.client.FoundryRecipeSyncClient;
import org.hp.tinker_foundry.common.FluidValues;
import org.hp.tinker_foundry.recipe.AlloyingRecipe;
import org.hp.tinker_foundry.recipe.CastingRecipe;
import org.hp.tinker_foundry.recipe.DamageableMeltingRecipe;
import org.hp.tinker_foundry.recipe.EntityMeltingRecipe;
import org.hp.tinker_foundry.recipe.FuelRecipe;
import org.hp.tinker_foundry.recipe.MeltingRecipe;
import org.hp.tinker_foundry.recipe.MoldingRecipe;
import org.hp.tinker_foundry.recipe.OreMeltingRecipe;
import org.hp.tinker_foundry.registry.TFItems;
import org.hp.tinker_foundry.registry.TFRecipes;

/** JEI 可选插件；没有 JEI 时本类不会被模组主类加载。 */
@JeiPlugin
public final class FoundryJeiPlugin implements IModPlugin {
    /** JEI 插件自身的稳定标识。 */
    private static final Identifier PLUGIN_ID = Identifier.fromNamespaceAndPath(TinkerFoundry.MOD_ID, "jei_plugin");
    /** JEI 熔炼分类类型。 */
    private static final RecipeType<MeltingRecipe> MELTING_TYPE = RecipeType.create(TinkerFoundry.MOD_ID, "melting", MeltingRecipe.class);
    /** JEI 合金分类类型。 */
    private static final RecipeType<AlloyingRecipe> ALLOYING_TYPE = RecipeType.create(TinkerFoundry.MOD_ID, "alloying", AlloyingRecipe.class);
    /** JEI 铸造盆分类类型。 */
    private static final RecipeType<CastingRecipe> CASTING_BASIN_TYPE = RecipeType.create(TinkerFoundry.MOD_ID, "casting_basin", CastingRecipe.class);
    /** JEI 铸件台分类类型。 */
    private static final RecipeType<CastingRecipe> CASTING_TABLE_TYPE = RecipeType.create(TinkerFoundry.MOD_ID, "casting_table", CastingRecipe.class);
    /** JEI 压模分类类型。 */
    private static final RecipeType<MoldingRecipe> MOLDING_TYPE = RecipeType.create(TinkerFoundry.MOD_ID, "molding", MoldingRecipe.class);
    /** JEI 燃料分类类型。 */
    private static final RecipeType<FuelRecipe> FUEL_TYPE = RecipeType.create(TinkerFoundry.MOD_ID, "fuel", FuelRecipe.class);
    /** JEI 生物熔炼分类类型。 */
    private static final RecipeType<EntityMeltingRecipe> ENTITY_MELTING_TYPE = RecipeType.create(TinkerFoundry.MOD_ID, "entity_melting", EntityMeltingRecipe.class);
    /** 官方冶炼链 JEI 分类的独立背景图集。 */
    private static final Identifier CASTING_BACKGROUND = Identifier.fromNamespaceAndPath(TinkerFoundry.MOD_ID, "textures/gui/jei/casting.png");
    /** 官方合金分类的独立背景图集。 */
    private static final Identifier ALLOY_BACKGROUND = Identifier.fromNamespaceAndPath(TinkerFoundry.MOD_ID, "textures/gui/jei/alloy.png");
    /** 官方熔炼和燃料分类的独立背景图集。 */
    private static final Identifier MELTING_BACKGROUND = Identifier.fromNamespaceAndPath(TinkerFoundry.MOD_ID, "textures/gui/jei/melting.png");
    /** 当前客户端同步到的固体燃料展示栈。 */
    private static List<ItemStack> JEI_SOLID_FUELS = List.of();
    /** 当前客户端同步到的流体燃料展示栈。 */
    private static List<FluidStack> JEI_FLUID_FUELS = List.of();
    /** 当前客户端同步到的最高固体燃料温度。 */
    private static int JEI_SOLID_FUEL_TEMPERATURE;
    /** 当前 JEI runtime，只有 JEI 存在时才会被填充。 */
    private static IJeiRuntime JEI_RUNTIME;
    /** 防止服务端同步事件重复把同一批配方追加到 JEI。 */
    private static boolean JEI_RECIPES_REGISTERED;

    /** 返回 JEI 插件的唯一标识。 */
    @Override
    public Identifier getPluginUid() {
        return PLUGIN_ID;
    }

    /** 注册独立背景和布局的七类冶炼配方。 */
    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
            new MeltingCategory(guiHelper),
            new AlloyingCategory(guiHelper),
            new CastingCategory(
                guiHelper,
                CASTING_BASIN_TYPE,
                Component.translatable("jei.tinker_foundry.casting.basin"),
                TFItems.SEARED_BASIN.get(),
                guiHelper.createDrawable(CASTING_BACKGROUND, 117, 16, 16, 16)
            ),
            new CastingCategory(
                guiHelper,
                CASTING_TABLE_TYPE,
                Component.translatable("jei.tinker_foundry.casting.table"),
                TFItems.SEARED_TABLE.get(),
                guiHelper.createDrawable(CASTING_BACKGROUND, 117, 0, 16, 16)
            ),
            new MoldingCategory(guiHelper),
            new FuelCategory(guiHelper),
            new EntityMeltingCategory(guiHelper)
        );
    }

    /** 从客户端配方管理器注册服务端同步下来的全部冶炼配方。 */
    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // 26.1.2 的服务端配方通过 NeoForge 同步到客户端，JEI 使用同步后的 RecipeMap。
        LoadedRecipes recipes = loadFoundryRecipes();
        registration.addRecipes(MELTING_TYPE, recipes.melting());
        registration.addRecipes(ALLOYING_TYPE, recipes.alloying());
        registration.addRecipes(CASTING_BASIN_TYPE, recipes.castingBasin());
        registration.addRecipes(CASTING_TABLE_TYPE, recipes.castingTable());
        registration.addRecipes(MOLDING_TYPE, recipes.molding());
        registration.addRecipes(FUEL_TYPE, recipes.fuel());
        registration.addRecipes(ENTITY_MELTING_TYPE, recipes.entityMelting());
        JEI_SOLID_FUELS = recipes.solidFuels();
        JEI_FLUID_FUELS = recipes.fluidFuels();
        JEI_SOLID_FUEL_TEMPERATURE = recipes.solidFuelTemperature();
        JEI_RECIPES_REGISTERED = !recipes.isEmpty();
        TinkerFoundry.LOGGER.info("[jei] Loaded foundry recipes: melting={}, alloying={}, basin={}, table={}, molding={}, fuel={}, entity_melting={}",
            recipes.melting().size(), recipes.alloying().size(), recipes.castingBasin().size(), recipes.castingTable().size(),
            recipes.molding().size(), recipes.fuel().size(), recipes.entityMelting().size());
    }

    /** 读取服务端同步到当前客户端的本模组配方，并按 JEI 分类拆分。 */
    private static LoadedRecipes loadFoundryRecipes() {
        LoadedRecipes loaded = new LoadedRecipes();
        RecipeMap recipeMap = FoundryRecipeSyncClient.recipeMap();
        int foundryCount = 0;
        for (RecipeHolder<?> holder : recipeMap.values()) {
            Identifier id = holder.id().identifier();
            if (!TinkerFoundry.MOD_ID.equals(id.getNamespace())) continue;
            foundryCount++;
            loaded.add(id, holder.value());
        }
        TinkerFoundry.LOGGER.debug("[jei] Read synchronized recipes: total={}, foundry={}", recipeMap.values().size(), foundryCount);
        return loaded;
    }

    /** JEI runtime 可用后接入同步回调，覆盖配方先于服务端登录到达的情况。 */
    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        JEI_RUNTIME = runtime;
        FoundryRecipeSyncClient.setRecipeMapListener(FoundryJeiPlugin::onRecipeMapChanged);
    }

    /** JEI runtime 失效时解除可选依赖状态。 */
    @Override
    public void onRuntimeUnavailable() {
        JEI_RUNTIME = null;
        JEI_RECIPES_REGISTERED = false;
        FoundryRecipeSyncClient.setRecipeMapListener(null);
    }

    /** 服务端配方到达后，把此前因空缓存跳过的配方追加到 JEI。 */
    private static void onRecipeMapChanged(RecipeMap recipeMap) {
        if (recipeMap.values().isEmpty()) {
            JEI_RECIPES_REGISTERED = false;
            return;
        }
        if (JEI_RUNTIME == null || JEI_RECIPES_REGISTERED) return;
        LoadedRecipes recipes = loadFoundryRecipes();
        addRuntimeRecipes(JEI_RUNTIME.getRecipeManager(), recipes);
        JEI_SOLID_FUELS = recipes.solidFuels();
        JEI_FLUID_FUELS = recipes.fluidFuels();
        JEI_SOLID_FUEL_TEMPERATURE = recipes.solidFuelTemperature();
        JEI_RECIPES_REGISTERED = !recipes.isEmpty();
        TinkerFoundry.LOGGER.info("[jei] Added synchronized foundry recipes: melting={}, alloying={}, basin={}, table={}, molding={}, fuel={}, entity_melting={}",
            recipes.melting().size(), recipes.alloying().size(), recipes.castingBasin().size(), recipes.castingTable().size(),
            recipes.molding().size(), recipes.fuel().size(), recipes.entityMelting().size());
    }

    /** 使用 JEI runtime 管理器追加所有自定义分类配方。 */
    private static void addRuntimeRecipes(IRecipeManager manager, LoadedRecipes recipes) {
        manager.addRecipes(MELTING_TYPE, recipes.melting());
        manager.addRecipes(ALLOYING_TYPE, recipes.alloying());
        manager.addRecipes(CASTING_BASIN_TYPE, recipes.castingBasin());
        manager.addRecipes(CASTING_TABLE_TYPE, recipes.castingTable());
        manager.addRecipes(MOLDING_TYPE, recipes.molding());
        manager.addRecipes(FUEL_TYPE, recipes.fuel());
        manager.addRecipes(ENTITY_MELTING_TYPE, recipes.entityMelting());
    }

    /** 暂存资源包配方及燃料展示信息，最后以不可变列表交给 JEI。 */
    private static final class LoadedRecipes {
        private final List<MeltingRecipe> melting = new ArrayList<>();
        private final List<AlloyingRecipe> alloying = new ArrayList<>();
        private final List<CastingRecipe> castingBasin = new ArrayList<>();
        private final List<CastingRecipe> castingTable = new ArrayList<>();
        private final List<MoldingRecipe> molding = new ArrayList<>();
        private final List<FuelRecipe> fuel = new ArrayList<>();
        private final List<EntityMeltingRecipe> entityMelting = new ArrayList<>();
        private final Map<Identifier, ItemStack> solidFuels = new LinkedHashMap<>();
        private final Map<Identifier, FluidStack> fluidFuels = new LinkedHashMap<>();
        private int solidFuelTemperature;

        /** 判断当前同步表是否没有任何本模组配方。 */
        private boolean isEmpty() {
            return melting.isEmpty() && alloying.isEmpty() && castingBasin.isEmpty() && castingTable.isEmpty()
                && molding.isEmpty() && fuel.isEmpty() && entityMelting.isEmpty();
        }

        /** 将解码出的自定义配方放入对应 JEI 分类，矿石和耐久配方复用熔炼布局展示。 */
        private void add(Identifier id, net.minecraft.world.item.crafting.Recipe<?> recipe) {
            if (recipe instanceof MeltingRecipe value) {
                melting.add(value);
            } else if (recipe instanceof OreMeltingRecipe value) {
                melting.add(new MeltingRecipe(value.ingredient(), value.result(), value.temperature(), value.time(), value.rate(), value.byproducts()));
            } else if (recipe instanceof DamageableMeltingRecipe value) {
                melting.add(new MeltingRecipe(value.ingredient(), value.result(), value.temperature(), value.time(), "none", value.byproducts()));
            } else if (recipe instanceof AlloyingRecipe value) {
                alloying.add(value);
            } else if (recipe instanceof CastingRecipe value) {
                (value.basin() ? castingBasin : castingTable).add(value);
            } else if (recipe instanceof MoldingRecipe value) {
                molding.add(value);
            } else if (recipe instanceof FuelRecipe value) {
                fuel.add(value);
                value.itemFuel().ifPresent(ingredient -> ingredient.items().forEach(holder -> {
                    ItemStack stack = new ItemStack(holder.value());
                    solidFuels.putIfAbsent(BuiltInRegistries.ITEM.getKey(holder.value()), stack);
                }));
                value.fluidFuel().ifPresent(ingredient -> ingredient.fluids().forEach(holder -> {
                    FluidStack stack = new FluidStack(holder, 1);
                    fluidFuels.putIfAbsent(BuiltInRegistries.FLUID.getKey(holder.value()), stack);
                }));
                solidFuelTemperature = Math.max(solidFuelTemperature, value.temperature());
            } else if (recipe instanceof EntityMeltingRecipe value) {
                entityMelting.add(value);
            } else {
                TinkerFoundry.LOGGER.debug("[jei] Ignored unsupported foundry recipe {} of type {}", id, recipe.getClass().getName());
            }
        }

        /** 返回熔炼分类配方。 */
        private List<MeltingRecipe> melting() { return List.copyOf(melting); }
        /** 返回合金分类配方。 */
        private List<AlloyingRecipe> alloying() { return List.copyOf(alloying); }
        /** 返回铸造盆分类配方。 */
        private List<CastingRecipe> castingBasin() { return List.copyOf(castingBasin); }
        /** 返回铸件台分类配方。 */
        private List<CastingRecipe> castingTable() { return List.copyOf(castingTable); }
        /** 返回压模分类配方。 */
        private List<MoldingRecipe> molding() { return List.copyOf(molding); }
        /** 返回燃料分类配方。 */
        private List<FuelRecipe> fuel() { return List.copyOf(fuel); }
        /** 返回实体熔炼分类配方。 */
        private List<EntityMeltingRecipe> entityMelting() { return List.copyOf(entityMelting); }
        /** 返回固体燃料展示栈。 */
        private List<ItemStack> solidFuels() { return List.copyOf(solidFuels.values()); }
        /** 返回流体燃料展示栈。 */
        private List<FluidStack> fluidFuels() { return List.copyOf(fluidFuels.values()); }
        /** 返回可覆盖普通熔炼配方的最高固体燃料温度。 */
        private int solidFuelTemperature() { return solidFuelTemperature; }
    }

    /** 为一个物品 Ingredient 创建带标准背景的 JEI 输入槽。 */
    private static IRecipeSlotBuilder addItemInput(IRecipeLayoutBuilder builder, Ingredient ingredient, int x, int y) {
        return builder.addInputSlot(x, y).addIngredients(ingredient).setStandardSlotBackground();
    }

    /** 为一个 SizedFluidIngredient 创建带官方流体背景的 JEI 输入槽。 */
    private static IRecipeSlotBuilder addFluidInput(IRecipeLayoutBuilder builder, SizedFluidIngredient ingredient, int x, int y, int width, int height) {
        IRecipeSlotBuilder slot = builder.addInputSlot(x, y).setFluidRenderer(FluidValues.BLOCK, false, width, height);
        for (Holder<net.minecraft.world.level.material.Fluid> holder : ingredient.ingredient().fluids()) {
            slot.addFluidStack(holder.value(), ingredient.amount());
        }
        return slot;
    }

    /** 为一个 FluidIngredient 创建可循环显示的 JEI 流体输入槽。 */
    private static IRecipeSlotBuilder addFluidIngredient(IRecipeLayoutBuilder builder, FluidIngredient ingredient, int x, int y, int width, int height) {
        IRecipeSlotBuilder slot = builder.addInputSlot(x, y).setFluidRenderer(FluidValues.BUCKET, true, width, height);
        for (Holder<net.minecraft.world.level.material.Fluid> holder : ingredient.fluids()) {
            slot.addFluidStack(holder.value(), 1);
        }
        return slot;
    }

    /** 为输出物品创建带官方输出背景的 JEI 输出槽。 */
    private static IRecipeSlotBuilder addItemOutput(IRecipeLayoutBuilder builder, ItemStack result, int x, int y) {
        return builder.addOutputSlot(x, y).addItemStack(result).setOutputSlotBackground();
    }

    /** 为输出流体创建带官方流体背景的 JEI 输出槽。 */
    private static IRecipeSlotBuilder addFluidOutput(IRecipeLayoutBuilder builder, FluidStack result, int x, int y, int width, int height) {
        return builder.addOutputSlot(x, y)
            .setFluidRenderer(FluidValues.BLOCK, false, width, height)
            .addFluidStack(result.getFluid(), result.getAmount());
    }

    /** 六类配方共享官方背景绘制和 JEI 分类基础属性。 */
    private abstract static class FoundryCategory<T extends Recipe<?>> extends AbstractRecipeCategory<T> {
        /** 当前分类背景。 */
        private final IDrawable background;

        /** 创建一个带独立图标、尺寸和背景的分类。 */
        private FoundryCategory(RecipeType<T> type, Component title, IDrawable icon, IDrawable background, int width, int height) {
            super(type, title, icon, width, height);
            this.background = background;
        }

        /** 返回官方背景图集中的当前分类区域。 */
        public IDrawable getBackground() {
            return background;
        }

        /** 绘制分类背景，避免把普通设备 GUI 当成 JEI 背景。 */
        @Override
        public void draw(T recipe, IRecipeSlotsView recipeSlots, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
            background.draw(graphics);
        }
    }

    /** 熔炼配方的 JEI 分类。 */
    private static final class MeltingCategory extends FoundryCategory<MeltingRecipe> {
        /** 熔炼输出流体的官方罐体覆盖层。 */
        private final IDrawable tankOverlay;
        /** 固体燃料输入的官方背景切片。 */
        private final IDrawable solidFuel;
        /** 创建熔炼分类。 */
        private MeltingCategory(IGuiHelper guiHelper) {
            super(
                MELTING_TYPE,
                Component.translatable("category.tinker_foundry.melting"),
                guiHelper.createDrawableItemLike(TFItems.SEARED_MELTER.get()),
                guiHelper.createDrawable(MELTING_BACKGROUND, 0, 0, 132, 40),
                132,
                40
            );
            this.tankOverlay = guiHelper.createDrawable(MELTING_BACKGROUND, 132, 0, 32, 32);
            this.solidFuel = guiHelper.createDrawable(MELTING_BACKGROUND, 164, 0, 18, 20);
        }

        /** 显示物品输入和熔融流体输出。 */
        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, MeltingRecipe recipe, IFocusGroup focuses) {
            addItemInput(builder, recipe.ingredient(), 24, 18).setSlotName("ingredient");
            addFluidOutput(builder, recipe.output(false), 96, 4, 32, 32)
                .setOverlay(tankOverlay, 0, 0)
                .setSlotName("result")
                // 副产物写进产物槽悬停提示
                .addRichTooltipCallback((slot, tooltip) -> recipe.byproductOutputs().forEach(byproduct -> tooltip.add(
                    Component.translatable("jei.tinker_foundry.melting.byproduct", byproduct.getHoverName(), byproduct.getAmount()))));
            // 按上游页面把可用固体燃料绘制在左下角，避免温度文字覆盖燃料栏。
            int fuelHeight = 32;
            if (recipe.temperature() <= JEI_SOLID_FUEL_TEMPERATURE && !JEI_SOLID_FUELS.isEmpty()) {
                fuelHeight = 15;
                builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 2, 22)
                    .setBackground(solidFuel, -1, -3)
                    .addItemStacks(JEI_SOLID_FUELS);
            }
            // 流体燃料使用左侧竖向燃料栏，温度不足时由固体燃料槽占据下段。
            builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 4, 4)
                .setFluidRenderer(1, false, 12, fuelHeight)
                .addIngredients(NeoForgeTypes.FLUID_STACK, JEI_FLUID_FUELS);
        }

        /** 显示与熔炼时间和温度相关的布局元素。 */
        @Override
        public void createRecipeExtras(IRecipeExtrasBuilder builder, MeltingRecipe recipe, IFocusGroup focuses) {
            // 箭头动画表达冷却时间；上游页面不再额外绘制“几秒”，避免压住燃料栏。
            builder.addAnimatedRecipeArrow(Math.max(1, recipe.time() * 5)).setPosition(56, 18);
            // 温度居中绘制在燃料栏和产物槽之间，保持 1.20.1 的文字层级和宽度。
            builder.addText(Component.translatable("jei.tinker_foundry.temperature", recipe.temperature()), 113, 9)
                .setPosition(0, 3)
                .setColor(Color.GRAY.getRGB())
                .setTextAlignment(HorizontalAlignment.CENTER);
        }
    }

    /** 合金配方的 JEI 分类。 */
    private static final class AlloyingCategory extends FoundryCategory<AlloyingRecipe> {
        /** 合金输入流体槽的官方覆盖层。 */
        private final IDrawable tankOverlay;
        /** 合金输出流体槽的官方覆盖层。 */
        private final IDrawable outputOverlay;
        /** 合金动画箭头图标。 */
        private final IDrawable alloyArrow;

        /** 创建合金分类。 */
        private AlloyingCategory(IGuiHelper guiHelper) {
            super(
                ALLOYING_TYPE,
                Component.translatable("category.tinker_foundry.alloying"),
                guiHelper.createDrawableItemLike(TFItems.SCORCHED_ALLOYER.get()),
                guiHelper.createDrawable(ALLOY_BACKGROUND, 0, 0, 172, 62),
                172,
                62
            );
            this.tankOverlay = guiHelper.createDrawable(ALLOY_BACKGROUND, 172, 17, 16, 16);
            this.outputOverlay = guiHelper.createDrawable(ALLOY_BACKGROUND, 172, 17, 16, 16);
            this.alloyArrow = guiHelper.drawableBuilder(ALLOY_BACKGROUND, 172, 0, 24, 17).buildAnimated(20, StartDirection.LEFT, false);
        }

        /** 显示多个流体输入和合金流体输出。 */
        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, AlloyingRecipe recipe, IFocusGroup focuses) {
            for (int index = 0; index < recipe.ingredients().size(); index++) {
                AlloyingRecipe.AlloyIngredient ingredient = recipe.ingredients().get(index);
                int x = 19 + (index % 2) * 24;
                int y = 11 + (index / 2) * 18;
                addFluidInput(builder, ingredient.ingredient(), x, y, 16, 32)
                    .setOverlay(tankOverlay, 0, 0)
                    .setSlotName("ingredient_" + index);
            }
            addFluidOutput(builder, recipe.result(), 137, 11, 16, 32)
                .setOverlay(outputOverlay, 0, 0)
                .setSlotName("result");
        }

        /** 显示合金箭头和所需温度。 */
        @Override
        public void createRecipeExtras(IRecipeExtrasBuilder builder, AlloyingRecipe recipe, IFocusGroup focuses) {
            builder.addDrawable(alloyArrow).setPosition(90, 21);
            // 温度文字放在输入槽右侧的空白区，避免覆盖上排流体槽和产物槽。
            builder.addText(Component.translatable("jei.tinker_foundry.temperature", recipe.temperature()), 72, 9).setPosition(65, 5);
        }
    }

    /** 铸造盆和铸件台共用的浇注配方分类。 */
    private static final class CastingCategory extends FoundryCategory<CastingRecipe> {
        /** 输入流体槽的官方罐体覆盖层。 */
        private final IDrawable tankOverlay;
        /** 浇注设备图标。 */
        private final IDrawable castingDevice;
        /** 浇注动画箭头图标。 */
        private final IGuiHelper guiHelper;

        /** 创建铸造盆或铸件台分类。 */
        private CastingCategory(IGuiHelper guiHelper, RecipeType<CastingRecipe> type, Component title, ItemLike icon, IDrawable castingDevice) {
            super(
                type,
                title,
                guiHelper.createDrawableItemLike(icon),
                guiHelper.createDrawable(CASTING_BACKGROUND, 0, 0, 117, 54),
                117,
                54
            );
            this.guiHelper = guiHelper;
            this.castingDevice = castingDevice;
            this.tankOverlay = guiHelper.createDrawable(CASTING_BACKGROUND, 133, 0, 32, 32);
        }

        /** 显示流体输入、可选可复用铸模和物品输出。 */
        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, CastingRecipe recipe, IFocusGroup focuses) {
            recipe.mold().ifPresent(mold -> builder.addSlot(RecipeIngredientRole.CRAFTING_STATION, 38, 19)
                .addIngredients(mold)
                .setStandardSlotBackground()
                .setSlotName("mold"));
            if (!recipe.copyPotionContents()) {
                addFluidInput(builder, recipe.fluid(), 3, 3, 32, 32)
                    .setOverlay(tankOverlay, 0, 0)
                    .setSlotName("fluid");
                addItemOutput(builder, recipe.result(), 93, 18).setSlotName("result");
                return;
            }
            // 药水浇注按全部原版药水展开，流体与产物一一对应。
            List<FluidStack> fluids = new ArrayList<>();
            List<ItemStack> results = new ArrayList<>();
            BuiltInRegistries.POTION.listElements().forEach(potion -> {
                PotionContents contents = new PotionContents(potion);
                for (Holder<net.minecraft.world.level.material.Fluid> holder : recipe.fluid().ingredient().fluids()) {
                    FluidStack stack = new FluidStack(holder.value(), recipe.fluid().amount());
                    stack.set(DataComponents.POTION_CONTENTS, contents);
                    fluids.add(stack);
                    ItemStack result = recipe.result();
                    result.set(DataComponents.POTION_CONTENTS, contents);
                    results.add(result);
                }
            });
            // 流体槽与产物槽建立焦点联动，保证轮播时两边是同一种药水。
            IRecipeSlotBuilder fluidSlot = builder.addInputSlot(3, 3)
                .setFluidRenderer(FluidValues.BLOCK, false, 32, 32)
                .addIngredients(NeoForgeTypes.FLUID_STACK, fluids)
                .addRichTooltipCallback((slot, tooltip) -> slot.getDisplayedIngredient(NeoForgeTypes.FLUID_STACK)
                    .map(stack -> stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY))
                    .filter(contents -> contents.potion().isPresent())
                    .ifPresent(contents -> {
                        Component potionName = contents.getName("item.minecraft.potion.effect.");
                        List<Either<net.minecraft.network.chat.FormattedText, net.minecraft.world.inventory.tooltip.TooltipComponent>> lines = tooltip.getLines();
                        if (!lines.isEmpty()) lines.set(0, Either.left(potionName));
                        else tooltip.add(potionName);
                    }))
                .setOverlay(tankOverlay, 0, 0)
                .setSlotName("fluid");
            IRecipeSlotBuilder resultSlot = builder.addOutputSlot(93, 18)
                .addItemStacks(results)
                .setOutputSlotBackground()
                .setSlotName("result");
            builder.createFocusLink(fluidSlot, resultSlot);
        }

        /** 显示设备图标、浇注动画和铸模不消耗语义。 */
        @Override
        public void createRecipeExtras(IRecipeExtrasBuilder builder, CastingRecipe recipe, IFocusGroup focuses) {
            builder.addDrawable(castingDevice).setPosition(38, 35);
            IDrawable arrow = guiHelper.drawableBuilder(CASTING_BACKGROUND, 117, 32, 24, 17)
                .buildAnimated(Math.max(1, recipe.time()), StartDirection.LEFT, false);
            builder.addDrawable(arrow).setPosition(58, 18);
            if (recipe.mold().isPresent()) {
                IDrawable castState = guiHelper.createDrawable(CASTING_BACKGROUND, 141, 43, 13, 11);
                builder.addDrawable(castState)
                    .setPosition(63, 39);
            }
        }
    }

    /** 模具配方的 JEI 分类。 */
    private static final class MoldingCategory extends FoundryCategory<MoldingRecipe> {
        /** 当前分类使用的 JEI 图形助手，用于按配方冷却时间创建箭头动画。 */
        private final IGuiHelper guiHelper;
        /** 流体输入槽的官方罐体覆盖层。 */
        private final IDrawable tankOverlay;
        /** 铸件台装饰图标。 */
        private final IDrawable castingTable;
        /** 模具消耗状态图标。 */
        private final IDrawable castConsumed;
        /** 模具保留状态图标。 */
        private final IDrawable castKept;

        /** 创建压模分类。 */
        private MoldingCategory(IGuiHelper guiHelper) {
            super(
                MOLDING_TYPE,
                Component.translatable("jei.tinker_foundry.molding.title"),
                guiHelper.createDrawableItemLike(TFItems.INGOT_SAND_CAST.get()),
                guiHelper.createDrawable(CASTING_BACKGROUND, 0, 0, 117, 54),
                117,
                54
            );
            // 保存图形助手，使每条配方都能使用自己的冷却时间创建箭头动画。
            this.guiHelper = guiHelper;
            this.tankOverlay = guiHelper.createDrawable(CASTING_BACKGROUND, 133, 0, 32, 32);
            this.castingTable = guiHelper.createDrawable(CASTING_BACKGROUND, 117, 0, 16, 16);
            this.castConsumed = guiHelper.createDrawable(CASTING_BACKGROUND, 141, 32, 13, 11);
            this.castKept = guiHelper.createDrawable(CASTING_BACKGROUND, 141, 43, 13, 11);
        }

        /** 按匠魂铸造卡片的三段式位置显示流体、模具和物品产物。 */
        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, MoldingRecipe recipe, IFocusGroup focuses) {
            // 模具消耗时作为输入，不消耗时作为催化剂，保持 JEI 查询语义与实际配方一致。
            RecipeIngredientRole moldRole = recipe.patternConsumed() ? RecipeIngredientRole.INPUT : RecipeIngredientRole.CRAFTING_STATION;
            IRecipeSlotBuilder mold = builder.addSlot(moldRole, 38, 19)
                .addIngredients(recipe.mold())
                .setSlotName("mold");
            if (recipe.patternConsumed()) {
                mold.addRichTooltipCallback((slot, tooltip) -> tooltip.add(Component.translatable("jei.tinker_foundry.molding.pattern_consumed")));
            }
            // 流体槽使用匠魂铸造卡片的左侧 32x32 罐体区域。
            addFluidInput(builder, recipe.fluid(), 3, 3, 32, 32)
                .setOverlay(tankOverlay, 0, 0)
                .setSlotName("fluid");
            // 物品产物使用右侧输出槽，避免与中间模具和箭头重叠。
            addItemOutput(builder, recipe.result(), 93, 18).setSlotName("result");
        }

        /** 显示匠魂铸造卡片的时间、箭头、设备和模具消耗状态。 */
        @Override
        public void createRecipeExtras(IRecipeExtrasBuilder builder, MoldingRecipe recipe, IFocusGroup focuses) {
            // 时间文字沿用匠魂的秒数显示，并放在卡片顶部中央。
            builder.addText(Component.translatable("jei.tinker_foundry.time", recipe.time() / 20), 144, 9)
                .setPosition(0, 2)
                .setColor(Color.GRAY.getRGB())
                .setTextAlignment(HorizontalAlignment.CENTER);
            // 箭头使用官方铸造背景中的 24x17 动画切片。
            IDrawable arrow = guiHelper.drawableBuilder(CASTING_BACKGROUND, 117, 32, 24, 17)
                .buildAnimated(Math.max(1, recipe.time()), StartDirection.LEFT, false);
            builder.addDrawable(arrow).setPosition(58, 18);
            // 使用单个居中的铸件台图标，使模具槽正对设备，匹配匠魂铸造卡片。
            builder.addDrawable(castingTable).setPosition(38, 35);
            // 用官方状态图标表达模具是否消耗。
            builder.addDrawable(recipe.patternConsumed() ? castConsumed : castKept)
                .setPosition(63, 39);
        }
    }

    /** 生物熔炼配方的 JEI 分类，展示实体条件、伤害和流体结果。 */
    private static final class EntityMeltingCategory extends FoundryCategory<EntityMeltingRecipe> {
        /** 实体熔炼专用的动画箭头。 */
        private final IDrawable arrow;
        /** 实体熔炼下方燃料槽的罐体覆盖层。 */
        private final IDrawable fuelTank;
        /** 创建生物熔炼分类。 */
        private EntityMeltingCategory(IGuiHelper guiHelper) {
            super(
                ENTITY_MELTING_TYPE,
                Component.translatable("category.tinker_foundry.entity_melting"),
                guiHelper.createDrawable(MELTING_BACKGROUND, 174, 41, 16, 16),
                guiHelper.createDrawable(MELTING_BACKGROUND, 0, 41, 150, 62),
                150,
                62
            );
            // 复用 1.20.1 实体熔炼背景图集中的箭头和燃料罐切片。
            this.arrow = guiHelper.drawableBuilder(MELTING_BACKGROUND, 150, 41, 24, 17)
                .buildAnimated(200, StartDirection.LEFT, false);
            this.fuelTank = guiHelper.createDrawable(MELTING_BACKGROUND, 150, 74, 16, 16);
        }

        /** 展示实体输入、熔融流体结果和可用燃料。 */
        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, EntityMeltingRecipe recipe, IFocusGroup focuses) {
            // 使用实体标签解析出的刷怪蛋作为 JEI 可查询输入，避免把标签文字挤进配方卡片。
            List<ItemStack> entities = entityDisplayStacks(recipe);
            // 刷怪蛋使用 18x18 物品槽，水平居中放入左侧 32x32 熔炉腔体，避免贴在左墙边。
            builder.addInputSlot(26, 11)
                .addItemStacks(entities)
                .setSlotName("entity");
            // 右侧输出使用窄型流体槽，背景中的砖墙和液面由实体熔炼专用切片提供。
            builder.addOutputSlot(115, 11)
                .setFluidRenderer(FluidValues.INGOT * 2L, false, 16, 32)
                .addIngredient(NeoForgeTypes.FLUID_STACK, recipe.result())
                // JEI 默认只显示流体类型；药水流体需要把组件中的具体药水名称写回第一行提示。
                .addRichTooltipCallback((slot, tooltip) -> {
                    PotionContents potionContents = recipe.result().getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
                    if (potionContents.potion().isPresent()) {
                        Component potionName = potionContents.getName("item.minecraft.potion.effect.");
                        List<Either<net.minecraft.network.chat.FormattedText, net.minecraft.world.inventory.tooltip.TooltipComponent>> lines = tooltip.getLines();
                        if (!lines.isEmpty()) {
                            lines.set(0, Either.left(potionName));
                        } else {
                            tooltip.add(potionName);
                        }
                    }
                })
                .setSlotName("result");
            // 下方显示当前同步燃料配方中的流体燃料，不把燃料逻辑硬编码到 JEI 分类。
            builder.addSlot(RecipeIngredientRole.CRAFTING_STATION, 75, 43)
                .setFluidRenderer(1, false, 16, 16)
                .setOverlay(fuelTank, 0, 0)
                .addIngredients(NeoForgeTypes.FLUID_STACK, JEI_FLUID_FUELS)
                .setSlotName("fuel");
        }

        /** 显示实体熔炼箭头和每两点伤害对应的一颗心。 */
        @Override
        public void createRecipeExtras(IRecipeExtrasBuilder builder, EntityMeltingRecipe recipe, IFocusGroup focuses) {
            builder.addDrawable(arrow).setPosition(71, 21);
            builder.addText(Component.literal(Float.toString(recipe.damage() / 2.0F)), 84, 9)
                .setPosition(0, 8)
                .setColor(Color.RED.getRGB())
                .setTextAlignment(HorizontalAlignment.RIGHT);
        }

        /** 将实体列表和实体标签解析为可查询的刷怪蛋图标。 */
        private static List<ItemStack> entityDisplayStacks(EntityMeltingRecipe recipe) {
            List<ItemStack> stacks = new ArrayList<>();
            for (String entityId : recipe.entities()) {
                addEntitySpawnEgg(stacks, Identifier.tryParse(entityId));
            }
            if (!recipe.entityTag().isEmpty()) {
                Identifier tagId = Identifier.tryParse(recipe.entityTag());
                if (tagId != null) {
                    TagKey<EntityType<?>> tag = TagKey.create(Registries.ENTITY_TYPE, tagId);
                    for (Holder<EntityType<?>> holder : BuiltInRegistries.ENTITY_TYPE.getTagOrEmpty(tag)) {
                        addEntitySpawnEgg(stacks, BuiltInRegistries.ENTITY_TYPE.getKey(holder.value()));
                    }
                }
            }
            return stacks;
        }

        /** 将注册表中的实体类型转换为刷怪蛋，无法转换时跳过而不伪造图标。 */
        private static void addEntitySpawnEgg(List<ItemStack> stacks, Identifier entityId) {
            if (entityId == null) return;
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(entityId).orElse(null);
            if (type == null) return;
            SpawnEggItem.byId(type).ifPresent(holder -> stacks.add(new ItemStack(holder.value())));
        }
    }

    /** 燃料配方的 JEI 分类。 */
    private static final class FuelCategory extends FoundryCategory<FuelRecipe> {
        /** 流体燃料栏的官方背景切片。 */
        private final IDrawable fuelBar;

        /** 创建燃料分类。 */
        private FuelCategory(IGuiHelper guiHelper) {
            super(
                FUEL_TYPE,
                Component.translatable("category.tinker_foundry.fuel"),
                guiHelper.createDrawableItemLike(TFItems.SEARED_HEATER.get()),
                guiHelper.createBlankDrawable(132, 40),
                132,
                40
            );
            // 燃料分类没有熔炼箭头和产物槽，只复用左侧流体燃料栏。
            this.fuelBar = guiHelper.createDrawable(MELTING_BACKGROUND, 3, 3, 14, 34);
        }

        /** 显示固体或流体燃料输入。 */
        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, FuelRecipe recipe, IFocusGroup focuses) {
            // 固体燃料使用独立的 18x18 输入槽，右侧文字从 x=21 开始，避免槽与文字相撞。
            recipe.itemFuel().ifPresent(item -> addItemInput(builder, item, 2, 12).setSlotName("item_fuel"));
            // 流体只绘制 12x32 内容，并用 14x34 背景切片包住内容，匹配官方燃料栏比例。
            recipe.fluidFuel().ifPresent(fluid -> addFluidIngredient(builder, fluid, 4, 4, 12, 32)
                .setBackground(fuelBar, -1, -1)
                .setSlotName("fluid_fuel"));
        }

        /** 显示燃料温度和持续时间。 */
        @Override
        public void createRecipeExtras(IRecipeExtrasBuilder builder, FuelRecipe recipe, IFocusGroup focuses) {
            // 两行文字固定在燃料槽右侧，避免复用熔炼背景后再次把文字推到错误位置。
            builder.addText(Component.translatable("jei.tinker_foundry.temperature", recipe.temperature()), 111, 9).setPosition(21, 11);
            builder.addText(Component.translatable("jei.tinker_foundry.time", Math.max(1, recipe.duration() / 20)), 111, 9).setPosition(21, 21);
        }
    }

    /** 防止可选插件被错误实例化。 */
    public FoundryJeiPlugin() {
    }
}
