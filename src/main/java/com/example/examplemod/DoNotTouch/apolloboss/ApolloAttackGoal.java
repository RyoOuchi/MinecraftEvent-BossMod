package com.example.examplemod.DoNotTouch.apolloboss;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import org.jetbrains.annotations.NotNull;

public class ApolloAttackGoal extends MeleeAttackGoal {
    // ApolloBossクラスのメソッド（setAttackingなど）を使いたいので保持しておく
    private final ApolloBoss apolloBoss;

    public ApolloAttackGoal(ApolloBoss apolloBoss, double speedModifier, boolean followingTarget) {
        super(apolloBoss, speedModifier, followingTarget);
        this.apolloBoss = apolloBoss;
    }

    @Override
    protected void checkAndPerformAttack(@NotNull LivingEntity enemy, double distanceToEnemy) {
        // 攻撃が届く距離の2乗を計算
        double reach = this.getAttackReachSqr(enemy);
        // 間合に入っている、かつ攻撃クールダウンが解消されているなら攻撃実行
        if (distanceToEnemy <= reach && this.isTimeToAttack()) {
            // 攻撃のクールダウンをリセット
            this.resetAttackCooldown();
            // 実際にダメージを与える処理
            this.mob.doHurtTarget(enemy);
            // アニメーションのトリガーを引く
            // 攻撃状態のタイマーをセット
            this.apolloBoss.startAttacking();
        }
    }

    @Override
    protected double getAttackReachSqr(LivingEntity enemy) {
        return (double) (this.mob.getBbWidth() * this.mob.getBbWidth() + enemy.getBbWidth());
    }
}
