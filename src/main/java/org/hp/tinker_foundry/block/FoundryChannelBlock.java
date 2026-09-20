package org.hp.tinker_foundry.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.hp.tinker_foundry.TinkerFoundry;

import java.util.Map;
import java.util.Locale;

/** 匠魂式多面流体疏导孔，按相邻能力和连接状态生成中心与四侧模型。 */
public final class FoundryChannelBlock extends FoundryEntityBlock {
    /** 疏导孔编解码器。 */
    public static final MapCodec<FoundryChannelBlock> CODEC = simpleCodec(FoundryChannelBlock::new);
    /** 底部连接状态。 */
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;
    /** 红石供能状态。 */
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    /** 四个水平连接状态。 */
    public static final EnumProperty<ChannelConnection> NORTH = EnumProperty.create("north", ChannelConnection.class);
    public static final EnumProperty<ChannelConnection> SOUTH = EnumProperty.create("south", ChannelConnection.class);
    public static final EnumProperty<ChannelConnection> WEST = EnumProperty.create("west", ChannelConnection.class);
    public static final EnumProperty<ChannelConnection> EAST = EnumProperty.create("east", ChannelConnection.class);
    /** 水平面到方块状态属性的映射，供方块实体能力和客户端渲染复用。 */
    public static final Map<Direction, EnumProperty<ChannelConnection>> DIRECTION_MAP = Map.of(
        Direction.NORTH, NORTH,
        Direction.SOUTH, SOUTH,
        Direction.WEST, WEST,
        Direction.EAST, EAST
    );

    /** 右键切换水平连接时显示的状态文本。 */
    private static final Map<ChannelConnection, Component> SIDE_MESSAGES = Map.of(
        ChannelConnection.IN, Component.translatable("block.tinker_foundry.channel.side.in"),
        ChannelConnection.OUT, Component.translatable("block.tinker_foundry.channel.side.out"),
        ChannelConnection.NONE, Component.translatable("block.tinker_foundry.channel.side.none")
    );
    /** 右键切换底部连接时显示的状态文本。 */
    private static final Component DOWN_OUT = Component.translatable("block.tinker_foundry.channel.down.out");
    private static final Component DOWN_NONE = Component.translatable("block.tinker_foundry.channel.down.none");

    /** 四个水平连接面的薄壁碰撞箱。 */
    private static final java.util.Map<Direction, VoxelShape> SIDE_BOUNDS = java.util.Map.of(
        Direction.NORTH, Shapes.join(Block.box(4, 4, 0, 12, 9, 4), Block.box(6, 6, 0, 10, 9, 4), BooleanOp.ONLY_FIRST),
        Direction.SOUTH, Shapes.join(Block.box(4, 4, 12, 12, 9, 16), Block.box(6, 6, 12, 10, 9, 16), BooleanOp.ONLY_FIRST),
        Direction.WEST, Shapes.join(Block.box(0, 4, 4, 4, 9, 12), Block.box(0, 6, 6, 4, 9, 10), BooleanOp.ONLY_FIRST),
        Direction.EAST, Shapes.join(Block.box(12, 4, 4, 16, 9, 12), Block.box(12, 6, 6, 16, 9, 10), BooleanOp.ONLY_FIRST)
    );
    /** 根据底部和四侧是否连通缓存碰撞箱。 */
    private static final VoxelShape[] BOUNDS = createBounds();

