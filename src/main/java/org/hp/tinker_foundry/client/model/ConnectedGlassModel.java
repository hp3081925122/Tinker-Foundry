package org.hp.tinker_foundry.client.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.math.Quadrant;
import com.mojang.serialization.MapCodec;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.renderer.block.dispatch.SingleVariant;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.cuboid.CuboidModelElement;
import net.minecraft.client.resources.model.cuboid.FaceBakery;
import net.minecraft.client.resources.model.cuboid.UnbakedCuboidGeometry;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DelegateUnbakedModel;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.client.model.UnbakedModelLoader;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;
import org.hp.tinker_foundry.TinkerFoundry;
import org.joml.Vector3fc;

/** 复刻匠魂玻璃的连接纹理模型：模型 JSON 声明连接纹理，方块状态模型按相邻方块切换十六种纹理变体。 */
public final class ConnectedGlassModel {
    /** 注册到 NeoForge 26.1.2 模型加载管线的玻璃加载器，保留连接纹理声明。 */
    public static final UnbakedModelLoader<ConnectedUnbaked> LOADER = ConnectedGlassModel::read;
    /** 方块状态 JSON 中 {@code "type": "tinker_foundry:connected"} 的编解码器，字段与原版变体一致。 */
    public static final MapCodec<BlockStateUnbaked> BLOCK_STATE_CODEC =
        Variant.MAP_CODEC.xmap(BlockStateUnbaked::new, BlockStateUnbaked::variant);

    /** 读取连接纹理键和玻璃板连接判定，其余字段交给原版模型解析器。 */
    private static ConnectedUnbaked read(JsonObject json, JsonDeserializationContext context) {
        JsonObject vanilla = json.deepCopy();
        vanilla.remove("loader");
        vanilla.remove("connection");
        UnbakedModel delegate = context.deserialize(vanilla, UnbakedModel.class);
        JsonObject connection = GsonHelper.getAsJsonObject(json, "connection");
        Set<String> connectedTextures = new HashSet<>();
        for (Map.Entry<String, JsonElement> entry : GsonHelper.getAsJsonObject(connection, "textures").entrySet()) {
            connectedTextures.add(entry.getKey());
        }
        boolean panePredicate = "pane".equals(GsonHelper.getAsString(connection, "predicate", "block"));
        TinkerFoundry.LOGGER.debug("Parsing connected glass model: predicate={}, keys={}", panePredicate ? "pane" : "block", connectedTextures);
        return new ConnectedUnbaked(delegate, Set.copyOf(connectedTextures), panePredicate);
    }

    /** 带连接纹理声明的普通方块模型；物品和未声明连接的方块状态仍按普通模型烘焙。 */
    public static final class ConnectedUnbaked extends DelegateUnbakedModel {
        /** 需要根据连接方向切换的纹理键。 */
        private final Set<String> connectedTextures;
        /** 是否使用匠魂玻璃板的中心/边缘连接判定。 */
        private final boolean panePredicate;

        /** 包装原版解析结果并保存连接参数。 */
        private ConnectedUnbaked(UnbakedModel delegate, Set<String> connectedTextures, boolean panePredicate) {
            super(delegate);
            this.connectedTextures = connectedTextures;
            this.panePredicate = panePredicate;
        }
    }

    /** 方块状态层的连接玻璃模型，按方块状态变体的旋转烘焙十六种连接状态。 */
    public record BlockStateUnbaked(Variant variant) implements CustomUnbakedBlockStateModel {
        /** 返回注册的方块状态模型编解码器。 */
        @Override
        public MapCodec<? extends CustomUnbakedBlockStateModel> codec() {
            return BLOCK_STATE_CODEC;
        }

        /** 让模型加载器提前解析引用的方块模型。 */
        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            variant.resolveDependencies(resolver);
        }

