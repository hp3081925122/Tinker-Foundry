package org.hp.tinker_foundry.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import org.hp.tinker_foundry.common.FoundryFluidCannonEffects;

/** 本地等价实现 Mantle 流体炮的流体弹实体。 */
public final class FoundryFluidCannonProjectile extends Projectile {
    /** 客户端同步流体注册名，避免把流体对象直接写入网络数据。 */
    private static final EntityDataAccessor<String> FLUID_ID = SynchedEntityData.defineId(
        FoundryFluidCannonProjectile.class, EntityDataSerializers.STRING);
    /** 客户端同步当前流体数量。 */
    private static final EntityDataAccessor<Integer> FLUID_AMOUNT = SynchedEntityData.defineId(
        FoundryFluidCannonProjectile.class, EntityDataSerializers.INT);
    /** 客户端同步炮体强度，供未来不同实体效果保持一致。 */
    private static final EntityDataAccessor<Float> POWER = SynchedEntityData.defineId(
        FoundryFluidCannonProjectile.class, EntityDataSerializers.FLOAT);
    /** 水中弹体的惯性，保持匠魂流体弹在水中的减速行为。 */
    private static final EntityDataAccessor<Float> WATER_INERTIA = SynchedEntityData.defineId(
        FoundryFluidCannonProjectile.class, EntityDataSerializers.FLOAT);

    /** 创建网络同步用的流体弹。 */
    public FoundryFluidCannonProjectile(EntityType<? extends FoundryFluidCannonProjectile> type, Level level) {
        super(type, level);
    }

    /** 创建一个从指定炮口方向飞出的流体弹。 */
    public FoundryFluidCannonProjectile(Level level, BlockPos cannonPos, Direction direction,
                                        FluidStack fluid, float power) {
        this(org.hp.tinker_foundry.registry.TFEntities.FLUID_CANNON_PROJECTILE.get(), level);
        setPos(
            cannonPos.getX() + 0.5D + direction.getStepX() * 0.65D,
            cannonPos.getY() + 0.5D + direction.getStepY() * 0.65D,
            cannonPos.getZ() + 0.5D + direction.getStepZ() * 0.65D
        );
        setFluid(fluid);
        entityData.set(POWER, power);
    }

    /** 流体弹按匠魂原版弹射物使用固定重力。 */
    @Override
    protected double getDefaultGravity() {
        return 0.06D;
    }

    /** 设置网络同步的流体内容。 */
    public void setFluid(FluidStack fluid) {
        if (fluid.isEmpty()) {
            entityData.set(FLUID_ID, "");
            entityData.set(FLUID_AMOUNT, 0);
            return;
        }
        ResourceLocation id = BuiltInRegistries.FLUID.getKey(fluid.getFluid());
        entityData.set(FLUID_ID, id == null ? "" : id.toString());
        entityData.set(FLUID_AMOUNT, Math.max(0, fluid.getAmount()));
    }

