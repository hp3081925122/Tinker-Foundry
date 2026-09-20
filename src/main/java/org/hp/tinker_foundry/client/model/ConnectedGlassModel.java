package org.hp.tinker_foundry.client.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.Plane;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.RenderTypeGroup;
import net.neoforged.neoforge.client.model.CompositeModel;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.hp.tinker_foundry.TinkerFoundry;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/** 复刻匠魂玻璃的连接纹理模型，不依赖 Mantle 也能按相邻方块切换纹理变体。 */
public final class ConnectedGlassModel implements IUnbakedGeometry<ConnectedGlassModel> {
    /** 注册到本模组命名空间的几何加载器。 */
    public static final IGeometryLoader<ConnectedGlassModel> LOADER = ConnectedGlassModel::deserialize;
    /** 模型数据中保存四个水平连接方向。 */
    private static final ModelProperty<Byte> CONNECTIONS = new ModelProperty<>();
    /** 当前模型的普通方块模型。 */
    private final BlockModel model;
    /** 需要根据连接方向切换的纹理键。 */
    private final Set<String> connectedTextures;
    /** 是否使用匠魂玻璃板的中心/边缘连接判定。 */
    private final boolean panePredicate;

    /** 创建连接纹理模型。 */
    private ConnectedGlassModel(BlockModel model, Set<String> connectedTextures, boolean panePredicate) {
        this.model = model;
        this.connectedTextures = connectedTextures;
        this.panePredicate = panePredicate;
    }

    /** 解析普通父模型，保证模板元素和纹理别名已经展开。 */
    @Override
    public void resolveParents(Function<ResourceLocation, net.minecraft.client.resources.model.UnbakedModel> modelGetter,
                               IGeometryBakingContext context) {
        model.resolveParents(modelGetter);
    }

    /** 按连接纹理定义烘焙一个可缓存十六种连接状态的模型。 */
    @Override
    public BakedModel bake(IGeometryBakingContext context, ModelBaker baker,
                           Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState,
                           ItemOverrides overrides) {
        BakedModel[] variants = new BakedModel[16];
        for (int mask = 0; mask < variants.length; mask++) {
            variants[mask] = bakeVariant(context, spriteGetter, modelState, (byte) mask);
        }
        TinkerFoundry.LOGGER.debug("Loaded connected glass model with {} texture keys", connectedTextures.size());
        return new Baked(this, context, variants[0], variants);
    }

    /** 烘焙指定四方向连接状态的元素和面。 */
    private BakedModel bakeVariant(IGeometryBakingContext context,
                                   Function<Material, TextureAtlasSprite> spriteGetter,
                                   ModelState modelState, byte connections) {
        TextureAtlasSprite particle = spriteGetter.apply(context.getMaterial("particle"));
        SimpleBakedModel.Builder builder = new SimpleBakedModel.Builder(
            context.useAmbientOcclusion(), context.useBlockLight(), context.isGui3d(),
            context.getTransforms(), ItemOverrides.EMPTY).particle(particle);
        for (BlockElement element : model.getElements()) {
            for (Map.Entry<Direction, BlockElementFace> entry : element.faces.entrySet()) {
                Direction direction = entry.getKey();
                BlockElementFace face = entry.getValue();
                TextureAtlasSprite sprite = spriteGetter.apply(resolveSprite(context, face, direction, connections));
                BakedQuad quad = BlockModel.bakeFace(element, face, sprite, direction, modelState);
                if (face.cullForDirection() == null) {
                    builder.addUnculledFace(quad);
                } else {
                    builder.addCulledFace(Direction.rotate(modelState.getRotation().getMatrix(), face.cullForDirection()), quad);
                }
            }
        }
        return builder.build(getRenderTypes(context));
    }

