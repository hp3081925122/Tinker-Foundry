package org.hp.tinker_foundry.client.model;

import com.mojang.math.Quadrant;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.cuboid.FaceBakery;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.color.item.FluidContentsTint;
import net.neoforged.neoforge.fluids.FluidStack;
import org.hp.tinker_foundry.item.FoundryTankItem;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

/** 为 26.1.2 物品模型管线补回按容量变化的储罐流体层。 */
public final class FoundryTankItemModel implements ItemModel {
    /** 物品 JSON 使用的动态储罐模型编解码器。 */
    public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Identifier.CODEC.fieldOf("base").forGetter(Unbaked::base),
        ExtraCodecs.VECTOR3F.fieldOf("fluid_from").forGetter(Unbaked::fluidFrom),
        ExtraCodecs.VECTOR3F.fieldOf("fluid_to").forGetter(Unbaked::fluidTo),
        ExtraCodecs.POSITIVE_INT.optionalFieldOf("increments", 256).forGetter(Unbaked::increments)
    ).apply(instance, Unbaked::new));

    /** 静态罐体物品模型。 */
    private final ItemModel baseModel;
    /** 流体模型烘焙所需的上下文。 */
    private final ItemModel.BakingContext bakingContext;
    /** 沿用静态罐体的物品光照和显示变换。 */
    private final ModelRenderProperties renderProperties;
    /** 外层物品模型传入的根变换。 */
    private final Matrix4fc transformation;
    /** 流体体积最小坐标。 */
    private final Vector3fc fluidFrom;
    /** 流体体积最大坐标。 */
    private final Vector3fc fluidTo;
    /** 流体高度离散精度。 */
    private final int increments;
    /** 按流体类型和离散高度缓存动态层，避免每帧重复烘焙。 */
    private final Map<FluidKey, ItemModel> fluidModels = new HashMap<>();

    /** 保存已烘焙的静态模型和动态流体参数。 */
    private FoundryTankItemModel(ItemModel baseModel, ItemModel.BakingContext bakingContext,
                                 ModelRenderProperties renderProperties, Matrix4fc transformation,
                                 Vector3fc fluidFrom, Vector3fc fluidTo, int increments) {
        this.baseModel = baseModel;
        this.bakingContext = bakingContext;
        this.renderProperties = renderProperties;
        this.transformation = transformation;
        this.fluidFrom = new Vector3f(fluidFrom);
        this.fluidTo = new Vector3f(fluidTo);
        this.increments = increments;
    }

    /** 先提交罐体，再按物品组件中的实际流体与容量比例提交流体体积。 */
    @Override
    public void update(ItemStackRenderState output, ItemStack stack, ItemModelResolver resolver,
                       ItemDisplayContext displayContext, @Nullable ClientLevel level,
                       @Nullable ItemOwner owner, int seed) {
        baseModel.update(output, stack, resolver, displayContext, level, owner, seed);
        if (!(stack.getItem() instanceof FoundryTankItem tankItem)) {
            return;
        }

        FluidStack fluid = tankItem.getFluid(stack);
        if (fluid.isEmpty()) {
            return;
        }

        int fluidLevel = Mth.clamp(fluid.getAmount() * increments / Math.max(1, tankItem.capacity()), 1, increments);
        FluidKey key = new FluidKey(fluid.getFluid(), fluidLevel);
        fluidModels.computeIfAbsent(key, ignored -> bakeFluidModel(fluid, fluidLevel))
            .update(output, stack, resolver, displayContext, level, owner, seed);
    }

    /** 烘焙指定流体和高度的六面体物品层。 */
    private ItemModel bakeFluidModel(FluidStack fluid, int fluidLevel) {
        ModelBaker baker = bakingContext.blockModelBaker();
        FluidModel fluidModel = Minecraft.getInstance().getModelManager().getFluidStateModelSet()
            .get(fluid.getFluid().defaultFluidState());
        Vector3f from = new Vector3f(fluidFrom);
        Vector3f to = new Vector3f(fluidTo);
        to.y = fluidFrom.y() + (fluidTo.y() - fluidFrom.y()) * fluidLevel / (float) increments;

        int lightEmission = Mth.clamp(fluid.getFluid().getFluidType().getLightLevel(fluid), 0, 15);
        QuadCollection.Builder quads = new QuadCollection.Builder();
        for (Map.Entry<Direction, CuboidFace> entry : fluidFaces(from, to).entrySet()) {
            quads.addUnculledFace(FaceBakery.bakeQuad(
                baker, from, to, entry.getValue(), fluidModel.stillMaterial(), entry.getKey(),
                BlockModelRotation.IDENTITY, null, false, lightEmission));
        }
        return new CuboidItemModelWrapper(
            List.of(FluidContentsTint.INSTANCE), quads.build(), renderProperties, transformation);
    }

    /** 构造带流体着色索引的六个面，UV 与原版方块模型规则一致。 */
    private static Map<Direction, CuboidFace> fluidFaces(Vector3fc from, Vector3fc to) {
        Map<Direction, CuboidFace> faces = new EnumMap<>(Direction.class);
        for (Direction direction : Direction.values()) {
            faces.put(direction, new CuboidFace(
                null, 0, "fluid", FaceBakery.defaultFaceUV(from, to, direction), Quadrant.R0));
        }
        return faces;
    }

    /** 保存一种流体及其离散液面高度。 */
    private record FluidKey(Fluid fluid, int level) {
    }

    /** 从物品模型 JSON 读取静态模型和流体边界。 */
    public record Unbaked(Identifier base, Vector3fc fluidFrom, Vector3fc fluidTo, int increments)
        implements ItemModel.Unbaked {
        /** 让模型加载器提前解析静态罐体依赖。 */
        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            resolver.markDependency(base);
        }

        /** 烘焙静态罐体，并保存按物品内容延迟生成流体层所需的数据。 */
        @Override
        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
            ModelBaker baker = context.blockModelBaker();
            ResolvedModel resolvedModel = baker.getModel(base);
            TextureSlots textures = resolvedModel.getTopTextureSlots();
            ModelRenderProperties properties = ModelRenderProperties.fromResolvedModel(baker, resolvedModel, textures);
            ItemModel baseModel = new CuboidItemModelWrapper.Unbaked(base, Optional.empty(), List.of())
                .bake(context, transformation);
            return new FoundryTankItemModel(
                baseModel, context, properties, transformation, fluidFrom, fluidTo, increments);
        }

        /** 返回注册到本模组命名空间的物品模型类型。 */
        @Override
        public MapCodec<? extends ItemModel.Unbaked> type() {
            return MAP_CODEC;
        }
    }

}
