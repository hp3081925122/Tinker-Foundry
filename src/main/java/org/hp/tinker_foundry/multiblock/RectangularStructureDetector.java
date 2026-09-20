package org.hp.tinker_foundry.multiblock;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.hp.tinker_foundry.TinkerFoundry;
import org.hp.tinker_foundry.block.FoundryControllerBlock;

/** 只负责矩形几何探测，炉种规则由各自的多方块描述器提供。 */
public final class RectangularStructureDetector {
    /** 炉体允许的最小总高度；控制器外壳至少还要有一层炉腔。 */
    public static final int MINIMUM_HEIGHT = 3;
    /** 炉体允许的最大总高度。 */
    public static final int MAXIMUM_HEIGHT = 64;
    /** 炉腔允许的最大宽度和深度。 */
    public static final int MAXIMUM_INNER_SIZE = 14;

    /** 炉体规则描述器。 */
    public interface Rules {
        /** 判断外壳方块。 */
        boolean isCasing(BlockState state);

        /** 底板中心只接受底板标签中的方块。 */
        boolean isFloor(BlockState state);

        /** 铸造炉需要角柱和底板外框，冶炼炉忽略这些位置。 */
        default boolean hasFrame() { return false; }

        /** 判断炉腔内允许的方块。 */
        boolean isInterior(BlockState state);

        /** 根据边界和内部方块数计算容量。 */
        int capacity(int shellWidth, int shellDepth, int shellHeight, int interiorBlocks);

        /** 根据外宽、外深和实际炉腔高度计算燃料倍率，不把封顶误算成炉腔。 */
        int fuelRate(int shellWidth, int shellDepth, int interiorHeight);
    }

    /** 按控制器朝向尝试闭合矩形炉体，未加载区块直接判定无效。 */
    public static StructureResult validate(Level level, BlockPos controller, Rules rules) {
        if (!level.isLoaded(controller)) {
            return StructureResult.invalid(controller, StructureErrorReason.NOT_LOADED);
        }
        StructureResult firstFailure = null;
        List<Direction> directions = horizontalDirections(level.getBlockState(controller));
        for (Direction direction : directions) {
            StructureResult result = detectOriented(level, controller, direction, rules);
            if (result.valid()) {
                return result;
            }
            if (firstFailure == null) {
                firstFailure = result;
            }
        }
        return firstFailure == null ? StructureResult.invalid(controller, StructureErrorReason.INVALID_INNER_BLOCK) : firstFailure;
    }

