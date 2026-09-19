package org.hp.tinker_foundry.client.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.RenderTypeGroup;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.model.CompositeModel;
import net.neoforged.neoforge.client.model.QuadTransformers;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import org.hp.tinker_foundry.item.FoundryTankItem;
import org.joml.Vector3f;
import org.jetbrains.annotations.Nullable;

/** 复刻匠魂储液罐的静态模型、库存模型和物品动态流体层。 */
public final class TankModel implements IUnbakedGeometry<TankModel> {
    /** 注册到当前模组命名空间的几何加载器。 */
    public static final IGeometryLoader<TankModel> LOADER = TankModel::deserialize;

    /** 罐体的静态模型元素。 */
    private final BlockModel model;
    /** 物品栏使用的简化模型元素。 */
    @Nullable
    private final BlockModel gui;
    /** 流体体积的最小坐标。 */
    private final Vector3f fluidFrom;
    /** 流体体积的最大坐标。 */
    private final Vector3f fluidTo;
    /** 流体高度的离散精度。 */
    private final int increments;

    /** 创建一个带静态模型和库存模型的储液罐几何体。 */
    private TankModel(BlockModel model, @Nullable BlockModel gui, Vector3f fluidFrom, Vector3f fluidTo, int increments) {
        this.model = model;
        this.gui = gui;
        this.fluidFrom = fluidFrom;
        this.fluidTo = fluidTo;
        this.increments = increments;
    }

    /** 让模型自身和库存模型解析普通父模型。 */
    @Override
    public void resolveParents(Function<ResourceLocation, net.minecraft.client.resources.model.UnbakedModel> modelGetter,
                               IGeometryBakingContext context) {
        model.resolveParents(modelGetter);
        if (gui != null) {
            gui.resolveParents(modelGetter);
        }
    }

    /** 烘焙静态罐体，并挂接读取物品流体能力的动态覆盖。 */
    @Override
    public BakedModel bake(IGeometryBakingContext context, ModelBaker baker,
                           Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState,
                           ItemOverrides overrides) {
        BakedModel base = bakeLayer(context, model, spriteGetter, modelState, ItemOverrides.EMPTY);
        BakedModel guiModel = gui == null ? base : bakeLayer(context, gui, spriteGetter, modelState, ItemOverrides.EMPTY);
        FluidOverrides fluidOverrides = new FluidOverrides();
        Baked baked = new Baked(context, baker, spriteGetter, modelState, base, guiModel,
            fluidOverrides, ImmutableMap.of("base", base), ImmutableList.of(base));
        fluidOverrides.owner = baked;
        return baked;
    }

    /** 使用外层方块模型的纹理别名烘焙一层，避免父模板丢失子模型的 side 和 top。 */
    private BakedModel bakeLayer(IGeometryBakingContext context, BlockModel source,
                                 Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState,
                                 ItemOverrides overrides) {
        TextureAtlasSprite particle = spriteGetter.apply(context.getMaterial("particle"));
        SimpleBakedModel.Builder builder = new SimpleBakedModel.Builder(
            context.useAmbientOcclusion(), context.useBlockLight(), context.isGui3d(),
            context.getTransforms(), overrides).particle(particle);
        for (BlockElement element : source.getElements()) {
            for (Map.Entry<Direction, BlockElementFace> entry : element.faces.entrySet()) {
                Direction direction = entry.getKey();
                BlockElementFace face = entry.getValue();
                TextureAtlasSprite sprite = spriteGetter.apply(context.getMaterial(face.texture()));
                var quad = BlockModel.bakeFace(element, face, sprite, direction, modelState);
                if (face.cullForDirection() == null) {
                    builder.addUnculledFace(quad);
                } else {
                    builder.addCulledFace(Direction.rotate(modelState.getRotation().getMatrix(), face.cullForDirection()), quad);
                }
            }
        }
        return builder.build(getRenderTypes(context));
    }

    /** 读取当前模型声明的方块和物品渲染层。 */
    private RenderTypeGroup getRenderTypes(IGeometryBakingContext context) {
        ResourceLocation hint = context.getRenderTypeHint();
        return hint == null ? RenderTypeGroup.EMPTY : context.getRenderType(hint);
    }

    /** 从模型 JSON 中读取普通元素、库存元素和可缩放流体体积。 */
    public static TankModel deserialize(JsonObject json, JsonDeserializationContext context) {
        JsonObject baseJson = json.deepCopy();
        baseJson.remove("loader");
        baseJson.remove("fluid");
        baseJson.remove("gui");
        BlockModel model = BlockModel.fromString(baseJson.toString());

        BlockModel gui = null;
        if (json.has("gui")) {
            JsonObject guiJson = GsonHelper.getAsJsonObject(json, "gui").deepCopy();
            if (json.has("textures")) {
                guiJson.add("textures", json.getAsJsonObject("textures").deepCopy());
            }
            gui = BlockModel.fromString(guiJson.toString());
        }

        JsonObject fluidJson = GsonHelper.getAsJsonObject(json, "fluid");
        Vector3f from = readVector(GsonHelper.getAsJsonArray(fluidJson, "from"));
        Vector3f to = readVector(GsonHelper.getAsJsonArray(fluidJson, "to"));
        int increments = Math.max(1, GsonHelper.getAsInt(fluidJson, "increments"));
        return new TankModel(model, gui, from, to, increments);
    }

