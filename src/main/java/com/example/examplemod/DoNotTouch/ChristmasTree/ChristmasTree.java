package com.example.examplemod.DoNotTouch.ChristmasTree;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

// カスタムモブエンティティクラスを定義。Animalを継承し、GeckoLibのアニメーションにも対応できるようにIAnimatableを実装
public class ChristmasTree extends Animal implements IAnimatable {
    // アニメーション管理のためのファクトリー
    private AnimationFactory factory = new AnimationFactory(this);

    public ChristmasTree(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);
    }

    // エンティティの属性（体力、攻撃力など）を設定するメソッド
    public static AttributeSupplier setAttributes() {
        return Animal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.ATTACK_DAMAGE, 3.0f)
                .add(Attributes.ATTACK_SPEED, 2.0f)
                .add(Attributes.MOVEMENT_SPEED, 0.3f).build();
    }

    // モブのAI行動（ゴール）を設定
    protected void registerGoals() {

    }

    @Override
    public boolean isPushable() {
        return false; // Entity cannot be pushed
    }

    @Override
    protected void doPush(Entity entity) {
        // Do nothing — tree does NOT push others
    }

    @Override
    protected void pushEntities() {
        // Prevent vanilla's entity pushing logic
    }

    @Override
    public boolean isAffectedByFluids() {
        return false; // Won’t get pushed by water
    }

    @Override
    public boolean isNoGravity() {
        return true; // Prevent falling
    }

    @Override
    public void travel(Vec3 travelVector) {
        // Disable movement entirely
        // (MovementSpeed attribute still exists, but ignored)
    }

    @Override
    public boolean causeFallDamage(float pFallDistance, float pMultiplier, DamageSource pSource) {
        return false; // Cannot take fall damage
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean isInvulnerable() {
        return true; // cannot take damage
    }

    @Override
    public boolean isAttackable() {
        return false; // players cannot attack it
    }

    @Override
    public boolean skipAttackInteraction(Entity attacker) {
        return true; // disables left-click damage from players
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false; // prevent all damage
    }

    @Override
    public boolean isPickable() {
        return false; // cannot be targeted/hit by raytrace or punches
    }

    // アニメーションの挙動を制御するメソッド（predicate）
    // isMoving() を使って、移動中にアニメーションを再生するように設定
    private <E extends IAnimatable> PlayState predicate(AnimationEvent<E> event) {
        //　ここでアニメーションを指定できます。
        //　今回はアニメーションが1つしか作ってないため、if文での分岐は必要ありませんが、
        //　複数のアニメーションを作成した場合は、以下のようにif文を使って切り替えてください。
        event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.christmas_tree.sway_leaves", true));
        return PlayState.CONTINUE;
    }

    @Override
    public void tick() {
        super.tick();
    }

    // 繁殖時に新しい個体を生成する処理（nullを返しているので繁殖しない）
    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel p_146743_, AgeableMob p_146744_) {
        return null;
    }

    // アニメーションコントローラを登録（predicateで指定したアニメーションを登録している感じ）
    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController(this, "controller",
                0, this::predicate));
    }

    // アニメーションファクトリーの取得
    @Override
    public AnimationFactory getFactory() {
        return this.factory;
    }
}