    /** 根据面方向和连接状态解析当前面的纹理资源。 */
    private Material resolveSprite(IGeometryBakingContext context, BlockElementFace face,
                                   Direction faceDirection, byte connections) {
        String textureKey = face.texture();
        if (textureKey.startsWith("#")) {
            textureKey = textureKey.substring(1);
        }
        if (!connectedTextures.contains(textureKey)) {
            return context.getMaterial(textureKey);
        }
        String suffix = getTextureSuffix(connections, getTransform(faceDirection, face.uv()));
        if (suffix.isEmpty()) {
            return context.getMaterial(textureKey);
        }
        Material base = context.getMaterial(textureKey);
        ResourceLocation texture = base.texture();
        ResourceLocation variant = ResourceLocation.fromNamespaceAndPath(
            texture.getNamespace(), texture.getPath() + "/" + suffix);
        return new Material(base.atlasLocation(), variant);
    }

    /** 按原版连接模型的面朝向规则把四方向状态映射到纹理后缀。 */
    private static String getTextureSuffix(byte connections, Function<Direction, Direction> transform) {
        int localConnections = 0;
        for (Direction direction : Plane.HORIZONTAL) {
            if (isConnected(connections, transform.apply(direction))) {
                localConnections |= 1 << direction.get2DDataValue();
            }
        }
        StringBuilder suffix = new StringBuilder();
        if ((localConnections & (1 << Direction.NORTH.get2DDataValue())) != 0) suffix.append('u');
        if ((localConnections & (1 << Direction.SOUTH.get2DDataValue())) != 0) suffix.append('d');
        if ((localConnections & (1 << Direction.WEST.get2DDataValue())) != 0) suffix.append('l');
        if ((localConnections & (1 << Direction.EAST.get2DDataValue())) != 0) suffix.append('r');
        return suffix.toString();
    }

    /** 判断指定世界方向是否连接。 */
    private static boolean isConnected(byte connections, Direction direction) {
        if (!direction.getAxis().isHorizontal()) {
            return false;
        }
        return (connections & (1 << direction.get2DDataValue())) != 0;
    }

    /** 复制 Mantle 对面 UV 使用的方向变换，保证每个面的变体方向一致。 */
    private static Function<Direction, Direction> getTransform(Direction face, BlockFaceUV uv) {
        Function<Direction, Direction> transform = direction -> rotateDirection(direction, face);
        boolean flipV = uv.uvs[1] > uv.uvs[3];
        if (uv.uvs[0] > uv.uvs[2]) {
            if (flipV) {
                transform = transform.compose(Direction::getOpposite);
            } else {
                transform = transform.compose(direction -> direction.getAxis() == Axis.X ? direction.getOpposite() : direction);
            }
        } else if (flipV) {
            transform = transform.compose(direction -> direction.getAxis() == Axis.Z ? direction.getOpposite() : direction);
        }
        return switch (uv.rotation) {
            case 90 -> transform.compose(Direction::getClockWise);
            case 180 -> transform.compose(Direction::getOpposite);
            case 270 -> transform.compose(Direction::getCounterClockWise);
            default -> transform;
        };
    }

    /** 将模型局部方向旋转到当前方块面的方向。 */
    private static Direction rotateDirection(Direction direction, Direction rotation) {
        if (rotation == Direction.UP) {
            return direction;
        }
        if (rotation == Direction.DOWN) {
            return direction.getAxis() == Axis.Z ? direction.getOpposite() : direction;
        }
        return switch (direction) {
            case NORTH -> Direction.UP;
            case SOUTH -> Direction.DOWN;
            case EAST -> rotation.getCounterClockWise();
            case WEST -> rotation.getClockWise();
            default -> throw new IllegalArgumentException("Direction must be horizontal axis");
        };
    }

    /** 返回当前模型声明的渲染层。 */
    private static RenderTypeGroup getRenderTypes(IGeometryBakingContext context) {
        ResourceLocation hint = context.getRenderTypeHint();
        return hint == null ? RenderTypeGroup.EMPTY : context.getRenderType(hint);
    }

    /** 根据模型数据或世界邻居计算四个水平连接方向。 */
    private byte getConnections(@Nullable BlockAndTintGetter world, @Nullable BlockPos pos,
                                @Nullable BlockState state) {
        if (world == null || pos == null || state == null) {
            return 0;
        }
        byte connections = 0;
        for (Direction direction : Plane.HORIZONTAL) {
            BlockState neighbor = world.getBlockState(pos.relative(direction));
            if (connects(state, neighbor)) {
                connections |= (byte) (1 << direction.get2DDataValue());
            }
        }
        return connections;
    }

