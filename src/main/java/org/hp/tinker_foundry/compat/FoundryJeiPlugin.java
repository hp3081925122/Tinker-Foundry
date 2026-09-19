package org.hp.tinker_foundry.compat;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated.StartDirection;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import org.hp.tinker_foundry.TinkerFoundry;
import org.hp.tinker_foundry.common.FluidValues;
import org.hp.tinker_foundry.recipe.AlloyingRecipe;
import org.hp.tinker_foundry.recipe.CastingRecipe;
import org.hp.tinker_foundry.recipe.FuelRecipe;
import org.hp.tinker_foundry.recipe.MeltingRecipe;
import org.hp.tinker_foundry.recipe.MoldingRecipe;
import org.hp.tinker_foundry.registry.TFItems;
import org.hp.tinker_foundry.registry.TFRecipes;

/** JEI 可选插件；没有 JEI 时本类不会被模组主类加载。 */
@JeiPlugin
public final class FoundryJeiPlugin implements IModPlugin {
    /** JEI 插件自身的稳定标识。 */
    private static final ResourceLocation PLUGIN_ID = ResourceLocation.fromNamespaceAndPath(TinkerFoundry.MOD_ID, "jei_plugin");
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
    /** 官方冶炼链 JEI 分类的独立背景图集。 */
    private static final ResourceLocation CASTING_BACKGROUND = ResourceLocation.fromNamespaceAndPath(TinkerFoundry.MOD_ID, "textures/gui/jei/casting.png");
    /** 官方合金分类的独立背景图集。 */
    private static final ResourceLocation ALLOY_BACKGROUND = ResourceLocation.fromNamespaceAndPath(TinkerFoundry.MOD_ID, "textures/gui/jei/alloy.png");
    /** 官方熔炼和燃料分类的独立背景图集。 */
    private static final ResourceLocation MELTING_BACKGROUND = ResourceLocation.fromNamespaceAndPath(TinkerFoundry.MOD_ID, "textures/gui/jei/melting.png");

