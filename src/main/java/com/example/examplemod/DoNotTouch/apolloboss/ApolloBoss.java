package com.example.examplemod.DoNotTouch.apolloboss;

import com.example.examplemod.ExampleMod;
import com.mojang.math.Vector3f;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

import java.util.List;
import java.util.Optional;

public class ApolloBoss extends Monster implements IAnimatable {
    private final AnimationFactory factory = new AnimationFactory(this);
    private final ServerBossEvent bossEvent = (ServerBossEvent) (new ServerBossEvent(this.getDisplayName(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS)).setDarkenScreen(true);

    // 状態を同期するためのキーを定義
    private static final EntityDataAccessor<Boolean> ATTACKING = SynchedEntityData.defineId(ApolloBoss.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> HEALING = SynchedEntityData.defineId(ApolloBoss.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> BEAMING = SynchedEntityData.defineId(ApolloBoss.class, EntityDataSerializers.BOOLEAN);

    private static final SoundEvent tips = new SoundEvent(new ResourceLocation(ExampleMod.MODID, "tips"));
    private static final SoundEvent beam = new SoundEvent(new ResourceLocation(ExampleMod.MODID, "beam"));
    private static final SoundEvent yummy = new SoundEvent(new ResourceLocation(ExampleMod.MODID, "yummy"));

    // 回復アクションの時間を管理するタイマー
    private int healTimer = 0;
    // 攻撃アクションの時間を管理するタイマー
    private int attackTimer = 0;
    // ビームのクールダウン時間を管理する変数
    private int beamCooldown = 0;

    public ApolloBoss(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.maxUpStep = 1.0F;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        // 初期値を登録
        this.entityData.define(ATTACKING, false);
        this.entityData.define(HEALING, false);
        this.entityData.define(BEAMING, false);
    }

    // 各種アニメーション状態のGetterとSetter
    public void setAttacking(boolean attacking) {
        this.entityData.set(ATTACKING, attacking);
    }

    public boolean isAttacking() {
        return this.entityData.get(ATTACKING);
    }

    public void setHealing(boolean healing) {
        this.entityData.set(HEALING, healing);
    }

    public boolean isHealing() {
        return this.entityData.get(HEALING);
    }

    public void setBeam(boolean beaming) {
        this.entityData.set(BEAMING, beaming);
    }

    public boolean isBeaming() {
        return this.entityData.get(BEAMING);
    }

    public static AttributeSupplier setAttributes() {
        return Animal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 2000.0D)
                .add(Attributes.ATTACK_DAMAGE, 10.0F)
                .add(Attributes.ATTACK_SPEED, 6.0F)
                .add(Attributes.MOVEMENT_SPEED, 0.25F).build();
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new PanicGoal(this, 1.25D));
        //this.goalSelector.addGoal(3,new ApolloAttackGoal(this,1.0D,true));
        this.goalSelector.addGoal(4, new ApolloBeamGoal(this, 20, 20, 100));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(7, (new HurtByTargetGoal(this)).setAlertOthers());
        this.targetSelector.addGoal(8, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        // すでに回復中や攻撃中でなければ、30%の確率で回復行動をとる
        if (!this.level.isClientSide && !isHealing() && !isAttacking()) {
            startHealing();
            System.out.println("アポロがアポロを食べる");
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean doHurtTarget(@NotNull Entity target) {
        // まず通常のダメージ処理を行う
        boolean success = super.doHurtTarget(target);
        // 攻撃が成功し、かつ対象が生き物ならば、上方向に吹き飛ばすような処理を行う
        if (success) {
            System.out.println("tips");
            this.level.playSound(null, this.blockPosition(), tips, SoundSource.HOSTILE, 1.0f, 1.0f);
            if (target instanceof LivingEntity) {
                // 1. 現在の速度ベクトルを取得
                Vec3 currentVelocity = target.getDeltaMovement();
                // 2. 上方向に強い力を加える
                double blastStrength = 1.0D;
                // 水平方向は今の速度を維持しつつ、上方向だけ加算する
                target.setDeltaMovement(currentVelocity.x, blastStrength, currentVelocity.z);
                // 3. 外部から衝撃が加わったというフラグを立てる
                target.hasImpulse = true;
            }
        }
        return success;
    }

    private void startHealing() {
        this.setHealing(true);
        this.healTimer = 40; // 40tick（2秒間）のモーション
        this.level.playSound(null, this.blockPosition(), yummy, SoundSource.HOSTILE, 1.0f, 1.0f);
        this.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ExampleMod.APOLLO_CHOCOLATE));
        System.out.println("Main hand: " + this.getMainHandItem());
    }

    public void startAttacking() {
        this.setAttacking(true);
        this.attackTimer = 20;
    }

    public boolean isBeamReady() {
        return this.beamCooldown <= 0;
    }

    public void setBeamCooldown(int ticks) {
        this.beamCooldown = ticks;
    }

    // 毎tick実行されるメソッドでタイマーを減らす
    @Override
    public void aiStep() {
        super.aiStep();
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        if (!this.level.isClientSide) {
            //System.out.println("cooldown:"+this.beamCooldown);
            // 毎tickクールダウンを減らす
            if (this.beamCooldown > 0) {
                this.beamCooldown--;
            }
            // 回復の処理
            if (isHealing()) {
                this.healTimer--;
                // タイマーが切れたら終了
                if (this.healTimer <= 0) {
                    this.setHealing(false);
                    // てのアイテムを消す、または空にする
                    this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                    // 実際に回復させる（HP+5）
                    this.heal(5.0F);
                }
            }
            // 攻撃の処理（テスト用）
            if (isAttacking()) {
                this.attackTimer--;
                if (this.attackTimer <= 0) {
                    this.setAttacking(false);
                }
            }
        }
    }

    private <E extends IAnimatable> PlayState predicate(AnimationEvent<E> event) {
        // ビーム攻撃
        if (this.isBeaming()) {
            event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.apollo_boss.beam", false));
            return PlayState.CONTINUE;
        }
        // 回復中ならアニメーション
        if (this.isHealing()) {
            event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.apollo_boss.eat", true));
            return PlayState.CONTINUE;
        }
        // 攻撃中なら攻撃アニメーション
        if (this.isAttacking()) {
            event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.apollo_boss.attack", true));
            return PlayState.CONTINUE;
        }
        if (event.isMoving() || event.getLimbSwingAmount() > 0.1F) {
            event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.apollo_boss.walk", true));
            return PlayState.CONTINUE;
        }
        event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.apollo_boss.idle", true));
        return PlayState.CONTINUE;
    }

    @Override
    public void registerControllers(AnimationData animationData) {
        // transitionLengthTicksはアニメーションの遷移時間を制御する。5ticksとすることで滑らかに変化するようにする
        animationData.addAnimationController(new AnimationController(this, "controller", 5, this::predicate));
    }

    @Override
    public AnimationFactory getFactory() {
        return this.factory;
    }


    public void performBeamAttack(LivingEntity target) {
        // サーバーのみで判定を行う
        if (this.level.isClientSide) {
            return;
        }
        this.level.playSound(null, this.blockPosition(), beam, SoundSource.HOSTILE, 1.0f, 1.0f);

        // 始点（目の位置）と終点（ターゲットの少し上）を計算
        Vec3 startPos = this.getEyePosition();
        Vec3 targetPos = target.getEyePosition();

        // 方向ベクトル
        Vec3 viewVector = targetPos.subtract(startPos).normalize();

        // ビームの最大距離
        double maxDistance = 20.0D;

        // 実際にビームが何かに当たるかを計算（レイキャスト）
        Vec3 endPos = startPos.add(viewVector.scale(maxDistance));

        // ブロックとの衝突判定
        HitResult blockHit = this.level.clip(new ClipContext(
                startPos, endPos,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                this
        ));

        // ブロックに当たっっていたら、そこを終点とする
        if (blockHit.getType() != HitResult.Type.MISS) {
            endPos = blockHit.getLocation();
        }
        // エンティティ（ターゲット）との衝突判定
        // ビームの線分周辺にいるエンティティを探す
        AABB beamBox = new AABB(startPos, endPos).inflate(1.0D); // 少し太めに判定
        List<Entity> entities = this.level.getEntities(this, beamBox, entity -> entity instanceof LivingEntity && entity != this);

        Entity finalHitEntity = null;
        double closestDistance = maxDistance * maxDistance;

        for (Entity entity : entities) {
            AABB entityBox = entity.getBoundingBox().inflate(0.5D);
            Optional<Vec3> hit = entityBox.clip(startPos, endPos);
            if (hit.isPresent()) {
                double dist = startPos.distanceToSqr(hit.get());
                if (dist < closestDistance) {
                    closestDistance = dist;
                    finalHitEntity = entity;
                    // エンティティに当たった場所を終点に更新
                    endPos = hit.get();
                }
            }
        }
        // ダメージと爆発処理
        if (finalHitEntity != null) {
            // ダメージを与える（魔法ダメージ扱い）
            finalHitEntity.hurt(DamageSource.mobAttack(this).setMagic().bypassArmor().bypassMagic().bypassInvul(), 10.0F);
        }
        // 着弾地点で爆発（地形破壊はしない）
        this.level.explode(this, endPos.x, endPos.y, endPos.z, 2.0F, Explosion.BlockInteraction.NONE);

        // パーティクル描画（簡易なもの）
        ServerLevel serverLevel = (ServerLevel) this.level;
        double distance = startPos.distanceTo(endPos);
        for (double d = 0; d < distance; d += 0.5D) {
            double progress = d / distance;
            double x = startPos.x + (endPos.x - startPos.x) * progress;
            double y = startPos.y + (endPos.y - startPos.y) * progress;
            double z = startPos.z + (endPos.z - startPos.z) * progress;

            // アポロチョコ色のダストパーティクル
            serverLevel.sendParticles(new DustParticleOptions(new Vector3f(1.0F, 0.4F, 0.7F), 1.0F), x, y, z, 1, 0, 0, 0, 0);
            // たまに茶色を混ぜる
            if (this.random.nextFloat() < 0.3F) {
                serverLevel.sendParticles(new DustParticleOptions(new Vector3f(0.4F, 0.2F, 0.1F), 1.0F), x, y, z, 1, 0, 0, 0, 0);
            }
        }
    }

    public void startSeenByPlayer(ServerPlayer pPlayer) {
        super.startSeenByPlayer(pPlayer);
        this.bossEvent.addPlayer(pPlayer);
    }

    public void stopSeenByPlayer(ServerPlayer pPlayer) {
        super.stopSeenByPlayer(pPlayer);
        this.bossEvent.removePlayer(pPlayer);
    }

    @Override
    public Component getDisplayName() {
        return new TextComponent("激おこアポロ");
    }
}