    /** 读取客户端或服务端当前同步的流体。 */
    public FluidStack getFluid() {
        String id = entityData.get(FLUID_ID);
        int amount = entityData.get(FLUID_AMOUNT);
        if (id == null || id.isEmpty() || amount <= 0) {
            return FluidStack.EMPTY;
        }
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null) {
            return FluidStack.EMPTY;
        }
        return BuiltInRegistries.FLUID.getOptional(location)
            .map(fluid -> new FluidStack(fluid, amount))
            .orElse(FluidStack.EMPTY);
    }

    /** 返回同步到客户端的炮体强度。 */
    public float getPower() {
        return entityData.get(POWER);
    }

    /** 每 tick 做碰撞检测、姿态更新、重力和生命周期处理。 */
    @Override
    public void tick() {
        super.tick();
        Vec3 movement = getDeltaMovement();
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS) {
            hitTargetOrDeflectSelf(hit);
        }
        if (isRemoved()) {
            return;
        }
        Vec3 nextLocation = position().add(movement);
        if (hit.getType() == HitResult.Type.BLOCK) {
            // 命中方块时把弹体移到碰撞面外沿，避免小实体尺寸导致穿过目标或卡入方块。
            EntityDimensions dimensions = getType().getDimensions();
            float offset = 0.01F + (hit instanceof BlockHitResult blockHit && blockHit.getDirection().getAxis() == Axis.Y
                ? dimensions.height() : dimensions.width() / 2.0F);
            nextLocation = hit.getLocation().add(movement.normalize().scale(offset));
        }
        updateRotation();
        if (tickCount > 80 || getY() > level().getMaxBuildHeight() + 64) {
            discard();
            return;
        }
        float inertia = isInWater() ? entityData.get(WATER_INERTIA) : 0.99F;
        Vec3 velocity = movement.scale(inertia);
        if (!isNoGravity()) {
            FluidStack current = getFluid();
            double gravity = !current.isEmpty() && current.getFluid().getFluidType().isLighterThanAir() ? 0.06D : -0.06D;
            velocity = velocity.add(0.0D, gravity, 0.0D);
        }
        setDeltaMovement(velocity);
        setPos(nextLocation);
    }

    /** 命中生物时执行对应流体效果，并让这一发流体弹结束。 */
    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        // 无论流体类型如何都保留匠魂流体弹的水平击退。
        Vec3 knockback = getDeltaMovement().multiply(1.0D, 0.0D, 1.0D).normalize().scale(0.6D);
        if (knockback.lengthSqr() > 0.0D) {
            hit.getEntity().push(knockback.x, 0.1D, knockback.z);
        }
        if (level().isClientSide) {
            return;
        }
        FluidStack current = getFluid();
        int consumed = FoundryFluidCannonEffects.applyToEntity(level(), hit.getEntity(), current, getPower());
        if (consumed > 0) {
            current = current.copyWithAmount(Math.max(0, current.getAmount() - consumed));
        }
        if (current.isEmpty()) {
            discard();
        } else {
            // 目标没有可执行效果时，保留剩余流体让弹体继续寻找下一个目标。
            setFluid(current);
        }
    }

    /** 命中方块时执行直接方块效果；无对应效果时仍结束弹体，避免穿透重复触发。 */
    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        if (level().isClientSide) {
            return;
        }
        FluidStack current = getFluid();
        int consumed = FoundryFluidCannonEffects.applyToBlock(level(), hit, current, getPower());
        if (consumed > 0) {
            current = current.copyWithAmount(Math.max(0, current.getAmount() - consumed));
            if (!current.isEmpty() && level().getBlockState(hit.getBlockPos()).isAir()) {
                // 目标被破坏后允许剩余流体继续飞行，保持原版的多次命中行为。
                setFluid(current);
                return;
            }
        }
        discard();
    }

    /** 定义客户端显示流体所需的同步字段。 */
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(FLUID_ID, "");
        builder.define(FLUID_AMOUNT, 0);
        builder.define(POWER, 1.0F);
        builder.define(WATER_INERTIA, 0.6F);
    }

    /** 保存跨区块加载所需的流体和强度。 */
    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("FluidId", entityData.get(FLUID_ID));
        tag.putInt("FluidAmount", entityData.get(FLUID_AMOUNT));
        tag.putFloat("Power", entityData.get(POWER));
        tag.putFloat("WaterInertia", entityData.get(WATER_INERTIA));
    }

    /** 从存档恢复流体弹的同步数据。 */
    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(FLUID_ID, tag.getString("FluidId"));
        entityData.set(FLUID_AMOUNT, Math.max(0, tag.getInt("FluidAmount")));
        entityData.set(POWER, tag.getFloat("Power"));
        entityData.set(WATER_INERTIA, tag.getFloat("WaterInertia"));
    }

    /** 客户端接收生成包时补充匠魂风格的喷射粒子和初速度。 */
    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        double x = packet.getXa();
        double y = packet.getYa();
        double z = packet.getZa();
        for (int index = 0; index < 7; index++) {
            double scale = 0.4D + 0.1D * index;
            level().addParticle(ParticleTypes.SPIT, getX(), getY(), getZ(), x * scale, y, z * scale);
        }
        setDeltaMovement(x, y, z);
    }
}