    /** 将三元坐标数组转换为模型坐标。 */
    private static Vector3f readVector(com.google.gson.JsonArray array) {
        if (array.size() != 3) {
            throw new IllegalArgumentException("Tank fluid coordinates must contain three values");
        }
        return new Vector3f(array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat());
    }

    /** 处理物品栏中不同流体和不同储量对应的模型缓存。 */
    private final class FluidOverrides extends ItemOverrides {
        /** 初始模型完成构造后回填，避免在父类构造阶段访问未完成对象。 */
        @Nullable
        private Baked owner;

        /** 根据物品流体能力返回带有对应流体高度的烘焙模型。 */
        @Override
        public BakedModel resolve(BakedModel originalModel, ItemStack stack, @Nullable ClientLevel level,
                                  @Nullable LivingEntity entity, int seed) {
            if (!(stack.getItem() instanceof FoundryTankItem tankItem)) {
                return originalModel;
            }
            return FluidUtil.getFluidContained(stack)
                .filter(fluid -> !fluid.isEmpty())
                .map(fluid -> owner == null ? originalModel : owner.getFluidModel(fluid, tankItem.capacity()))
                .orElse(originalModel);
        }
    }

    /** 保存一个可缓存的流体类型和离散液面高度。 */
    private record FluidKey(net.minecraft.world.level.material.Fluid fluid, int level) {
    }

    /** 带有 GUI 专用模型和动态物品覆盖的烘焙储液罐。 */
    private final class Baked extends CompositeModel.Baked {
        /** 当前储液罐几何定义。 */
        private final IGeometryBakingContext context;
        /** 用于动态流体纹理的模型烘焙器。 */
        private final ModelBaker baker;
        /** 用于解析流体纹理材质的纹理函数。 */
        private final Function<Material, TextureAtlasSprite> spriteGetter;
        /** 当前方块或物品变换。 */
        private final ModelState modelState;
        /** GUI 中使用的独立模型。 */
        private final BakedModel guiModel;
        /** 当前模型中的静态主体层。 */
        private final BakedModel baseModel;
        /** 已按流体类型和液面高度缓存的动态模型。 */
        private final Map<FluidKey, BakedModel> fluidModels = new HashMap<>();

        /** 创建一个静态或动态储液罐烘焙模型。 */
        private Baked(IGeometryBakingContext context, ModelBaker baker,
                      Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState,
                      BakedModel baseModel, BakedModel guiModel, ItemOverrides overrides,
                      ImmutableMap<String, BakedModel> children, ImmutableList<BakedModel> itemPasses) {
            super(context.isGui3d(), context.useBlockLight(), context.useAmbientOcclusion(),
                baseModel.getParticleIcon(), context.getTransforms(), overrides, children, itemPasses);
            this.context = context;
            this.baker = baker;
            this.spriteGetter = spriteGetter;
            this.modelState = modelState;
            this.guiModel = guiModel;
            this.baseModel = baseModel;
        }

        /** 在物品栏变换阶段切换到匠魂同样的简化 GUI 模型。 */
        @Override
        public BakedModel applyTransform(ItemDisplayContext displayContext, PoseStack poseStack, boolean leftHand) {
            if (displayContext == ItemDisplayContext.GUI) {
                return guiModel.applyTransform(displayContext, poseStack, leftHand);
            }
            getTransforms().getTransform(displayContext).apply(leftHand, poseStack);
            return this;
        }

        /** 返回对应流体和容量比例的动态模型，并按流体类型缓存。 */
        private BakedModel getFluidModel(FluidStack fluid, int capacity) {
            int level = Mth.clamp(fluid.getAmount() * increments / Math.max(1, capacity), 1, increments);
            FluidKey key = new FluidKey(fluid.getFluid(), level);
            return fluidModels.computeIfAbsent(key, ignored -> bakeFluidModel(fluid, level));
        }

        /** 烘焙世界模型和 GUI 模型中的流体层。 */
        private BakedModel bakeFluidModel(FluidStack fluid, int level) {
            BakedModel baseLayer = getBaseLayer();
            BakedModel guiLayer = getGuiLayer();
            BakedModel fluidLayer = bakeFluidLayer(fluid, level, modelState, context.isGui3d());
            BakedModel guiFluidLayer = bakeFluidLayer(fluid, level, modelState, context.isGui3d());
            BakedModel guiComposite = composite(context, guiLayer, guiFluidLayer);
            return new Baked(context, baker, spriteGetter, modelState, baseLayer, guiComposite,
                ItemOverrides.EMPTY, ImmutableMap.of("base", baseLayer, "fluid", fluidLayer),
                ImmutableList.of(baseLayer, fluidLayer));
        }