    /** 返回 JEI 插件的唯一标识。 */
    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    /** 注册独立背景和布局的六类冶炼配方。 */
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
                TFItems.CASTING_BASIN.get(),
                guiHelper.createDrawable(CASTING_BACKGROUND, 117, 16, 16, 16)
            ),
            new CastingCategory(
                guiHelper,
                CASTING_TABLE_TYPE,
                Component.translatable("jei.tinker_foundry.casting.table"),
                TFItems.CASTING_TABLE.get(),
                guiHelper.createDrawable(CASTING_BACKGROUND, 117, 0, 16, 16)
            ),
            new MoldingCategory(guiHelper),
            new FuelCategory(guiHelper)
        );
    }

    /** 从客户端配方管理器注册服务端同步下来的全部冶炼配方。 */
    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        Minecraft minecraft = Minecraft.getInstance();
        var connection = minecraft.getConnection();
        var recipeManager = connection != null ? connection.getRecipeManager() : minecraft.level == null ? null : minecraft.level.getRecipeManager();
        if (recipeManager == null) {
            // JEI 可能在没有客户端连接的纯菜单初始化阶段回调，不能用空管理器伪造配方。
            TinkerFoundry.LOGGER.warn("[jei] Client recipe manager is unavailable while registering recipes");
            return;
        }
        // 客户端连接中的配方管理器包含服务端同步的数据，保证单机和专用服务器使用同一份配方。
        registration.addRecipes(MELTING_TYPE, recipeManager.getAllRecipesFor(TFRecipes.MELTING.get()).stream().map(RecipeHolder::value).toList());
        registration.addRecipes(ALLOYING_TYPE, recipeManager.getAllRecipesFor(TFRecipes.ALLOYING.get()).stream().map(RecipeHolder::value).toList());
        var castingRecipes = recipeManager.getAllRecipesFor(TFRecipes.CASTING.get()).stream().map(RecipeHolder::value).toList();
        registration.addRecipes(CASTING_BASIN_TYPE, castingRecipes.stream().filter(recipe -> recipe.mold().isEmpty()).toList());
        registration.addRecipes(CASTING_TABLE_TYPE, castingRecipes.stream().filter(recipe -> recipe.mold().isPresent()).toList());
        registration.addRecipes(MOLDING_TYPE, recipeManager.getAllRecipesFor(TFRecipes.MOLDING.get()).stream().map(RecipeHolder::value).toList());
        registration.addRecipes(FUEL_TYPE, recipeManager.getAllRecipesFor(TFRecipes.FUEL.get()).stream().map(RecipeHolder::value).toList());
        TinkerFoundry.LOGGER.info("[jei] Registered foundry recipe categories");
    }

    /** 为一个物品 Ingredient 创建带标准背景的 JEI 输入槽。 */
    private static IRecipeSlotBuilder addItemInput(IRecipeLayoutBuilder builder, Ingredient ingredient, int x, int y) {
        return builder.addInputSlot(x, y).addIngredients(ingredient).setStandardSlotBackground();
    }

    /** 为一个 SizedFluidIngredient 创建带官方流体背景的 JEI 输入槽。 */
    private static IRecipeSlotBuilder addFluidInput(IRecipeLayoutBuilder builder, SizedFluidIngredient ingredient, int x, int y, int width, int height) {
        IRecipeSlotBuilder slot = builder.addInputSlot(x, y).setFluidRenderer(FluidValues.BLOCK, false, width, height);
        for (FluidStack stack : ingredient.getFluids()) {
            slot.addFluidStack(stack.getFluid(), ingredient.amount());
        }
        return slot;
    }

    /** 为一个 FluidIngredient 创建可循环显示的 JEI 流体输入槽。 */
    private static IRecipeSlotBuilder addFluidIngredient(IRecipeLayoutBuilder builder, FluidIngredient ingredient, int x, int y, int width, int height) {
        IRecipeSlotBuilder slot = builder.addInputSlot(x, y).setFluidRenderer(FluidValues.BUCKET, true, width, height);
        for (FluidStack stack : ingredient.getStacks()) {
            slot.addFluidStack(stack.getFluid(), stack.getAmount());
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
        @Override
        public IDrawable getBackground() {
            return background;
        }

        /** 绘制分类背景，避免把普通设备 GUI 当成 JEI 背景。 */
        @Override
        public void draw(T recipe, IRecipeSlotsView recipeSlots, GuiGraphics graphics, double mouseX, double mouseY) {
            background.draw(graphics);
        }
    }

    /** 熔炼配方的 JEI 分类。 */
    private static final class MeltingCategory extends FoundryCategory<MeltingRecipe> {
        /** 熔炼输出流体的官方罐体覆盖层。 */
        private final IDrawable tankOverlay;
        /** 创建熔炼分类。 */
        private MeltingCategory(IGuiHelper guiHelper) {
            super(
                MELTING_TYPE,
                Component.translatable("category.tinker_foundry.melting"),
                guiHelper.createDrawableItemLike(TFItems.MELTER.get()),
                guiHelper.createDrawable(MELTING_BACKGROUND, 0, 0, 132, 40),
                132,
                40
            );
            this.tankOverlay = guiHelper.createDrawable(MELTING_BACKGROUND, 132, 0, 32, 32);
        }

        /** 显示物品输入和熔融流体输出。 */
        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, MeltingRecipe recipe, IFocusGroup focuses) {
            addItemInput(builder, recipe.ingredient(), 24, 18).setSlotName("ingredient");
            addFluidOutput(builder, recipe.result(), 96, 4, 32, 32)
                .setOverlay(tankOverlay, 0, 0)
                .setSlotName("result");
        }

        /** 显示与熔炼时间和温度相关的布局元素。 */
        @Override
        public void createRecipeExtras(IRecipeExtrasBuilder builder, MeltingRecipe recipe, IFocusGroup focuses) {
            builder.addAnimatedRecipeArrowWidget(Math.max(1, recipe.time())).setPosition(56, 18);
            builder.addText(Component.translatable("jei.tinker_foundry.temperature", recipe.temperature()), 132, 9).setPosition(0, 3);
            builder.addText(Component.translatable("jei.tinker_foundry.time", Math.max(1, recipe.time() / 20)), 132, 9).setPosition(0, 29);
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
                guiHelper.createDrawableItemLike(TFItems.ALLOYER.get()),
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
            builder.addDrawableWidget(alloyArrow).setPosition(90, 21);
            builder.addText(Component.translatable("jei.tinker_foundry.temperature", recipe.temperature()), 106, 9).setPosition(33, 5);
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
            recipe.mold().ifPresent(mold -> builder.addSlot(RecipeIngredientRole.CATALYST, 38, 19)
                .addIngredients(mold)
                .setStandardSlotBackground()
                .setSlotName("mold"));
            addFluidInput(builder, recipe.fluid(), 3, 3, 32, 32)
                .setOverlay(tankOverlay, 0, 0)
                .setSlotName("fluid");
            addItemOutput(builder, recipe.result(), 93, 18).setSlotName("result");
        }

        /** 显示设备图标、浇注动画和铸模不消耗语义。 */
        @Override
        public void createRecipeExtras(IRecipeExtrasBuilder builder, CastingRecipe recipe, IFocusGroup focuses) {
            builder.addDrawableWidget(castingDevice).setPosition(38, 35);
            IDrawable arrow = guiHelper.drawableBuilder(CASTING_BACKGROUND, 117, 32, 24, 17)
                .buildAnimated(Math.max(1, recipe.time()), StartDirection.LEFT, false);
            builder.addDrawableWidget(arrow).setPosition(58, 18);
            if (recipe.mold().isPresent()) {
                IDrawable castKept = guiHelper.createDrawable(CASTING_BACKGROUND, 141, 43, 13, 11);
                builder.addDrawableWidget(castKept)
                    .setPosition(63, 39)
                    .setTooltip(Component.translatable("jei.tinker_foundry.casting.cast_kept"));
            }
        }
    }

    /** 模具配方的 JEI 分类。 */
    private static final class MoldingCategory extends FoundryCategory<MoldingRecipe> {
        /** 模具输入的方向提示图标。 */
        private final IDrawable moldArrow;
        /** 流体输入槽的官方罐体覆盖层。 */
        private final IDrawable tankOverlay;
        /** 铸件台装饰图标。 */
        private final IDrawable castingTable;
        /** 铸造盆装饰图标。 */
        private final IDrawable castingBasin;

        /** 创建压模分类。 */
        private MoldingCategory(IGuiHelper guiHelper) {
            super(
                MOLDING_TYPE,
                Component.translatable("jei.tinker_foundry.molding.title"),
                guiHelper.createDrawableItemLike(TFItems.INGOT_SAND_CAST.get()),
                guiHelper.createDrawable(CASTING_BACKGROUND, 0, 0, 70, 57),
                70,
                57
            );
            this.moldArrow = guiHelper.createDrawable(CASTING_BACKGROUND, 70, 55, 6, 10);
            this.tankOverlay = guiHelper.createDrawable(CASTING_BACKGROUND, 133, 0, 32, 32);
            this.castingTable = guiHelper.createDrawable(CASTING_BACKGROUND, 117, 0, 16, 16);
            this.castingBasin = guiHelper.createDrawable(CASTING_BACKGROUND, 117, 16, 16, 16);
        }

        /** 显示模具、流体输入和物品输出。 */
        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, MoldingRecipe recipe, IFocusGroup focuses) {
            addItemInput(builder, recipe.mold(), 3, 1)
                .setSlotName("mold")
                .addRichTooltipCallback((slot, tooltip) -> tooltip.add(Component.translatable("jei.tinker_foundry.molding.pattern_consumed")));
            addFluidInput(builder, recipe.fluid(), 3, 24, 32, 32)
                .setOverlay(tankOverlay, 0, 0)
                .setSlotName("fluid");
            addItemOutput(builder, recipe.result(), 51, 24).setSlotName("result");
        }

        /** 显示模具消耗提示以及可用的铸件台和铸造盆。 */
        @Override
        public void createRecipeExtras(IRecipeExtrasBuilder builder, MoldingRecipe recipe, IFocusGroup focuses) {
            builder.addRecipeArrowWidget().setPosition(24, 23);
            builder.addDrawableWidget(moldArrow).setPosition(8, 17);
            builder.addDrawableWidget(castingTable).setPosition(3, 40);
            builder.addDrawableWidget(castingBasin).setPosition(51, 40);
        }
    }

    /** 燃料配方的 JEI 分类。 */
    private static final class FuelCategory extends FoundryCategory<FuelRecipe> {
        /** 创建燃料分类。 */
        private FuelCategory(IGuiHelper guiHelper) {
            super(
                FUEL_TYPE,
                Component.translatable("category.tinker_foundry.fuel"),
                guiHelper.createDrawableItemLike(TFItems.HEATER.get()),
                guiHelper.createDrawable(MELTING_BACKGROUND, 0, 0, 132, 40),
                132,
                40
            );
        }

        /** 显示固体或流体燃料输入。 */
        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, FuelRecipe recipe, IFocusGroup focuses) {
            recipe.itemFuel().ifPresent(item -> addItemInput(builder, item, 24, 18).setSlotName("item_fuel"));
            recipe.fluidFuel().ifPresent(fluid -> addFluidIngredient(builder, fluid, 4, 4, 16, 32).setSlotName("fluid_fuel"));
        }

        /** 显示燃料温度和持续时间。 */
        @Override
        public void createRecipeExtras(IRecipeExtrasBuilder builder, FuelRecipe recipe, IFocusGroup focuses) {
            builder.addText(Component.translatable("jei.tinker_foundry.temperature", recipe.temperature()), 132, 9).setPosition(0, 3);
            builder.addText(Component.translatable("jei.tinker_foundry.time", Math.max(1, recipe.duration() / 20)), 132, 9).setPosition(0, 29);
        }
    }

    /** 防止可选插件被错误实例化。 */
    public FoundryJeiPlugin() {
    }
}