        /** 找到模型链上的连接声明并烘焙十六种变体，没有声明时退回普通单变体模型。 */
        @Override
        public BlockStateModel bake(ModelBaker baker) {
            ResolvedModel model = baker.getModel(variant.modelLocation());
            ModelState modelState = variant.modelState().asModelState();
            ConnectedUnbaked connection = findConnection(model);
            if (connection == null || !(model.getTopGeometry() instanceof UnbakedCuboidGeometry geometry)) {
                return new SingleVariant(SimpleModelWrapper.bake(baker, model, modelState));
            }
            TextureSlots textures = model.getTopTextureSlots();
            boolean ambientOcclusion = model.getTopAmbientOcclusion();
            Material.Baked particle = model.resolveParticleMaterial(textures, baker);
            BlockStateModelPart[] variants = new BlockStateModelPart[16];
            for (int mask = 0; mask < variants.length; mask++) {
                QuadCollection quads = bakeVariant(connection, geometry.elements(), textures, baker, modelState, model, (byte) mask);
                variants[mask] = new SimpleModelWrapper(quads, ambientOcclusion, particle);
            }
            TinkerFoundry.LOGGER.debug("Loaded connected glass model {} with {} texture keys",
                variant.modelLocation(), connection.connectedTextures.size());
            return new Baked(connection, variants);
        }
    }

    /** 沿父模型链寻找连接纹理声明。 */
    private static ConnectedUnbaked findConnection(ResolvedModel model) {
        for (ResolvedModel current = model; current != null; current = current.parent()) {
            if (current.wrapped() instanceof ConnectedUnbaked unbaked) {
                return unbaked;
            }
        }
        return null;
    }

    /** 按原版立方体几何规则烘焙指定四方向连接状态的元素和面。 */
    private static QuadCollection bakeVariant(ConnectedUnbaked connection, List<CuboidModelElement> elements, TextureSlots textures,
                                              ModelBaker baker, ModelState modelState, ResolvedModel model, byte connections) {
        QuadCollection.Builder builder = new QuadCollection.Builder();
        for (CuboidModelElement element : elements) {
            Vector3fc from = element.from();
            Vector3fc to = element.to();
            boolean drawX = from.y() != to.y() && from.z() != to.z();
            boolean drawY = from.x() != to.x() && from.z() != to.z();
            boolean drawZ = from.x() != to.x() && from.y() != to.y();
            if (!drawX && !drawY && !drawZ) {
                continue;
            }
            for (Map.Entry<Direction, CuboidFace> entry : element.faces().entrySet()) {
                Direction direction = entry.getKey();
                CuboidFace face = entry.getValue();
                boolean draw = switch (direction.getAxis()) {
                    case X -> drawX;
                    case Y -> drawY;
                    case Z -> drawZ;
                };
                if (!draw) {
                    continue;
                }
                Material.Baked material = resolveMaterial(connection, textures, baker, model, face, direction, from, to, connections);
                BakedQuad quad = FaceBakery.bakeQuad(baker, from, to, face, material, direction, modelState,
                    element.rotation(), element.shade(), element.lightEmission());
                if (face.cullForDirection() == null) {
                    builder.addUnculledFace(quad);
                } else {
                    builder.addCulledFace(Direction.rotate(modelState.transformation().getMatrix(), face.cullForDirection()), quad);
                }
            }
        }
        return builder.build();
    }

    /** 根据面方向和连接状态解析当前面的纹理，连接时使用 {@code 纹理/后缀} 变体。 */
    private static Material.Baked resolveMaterial(ConnectedUnbaked connection, TextureSlots textures, ModelBaker baker,
                                                  ResolvedModel model, CuboidFace face, Direction direction,
                                                  Vector3fc from, Vector3fc to, byte connections) {
        String textureKey = face.texture().startsWith("#") ? face.texture().substring(1) : face.texture();
        if (!connection.connectedTextures.contains(textureKey)) {
            return baker.materials().resolveSlot(textures, face.texture(), model);
        }
        CuboidFace.UVs uvs = face.uvs() == null ? FaceBakery.defaultFaceUV(from, to, direction) : face.uvs();
        String suffix = getTextureSuffix(connections, getTransform(direction, uvs, face.rotation()));
        Material base = textures.getMaterial(textureKey);
        if (suffix.isEmpty() || base == null) {
            return baker.materials().resolveSlot(textures, face.texture(), model);
        }
        Identifier variant = Identifier.fromNamespaceAndPath(base.sprite().getNamespace(), base.sprite().getPath() + "/" + suffix);
        return baker.materials().get(new Material(variant, base.forceTranslucent()), model);
    }

    /** 按原版连接模型的面朝向规则把四方向状态映射到纹理后缀。 */
    private static String getTextureSuffix(byte connections, Function<Direction, Direction> transform) {
        int localConnections = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
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
        return direction.getAxis().isHorizontal() && (connections & (1 << direction.get2DDataValue())) != 0;
    }

    /** 复制 Mantle 对面 UV 使用的方向变换，保证每个面的变体方向一致。 */
    private static Function<Direction, Direction> getTransform(Direction face, CuboidFace.UVs uvs, Quadrant rotation) {
        Function<Direction, Direction> transform = direction -> rotateDirection(direction, face);
        boolean flipV = uvs.minV() > uvs.maxV();
        if (uvs.minU() > uvs.maxU()) {
            if (flipV) {
                transform = transform.compose(Direction::getOpposite);
            } else {
                transform = transform.compose(direction -> direction.getAxis() == Direction.Axis.X ? direction.getOpposite() : direction);
            }
        } else if (flipV) {
            transform = transform.compose(direction -> direction.getAxis() == Direction.Axis.Z ? direction.getOpposite() : direction);
        }
        return switch (rotation) {
            case R90 -> transform.compose(Direction::getClockWise);
            case R180 -> transform.compose(Direction::getOpposite);
            case R270 -> transform.compose(Direction::getCounterClockWise);
            default -> transform;
        };
    }

    /** 将模型局部方向旋转到当前方块面的方向。 */
    private static Direction rotateDirection(Direction direction, Direction rotation) {
        if (rotation == Direction.UP) {
            return direction;
        }
        if (rotation == Direction.DOWN) {
            return direction.getAxis() == Direction.Axis.Z ? direction.getOpposite() : direction;
        }
        return switch (direction) {
            case NORTH -> Direction.UP;
            case SOUTH -> Direction.DOWN;
            case EAST -> rotation.getCounterClockWise();
            case WEST -> rotation.getClockWise();
            default -> throw new IllegalArgumentException("Direction must be horizontal axis");
        };
    }

    /** 判断普通玻璃或玻璃板是否应当连接到相邻方块。 */
    private static boolean connects(boolean panePredicate, BlockState state, BlockState neighbor) {
        if (state.getBlock() != neighbor.getBlock()) {
            return false;
        }
        return !panePredicate || hasPaneSide(state) == hasPaneSide(neighbor);
    }

    /** 判断玻璃板状态是否已经带有至少一个水平侧面。 */
    private static boolean hasPaneSide(BlockState state) {
        return state.hasProperty(PipeBlock.NORTH) && (state.getValue(PipeBlock.NORTH)
            || state.getValue(PipeBlock.EAST) || state.getValue(PipeBlock.SOUTH) || state.getValue(PipeBlock.WEST));
    }

    /** 连接模型的烘焙结果，按四方向连接掩码缓存十六个模型部件。 */
    private static final class Baked implements DynamicBlockStateModel {
        /** 提供玻璃板连接判定。 */
        private final ConnectedUnbaked connection;
        /** 按四方向连接掩码缓存的烘焙部件。 */
        private final BlockStateModelPart[] variants;

        /** 创建连接模型包装器。 */
        private Baked(ConnectedUnbaked connection, BlockStateModelPart[] variants) {
            this.connection = connection;
            this.variants = variants;
        }

        /** 从世界邻居计算四个水平连接方向。 */
        private int connections(BlockAndTintGetter level, BlockPos pos, BlockState state) {
            int connections = 0;
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                if (connects(connection.panePredicate, state, level.getBlockState(pos.relative(direction)))) {
                    connections |= 1 << direction.get2DDataValue();
                }
            }
            return connections;
        }

        /** 根据相邻方块选择对应连接纹理变体。 */
        @Override
        public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
                                 List<BlockStateModelPart> parts) {
            parts.add(variants[connections(level, pos, state)]);
        }

        /** 区块网格缓存按实际选中的变体区分几何。 */
        @Override
        public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
            return variants[connections(level, pos, state)];
        }

        /** 破坏粒子使用未连接变体的粒子纹理。 */
        @Override
        public Material.Baked particleMaterial() {
            return variants[0].particleMaterial();
        }

        /** 没有世界信息时使用未连接变体的材质标记。 */
        @Override
        public int materialFlags() {
            return variants[0].materialFlags();
        }

        /** 有世界信息时按实际选中的变体返回材质标记，避免变体纹理透明度不同导致渲染层错误。 */
        @Override
        public int materialFlags(BlockAndTintGetter level, BlockPos pos, BlockState state) {
            return variants[connections(level, pos, state)].materialFlags();
        }
    }

    private ConnectedGlassModel() {
    }
}