    /** 有真实朝向的控制器严格按自身朝向检测，无方向属性的旧状态才遍历水平朝向。 */
    private static List<Direction> horizontalDirections(BlockState state) {
        if (state.hasProperty(FoundryControllerBlock.FACING)) {
            return List.of(state.getValue(FoundryControllerBlock.FACING));
        }
        List<Direction> directions = new ArrayList<>();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            directions.add(direction);
        }
        return directions;
    }

    /** 依据一个方向完成内部尺寸、边界盒和壳体闭合检查。 */
    private static StructureResult detectOriented(Level level, BlockPos controller, Direction facing, Rules rules) {
        Direction inside = facing.getOpposite();
        Direction side = inside.getClockWise();
        BlockPos center = controller.relative(inside);
        BlockPos firstInner = center;
        if (!rules.isInterior(level.getBlockState(firstInner))) {
            if (!rules.hasFrame()) return StructureResult.invalid(center, StructureErrorReason.INVALID_INNER_BLOCK);
            // 控制器可以位于底层外壳；此时按官方逻辑从控制器内侧的上一层开始找炉腔。
            firstInner = center.above();
            if (!rules.isInterior(level.getBlockState(firstInner))) {
                return StructureResult.invalid(center, StructureErrorReason.INVALID_INNER_BLOCK);
            }
        }

        InteriorScan leftScan = scanInterior(level, firstInner.relative(side), side, rules);
        InteriorScan rightScan = scanInterior(level, firstInner.relative(side.getOpposite()), side.getOpposite(), rules);
        InteriorScan depthScan = scanInterior(level, firstInner, inside, rules);
        if (!leftScan.bounded()) {
            return StructureResult.invalid(leftScan.errorPos(), StructureErrorReason.INVALID_WALL_BLOCK);
        }
        if (!rightScan.bounded()) {
            return StructureResult.invalid(rightScan.errorPos(), StructureErrorReason.INVALID_WALL_BLOCK);
        }
        if (!depthScan.bounded()) {
            return StructureResult.invalid(depthScan.errorPos(), StructureErrorReason.INVALID_WALL_BLOCK);
        }
        int left = leftScan.count();
        int right = rightScan.count();
        int depth = depthScan.count();
        if (depth < 1) {
            return StructureResult.invalid(firstInner.relative(inside), StructureErrorReason.INVALID_INNER_BLOCK);
        }
        int innerWidth = left + right + 1;
        if (innerWidth > MAXIMUM_INNER_SIZE || depth > MAXIMUM_INNER_SIZE) {
            return StructureResult.invalid(firstInner, StructureErrorReason.TOO_LARGE);
        }

        // 控制器位于正面外壳，水平边界由连续内部空间推导，垂直边界由炉腔层数推导。
        int firstSide = left + 1;
        int lastSide = -(right + 1);
        int lastInside = depth + 1;
        BlockPos min = boundingCorner(controller, side, firstSide, inside, 0, 0);
        BlockPos max = boundingCorner(controller, side, lastSide, inside, lastInside, 0);
        int minX = Math.min(min.getX(), max.getX());
        int maxX = Math.max(min.getX(), max.getX());
        int minZ = Math.min(min.getZ(), max.getZ());
        int maxZ = Math.max(min.getZ(), max.getZ());

        // 匠魂官方冶炼炉顶部开放，炉腔层必须由侧壁和内部共同确定，不能把天空空气当成无限炉腔。
        // 记录上下扫描遇到的第一个真实错误方块，避免把炉腔中心空气误标成侧壁错误。
        LayerScan belowScan = countInteriorWallLayers(level, controller, firstInner.getY() - 1,
            minX, maxX, minZ, maxZ, rules, -1, MAXIMUM_HEIGHT - 2);
        LayerScan aboveScan = countInteriorWallLayers(level, controller, firstInner.getY() + 1,
            minX, maxX, minZ, maxZ, rules, 1, MAXIMUM_HEIGHT - 2 - belowScan.layers());
        int below = belowScan.layers();
        int above = aboveScan.layers();
        int interiorLayers = below + above + 1;
        int ceilingY = firstInner.getY() + above + 1;
        // 两种结构均不包含封顶；上方停止扩展，不把屋顶作为容量或附件。
        boolean hasCeiling = false;
        int structureHeight = interiorLayers + 1 + (hasCeiling ? 1 : 0);
        TinkerFoundry.LOGGER.debug("[structure-detector] controller={} facing={} firstInner={} bounds=({},{};{},{}), below={} above={} ceilingY={} hasCeiling={} height={}",
            controller, facing, firstInner, minX, maxX, minZ, maxZ, below, above, ceilingY, hasCeiling, structureHeight);
        if (structureHeight > MAXIMUM_HEIGHT) {
            return StructureResult.invalid(new BlockPos(firstInner.getX(), ceilingY, firstInner.getZ()),
                StructureErrorReason.TOO_HIGH);
        }
        if (structureHeight < MINIMUM_HEIGHT) {
            // 优先返回缺失层中实际不合规的位置；没有具体方块时才返回结构中心。
            LayerFailure failure = aboveScan.failure() != null ? aboveScan.failure() : belowScan.failure();
            if (failure != null) {
                return StructureResult.invalid(failure.pos(), failure.reason());
            }
            return StructureResult.invalid(new BlockPos(firstInner.getX(), ceilingY, firstInner.getZ()),
                StructureErrorReason.INVALID_WALL_BLOCK);
        }

        BlockPos actualMin = new BlockPos(minX, firstInner.getY() - below - 1, minZ);
        BlockPos actualMax = new BlockPos(maxX, hasCeiling ? ceilingY : firstInner.getY() + above, maxZ);
        if (!level.hasChunksAt(actualMin, actualMax)) {
            return StructureResult.invalid(actualMin, StructureErrorReason.NOT_LOADED);
        }
        ValidationFailure failure = findInvalidBlock(level, controller, actualMin, actualMax, rules);
        if (failure != null) {
            TinkerFoundry.LOGGER.debug("[structure-detector] controller={} facing={} invalidPos={} reason={} boundsMin={} boundsMax={}",
                controller, facing, failure.pos(), failure.reason(), actualMin, actualMax);
            return StructureResult.invalid(failure.pos(), failure.reason());
        }

        int shellWidth = maxX - minX + 1;
        int shellDepth = maxZ - minZ + 1;
        int interiorBlocks = (shellWidth - 2) * (shellDepth - 2) * interiorLayers;
        int capacity = Math.max(0, rules.capacity(shellWidth, shellDepth, structureHeight, interiorBlocks));
        return new StructureResult(true, capacity, actualMin, actualMax, interiorBlocks,
            Math.max(1, rules.fuelRate(shellWidth, shellDepth, interiorLayers)));
    }

    /** 统计指定方向上连续的炉腔侧壁层，封顶层不计入炉腔容量。 */
    private static LayerScan countInteriorWallLayers(Level level, BlockPos controller, int startY, int minX, int maxX,
                                       int minZ, int maxZ, Rules rules, int direction, int limit) {
        int amount = 0;
        int y = startY;
        LayerFailure failure = null;
        while (amount < limit) {
            failure = findInteriorWallFailure(level, controller, y, minX, maxX, minZ, maxZ, rules);
            if (failure != null) {
                break;
            }
            amount++;
            y += direction;
        }
        return new LayerScan(amount, failure);
    }

    /** 返回一层侧壁或内部的第一个非法方块。 */
    private static LayerFailure findInteriorWallFailure(Level level, BlockPos controller, int y, int minX, int maxX,
                                                        int minZ, int maxZ, Rules rules) {
        if (!level.hasChunksAt(new BlockPos(minX, y, minZ), new BlockPos(maxX, y, maxZ))) {
            return new LayerFailure(new BlockPos(minX, y, minZ), StructureErrorReason.NOT_LOADED);
        }
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                cursor.set(x, y, z);
                if (cursor.equals(controller)) {
                    continue;
                }
                boolean wall = x == minX || x == maxX || z == minZ || z == maxZ;
                // 冶炼炉的四根角柱完全不属于结构，不限制其中放置的方块。
                if (!rules.hasFrame() && (x == minX || x == maxX) && (z == minZ || z == maxZ)) continue;
                BlockState state = level.getBlockState(cursor);
                if (wall ? !rules.isCasing(state) : !rules.isInterior(state)) {
                    // 侧壁和炉腔使用不同的错误原因，供客户端悬浮提示与高亮同步使用。
                    return new LayerFailure(cursor.immutable(), wall
                        ? StructureErrorReason.INVALID_WALL_BLOCK : StructureErrorReason.INVALID_INNER_BLOCK);
                }
            }
        }
        return null;
    }

    /** 保存一个垂直层扫描结果及该层第一个非法位置。 */
    private record LayerScan(int layers, LayerFailure failure) {
    }

    /** 保存非法方块位置和对应的官方结构错误原因。 */
    private record LayerFailure(BlockPos pos, StructureErrorReason reason) {
    }

    /** 判断炉腔上方是否存在可选的完整封顶层。 */
    private static boolean isCeilingLayer(Level level, BlockPos controller, int y, int minX, int maxX,
                                          int minZ, int maxZ, Rules rules) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                cursor.set(x, y, z);
                if (cursor.equals(controller)) {
                    continue;
                }
                if (!rules.isCasing(level.getBlockState(cursor))) {
                    return false;
                }
            }
        }
        return true;
    }

    /** 沿一条轴线计算连续炉腔长度，并要求末端存在实际炉壁。 */
    private static InteriorScan scanInterior(Level level, BlockPos start, Direction direction, Rules rules) {
        int amount = 0;
        BlockPos.MutableBlockPos cursor = start.mutable();
        while (amount < MAXIMUM_INNER_SIZE && level.isLoaded(cursor) && rules.isInterior(level.getBlockState(cursor))) {
            amount++;
            cursor.move(direction);
        }
        boolean bounded = level.isLoaded(cursor) && rules.isCasing(level.getBlockState(cursor));
        BlockPos errorPos = bounded ? cursor.immutable() : start;
        return new InteriorScan(amount, errorPos, bounded);
    }

    /** 保存一条炉腔轴线扫描结果，防止墙外空气被当成无限炉腔。 */
    private record InteriorScan(int count, BlockPos errorPos, boolean bounded) {
    }

    /** 用两个正交方向偏移生成结构角点。 */
    private static BlockPos boundingCorner(BlockPos origin, Direction first, int firstOffset, Direction second, int secondOffset, int yOffset) {
        return origin.relative(first, firstOffset).relative(second, secondOffset).above(yOffset);
    }

    /** 检查外壳闭合且内部没有未允许的方块，并记录第一个失败位置。 */
    private static ValidationFailure findInvalidBlock(Level level, BlockPos controller, BlockPos min, BlockPos max, Rules rules) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = min.getY(); y <= max.getY(); y++) {
            for (int x = min.getX(); x <= max.getX(); x++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    cursor.set(x, y, z);
                    if (cursor.equals(controller)) {
                        continue;
                    }
                    boolean floor = y == min.getY();
                    boolean boundary = x == min.getX() || x == max.getX() || z == min.getZ() || z == max.getZ();
                    boolean ceiling = y == max.getY();
                    boolean corner = (x == min.getX() || x == max.getX()) && (z == min.getZ() || z == max.getZ());
                    if (!rules.hasFrame() && (floor && boundary || corner)) continue;
                    BlockState state = level.getBlockState(cursor);
                    boolean valid = floor && !boundary ? rules.isFloor(state) : boundary
                        ? rules.isCasing(state) : rules.isInterior(state);
                    if (!valid) {
                        StructureErrorReason reason;
                        if (floor) {
                            reason = StructureErrorReason.INVALID_FLOOR_BLOCK;
                        } else if (boundary) {
                            reason = StructureErrorReason.INVALID_WALL_BLOCK;
                        } else if (ceiling) {
                            reason = StructureErrorReason.INVALID_CEILING_BLOCK;
                        } else {
                            reason = StructureErrorReason.INVALID_INNER_BLOCK;
                        }
                        return new ValidationFailure(cursor.immutable(), reason);
                    }
                }
            }
        }
        return null;
    }

    /** 保存一次结构验证失败的位置和原因。 */
    private record ValidationFailure(BlockPos pos, StructureErrorReason reason) {
    }

    private RectangularStructureDetector() {
    }
}