    /** 判断普通玻璃或玻璃板是否应当连接到相邻方块。 */
    private boolean connects(BlockState state, BlockState neighbor) {
        if (state.getBlock() != neighbor.getBlock()) {
            return false;
        }
        if (!panePredicate) {
            return true;
        }
        boolean stateHasSide = hasPaneSide(state);
        return stateHasSide == hasPaneSide(neighbor);
    }

    /** 判断玻璃板状态是否已经带有至少一个水平侧面。 */
    private static boolean hasPaneSide(BlockState state) {
        return state.hasProperty(PipeBlock.NORTH) && (state.getValue(PipeBlock.NORTH)
            || state.getValue(PipeBlock.EAST) || state.getValue(PipeBlock.SOUTH) || state.getValue(PipeBlock.WEST));
    }

    /** 使用原版方块状态属性作为没有世界模型数据时的兼容回退。 */
    private static byte getStateConnections(@Nullable BlockState state) {
        if (state == null) {
            return 0;
        }
        byte connections = 0;
        if (state.hasProperty(PipeBlock.NORTH) && state.getValue(PipeBlock.NORTH)) connections |= 1;
        if (state.hasProperty(PipeBlock.EAST) && state.getValue(PipeBlock.EAST)) connections |= 2;
        if (state.hasProperty(PipeBlock.SOUTH) && state.getValue(PipeBlock.SOUTH)) connections |= 4;
        if (state.hasProperty(PipeBlock.WEST) && state.getValue(PipeBlock.WEST)) connections |= 8;
        return connections;
    }

    /** 几何模型解析器，读取连接纹理键和玻璃板连接判定。 */
    public static ConnectedGlassModel deserialize(JsonObject json, JsonDeserializationContext context) {
        JsonObject baseJson = json.deepCopy();
        baseJson.remove("loader");
        baseJson.remove("connection");
        BlockModel model = BlockModel.fromString(baseJson.toString());
        JsonObject connection = GsonHelper.getAsJsonObject(json, "connection");
        JsonObject textures = GsonHelper.getAsJsonObject(connection, "textures");
        Set<String> connectedTextures = new HashSet<>();
        for (Map.Entry<String, JsonElement> entry : textures.entrySet()) {
            connectedTextures.add(entry.getKey());
        }
        boolean panePredicate = "pane".equals(GsonHelper.getAsString(connection, "predicate", "block"));
        TinkerFoundry.LOGGER.debug("Parsing connected glass model: predicate={}, keys={}", panePredicate ? "pane" : "block", connectedTextures);
        return new ConnectedGlassModel(model, Set.copyOf(connectedTextures), panePredicate);
    }

    /** 连接模型的烘焙结果，按四方向连接掩码缓存十六个普通模型。 */
    private static final class Baked extends CompositeModel.Baked {
        /** 保存连接判定逻辑，供方块世界模型数据计算使用。 */
        private final ConnectedGlassModel parent;
        /** 按四方向连接掩码缓存的烘焙模型。 */
        private final BakedModel[] variants;

        /** 创建连接模型包装器。 */
        private Baked(ConnectedGlassModel parent, IGeometryBakingContext context, BakedModel base, BakedModel[] variants) {
            super(context.isGui3d(), context.useBlockLight(), context.useAmbientOcclusion(),
                base.getParticleIcon(), context.getTransforms(), ItemOverrides.EMPTY,
                ImmutableMap.of("base", base), ImmutableList.of(base));
            this.parent = parent;
            this.variants = variants;
        }

        /** 从世界邻居计算模型数据，避免仅使用放置时的静态模型。 */
        @Override
        public ModelData getModelData(BlockAndTintGetter world, BlockPos pos, BlockState state, ModelData tileData) {
            return tileData.derive().with(CONNECTIONS, parent.getConnections(world, pos, state)).build();
        }

        /** 根据模型数据选择对应连接纹理变体。 */
        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                       RandomSource rand, ModelData data, @Nullable RenderType renderType) {
            Byte connections = data.get(CONNECTIONS);
            int index = connections == null ? getStateConnections(state) : connections & 15;
            return variants[index].getQuads(state, side, rand, data, renderType);
        }
    }
}