        /** 返回静态模型层，供动态模型复用。 */
        private BakedModel getBaseLayer() {
            return baseModel;
        }

        /** 返回 GUI 静态模型层。 */
        private BakedModel getGuiLayer() {
            return guiModel;
        }

        /** 以两个独立渲染层组合 GUI 模型，确保物品渲染顺序稳定。 */
        private BakedModel composite(IGeometryBakingContext context, BakedModel baseLayer, BakedModel fluidLayer) {
            return new CompositeModel.Baked(context.isGui3d(), context.useBlockLight(), context.useAmbientOcclusion(),
                baseLayer.getParticleIcon(), context.getTransforms(), ItemOverrides.EMPTY,
                ImmutableMap.of("base", baseLayer, "fluid", fluidLayer), ImmutableList.of(baseLayer, fluidLayer));
        }

        /** 读取当前模板声明的方块渲染层。 */
        private RenderTypeGroup getRenderTypes() {
            ResourceLocation hint = context.getRenderTypeHint();
            return hint == null ? RenderTypeGroup.EMPTY : context.getRenderType(hint);
        }

        /** 按液面高度构造六面流体体积，并应用流体颜色和发光等级。 */
        private BakedModel bakeFluidLayer(FluidStack fluid, int level, ModelState state, boolean gui3d) {
            IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid.getFluid());
            TextureAtlasSprite sprite = spriteGetter.apply(new Material(TextureAtlas.LOCATION_BLOCKS,
                extensions.getStillTexture(fluid)));
            float maxY = fluidFrom.y + (fluidTo.y - fluidFrom.y) * level / (float) increments;
            Vector3f from = new Vector3f(fluidFrom.x, fluidFrom.y, fluidFrom.z);
            Vector3f to = new Vector3f(fluidTo.x, maxY, fluidTo.z);
            BlockElement element = fluidElement(from, to);
            net.neoforged.neoforge.client.model.IModelBuilder<?> builder = net.neoforged.neoforge.client.model.IModelBuilder.of(
                context.useAmbientOcclusion(), context.useBlockLight(), gui3d, context.getTransforms(),
                ItemOverrides.EMPTY, sprite, getRenderTypes());
            int tint = extensions.getTintColor(fluid);
            for (Direction direction : Direction.values()) {
                BlockElementFace face = element.faces.get(direction);
                if (face == null) {
                    continue;
                }
                var quad = BlockModel.bakeFace(element, face, sprite, direction, state);
                QuadTransformers.applyingColor(tint).processInPlace(quad);
                int light = Math.min(15, fluid.getFluidType().getLightLevel(fluid));
                if (light > 0) {
                    QuadTransformers.settingEmissivity(light).processInPlace(quad);
                }
                builder.addUnculledFace(quad);
            }
            return builder.build();
        }

        /** 构造与匠魂 1201 相同边界和 UV 方向的流体方块元素。 */
        private BlockElement fluidElement(Vector3f from, Vector3f to) {
            Map<Direction, BlockElementFace> faces = new java.util.EnumMap<>(Direction.class);
            for (Direction direction : Direction.values()) {
                faces.put(direction, new BlockElementFace(null, -1, "fluid", fluidUv(from, to, direction)));
            }
            return new BlockElement(from, to, faces, null, false);
        }

        /** 计算六个方向的流体 UV，保持原版静止流体的贴图方向。 */
        private BlockFaceUV fluidUv(Vector3f from, Vector3f to, Direction direction) {
            float u1;
            float u2;
            float v1;
            float v2;
            switch (direction) {
                case DOWN -> {
                    u1 = from.x;
                    v1 = 16.0F - to.z;
                    u2 = to.x;
                    v2 = 16.0F - from.z;
                }
                case UP -> {
                    u1 = from.x;
                    v1 = from.z;
                    u2 = to.x;
                    v2 = to.z;
                }
                case NORTH -> {
                    u1 = 16.0F - to.x;
                    v1 = 16.0F - to.y;
                    u2 = 16.0F - from.x;
                    v2 = 16.0F - from.y;
                }
                case SOUTH -> {
                    u1 = from.x;
                    v1 = 16.0F - to.y;
                    u2 = to.x;
                    v2 = 16.0F - from.y;
                }
                case WEST -> {
                    u1 = from.z;
                    v1 = 16.0F - to.y;
                    u2 = to.z;
                    v2 = 16.0F - from.y;
                }
                case EAST -> {
                    u1 = 16.0F - to.z;
                    v1 = 16.0F - to.y;
                    u2 = 16.0F - from.z;
                    v2 = 16.0F - from.y;
                }
                default -> throw new IllegalStateException("Unexpected fluid direction: " + direction);
            }
            return new BlockFaceUV(new float[]{u1, v1, u2, v2}, 0);
        }

    }
}