    /** 创建疏导孔并注册与匠魂一致的默认连接状态。 */
    public FoundryChannelBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
            .setValue(DOWN, false)
            .setValue(POWERED, false)
            .setValue(NORTH, ChannelConnection.NONE)
            .setValue(SOUTH, ChannelConnection.NONE)
            .setValue(WEST, ChannelConnection.NONE)
            .setValue(EAST, ChannelConnection.NONE));
    }

    /** 生成官方疏导孔中心、底部和四侧连接组合的碰撞箱。 */
    private static VoxelShape[] createBounds() {
        VoxelShape centerUnconnected = Shapes.joinUnoptimized(
            Block.box(4, 4, 4, 12, 9, 12),
            Shapes.or(Block.box(6, 6, 4, 10, 9, 12), Block.box(4, 6, 6, 12, 9, 10)),
            BooleanOp.ONLY_FIRST);
        VoxelShape centerConnected = Shapes.joinUnoptimized(
            Block.box(4, 2, 4, 12, 9, 12),
            Shapes.or(Block.box(6, 6, 4, 10, 9, 12), Block.box(4, 6, 6, 12, 9, 10), Block.box(6, 2, 6, 10, 9, 10)),
            BooleanOp.ONLY_FIRST);
        VoxelShape northWall = Block.box(6, 6, 4, 10, 9, 6);
        VoxelShape southWall = Block.box(6, 6, 10, 10, 9, 12);
        VoxelShape westWall = Block.box(4, 6, 6, 6, 9, 10);
        VoxelShape eastWall = Block.box(10, 6, 6, 12, 9, 10);
        VoxelShape[] bounds = new VoxelShape[32];
        for (boolean down : new boolean[] {false, true}) {
            VoxelShape center = down ? centerConnected : centerUnconnected;
            for (boolean north : new boolean[] {false, true}) {
                VoxelShape northShape = north ? SIDE_BOUNDS.get(Direction.NORTH) : northWall;
                for (boolean south : new boolean[] {false, true}) {
                    VoxelShape southShape = south ? SIDE_BOUNDS.get(Direction.SOUTH) : southWall;
                    for (boolean west : new boolean[] {false, true}) {
                        VoxelShape westShape = west ? SIDE_BOUNDS.get(Direction.WEST) : westWall;
                        for (boolean east : new boolean[] {false, true}) {
                            VoxelShape eastShape = east ? SIDE_BOUNDS.get(Direction.EAST) : eastWall;
                            bounds[makeKey(down, north, south, west, east)] = Shapes.or(center, northShape, southShape, westShape, eastShape);
                        }
                    }
                }
            }
        }
        return bounds;
    }

    /** 将底部和四个水平连接状态压缩为碰撞箱索引。 */
    private static int makeKey(boolean down, boolean north, boolean south, boolean west, boolean east) {
        return (down ? 1 : 0) | (north ? 2 : 0) | (south ? 4 : 0) | (west ? 8 : 0) | (east ? 16 : 0);
    }

    /** 获取指定水平面的状态属性。 */
    private static EnumProperty<ChannelConnection> property(Direction direction) {
        EnumProperty<ChannelConnection> property = DIRECTION_MAP.get(direction);
        if (property == null) {
            throw new IllegalArgumentException("Channel side must be horizontal");
        }
        return property;
    }

    /** 检查相邻方块是否提供流体能力，使用当前 NeoForge 1.21.1 能力查询入口。 */
    private static boolean isFluidHandler(LevelAccessor level, Direction side, BlockPos pos) {
        if (!(level instanceof Level actualLevel)) {
            return false;
        }
        return actualLevel.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, pos, side) != null;
    }

    /** 检查指定侧面是否可以连接疏导孔或流体设备。 */
    private boolean canConnect(LevelAccessor level, BlockPos pos, Direction side) {
        BlockPos adjacentPos = pos.relative(side);
        BlockState adjacentState = level.getBlockState(adjacentPos);
        return adjacentState.is(this) || isFluidHandler(level, side.getOpposite(), adjacentPos);
    }

    /** 由放置面建立初始连接，保留匠魂的潜行放置为输入侧语义。 */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = defaultBlockState().setValue(POWERED, level.hasNeighborSignal(pos));
        Direction clickedFace = context.getClickedFace();
        if (clickedFace == Direction.DOWN) {
            return state;
        }
        if (clickedFace == Direction.UP) {
            return state.setValue(DOWN, canConnect(level, pos, Direction.DOWN));
        }
        BlockPos placedOn = pos.relative(clickedFace.getOpposite());
        ChannelConnection connection = ChannelConnection.NONE;
        if (level.getBlockState(placedOn).is(this)) {
            Player player = context.getPlayer();
            connection = player != null && player.isShiftKeyDown() ? ChannelConnection.IN : ChannelConnection.OUT;
        } else if (isFluidHandler(level, clickedFace.getOpposite(), placedOn)) {
            connection = ChannelConnection.OUT;
        }
        return state.setValue(property(clickedFace.getOpposite()), connection);
    }

    /** 返回当前连接组合的精确碰撞箱，避免四侧连接只显示却无法选中的问题。 */
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return BOUNDS[makeKey(state.getValue(DOWN), state.getValue(NORTH).canFlow(), state.getValue(SOUTH).canFlow(),
            state.getValue(WEST).canFlow(), state.getValue(EAST).canFlow())];
    }

    /** 邻接疏导孔共享连接方向，并在支持方块消失时清理底部连接。 */
    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState adjacentState,
                                  LevelAccessor level, BlockPos pos, BlockPos adjacentPos) {
        if (direction == Direction.DOWN) {
            return state.getValue(DOWN) && adjacentState.isAir() ? state.setValue(DOWN, false) : state;
        }
        if (direction == Direction.UP) {
            return state;
        }
        EnumProperty<ChannelConnection> sideProperty = property(direction);
        if (adjacentState.is(this)) {
            return state.setValue(sideProperty, adjacentState.getValue(property(direction.getOpposite())).getOpposite());
        }
        ChannelConnection current = state.getValue(sideProperty);
        return current != ChannelConnection.NONE && adjacentState.isAir()
            ? state.setValue(sideProperty, ChannelConnection.NONE) : state;
    }

    /** 邻居变化时同步红石状态和所有连接侧，保证放置方向变化立即同步客户端。 */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (level.isClientSide) {
            return;
        }
        boolean powered = level.hasNeighborSignal(pos);
        if (powered != state.getValue(POWERED)) {
            BlockState refreshed = state.setValue(POWERED, powered)
                .setValue(DOWN, powered && canConnect(level, pos, Direction.DOWN));
            level.setBlock(pos, refreshed, Block.UPDATE_CLIENTS);
        }
    }

    /** 空手点击侧面时循环切换无连接、输入和输出状态。 */
    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        Direction side = hit.getDirection() == Direction.UP ? Direction.DOWN : hit.getDirection();
        if (player.isShiftKeyDown() && side != Direction.DOWN) {
            side = side.getOpposite();
        }
        Vec3 hitVec = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
        if (hitVec.z() < 0.25f) side = Direction.NORTH;
        else if (hitVec.z() > 0.75f) side = Direction.SOUTH;
        else if (hitVec.x() < 0.25f) side = Direction.WEST;
        else if (hitVec.x() > 0.75f) side = Direction.EAST;

        BlockState newState = toggleConnection(state, level, pos, player, side);
        if (newState == null) {
            return net.minecraft.world.InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            level.setBlock(pos, newState, Block.UPDATE_CLIENTS);
        }
        return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** 计算玩家点击后的下一种连接状态，禁止对非流体设备产生伪输出口。 */
    private BlockState toggleConnection(BlockState state, Level level, BlockPos pos, Player player, Direction side) {
        if (side == Direction.DOWN) {
            if (!state.getValue(DOWN) && canConnect(level, pos, Direction.DOWN)) {
                player.displayClientMessage(DOWN_OUT, true);
                return state.setValue(DOWN, true);
            }
            if (state.getValue(DOWN)) {
                player.displayClientMessage(DOWN_NONE, true);
                return state.setValue(DOWN, false);
            }
            return null;
        }
        EnumProperty<ChannelConnection> sideProperty = property(side);
        ChannelConnection next = state.getValue(sideProperty).next(player.isShiftKeyDown());
        BlockPos adjacentPos = pos.relative(side);
        if (next == ChannelConnection.OUT && !level.getBlockState(adjacentPos).is(this)
            && !isFluidHandler(level, side.getOpposite(), adjacentPos)) {
            next = next.next(player.isShiftKeyDown());
        }
        player.displayClientMessage(SIDE_MESSAGES.get(next), true);
        TinkerFoundry.LOGGER.debug("[channel] toggled pos={} side={} connection={}", pos, side, next);
        return state.setValue(sideProperty, next);
    }

    /** 相邻同类疏导孔的可流通面不再被 Minecraft 当作重复内部面渲染。 */
    @Override
    @SuppressWarnings("deprecation")
    public boolean skipRendering(BlockState state, BlockState adjacentState, Direction side) {
        return side.getAxis().isHorizontal() && adjacentState.is(this)
            && state.getValue(property(side)).canFlow()
            && adjacentState.getValue(property(side.getOpposite())).canFlow();
    }

    /** 将疏导孔属性加入方块状态定义。 */
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DOWN, POWERED, NORTH, SOUTH, WEST, EAST);
    }

    /** 疏导孔三种连接状态。 */
    public enum ChannelConnection implements StringRepresentable {
        NONE,
        IN,
        OUT;

        /** 输出小写状态名供 blockstate multipart 条件使用。 */
        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        /** 判断该侧是否存在可见和可流通连接。 */
        public boolean canFlow() {
            return this != NONE;
        }

        /** 反转相邻疏导孔的输入输出关系。 */
        public ChannelConnection getOpposite() {
            return switch (this) {
                case IN -> OUT;
                case OUT -> IN;
                case NONE -> NONE;
            };
        }

        /** 按普通或潜行点击循环切换连接状态。 */
        public ChannelConnection next(boolean reverse) {
            if (reverse) {
                return switch (this) {
                    case NONE -> OUT;
                    case OUT -> IN;
                    case IN -> NONE;
                };
            }
            return switch (this) {
                case NONE -> IN;
                case IN -> OUT;
                case OUT -> NONE;
            };
        }
    }

    /** 返回导流槽自身编解码器。 */
    @Override
    protected MapCodec<? extends FoundryEntityBlock> codec() {
        return CODEC;
    }
}
