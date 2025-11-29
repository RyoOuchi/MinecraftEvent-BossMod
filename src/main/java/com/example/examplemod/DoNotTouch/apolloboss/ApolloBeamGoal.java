package com.example.examplemod.DoNotTouch.apolloboss;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class ApolloBeamGoal extends Goal {
    private final ApolloBoss apolloBoss;
    private LivingEntity target;

    // アニメーションと攻撃の進行管理用
    private int attackTime; // 現在の経過tick
    private final int warmUpTime; // 予備動作（構え）の時間
    private final int animationTime; // 攻撃全体の時間

    // クールダウン（連続使用防止）
    private final int cooldownTime;

    public ApolloBeamGoal(ApolloBoss apolloBoss, int warmUpTicks, int totalAnimationTicks, int cooldownTicks) {
        this.apolloBoss = apolloBoss;
        this.warmUpTime = warmUpTicks;
        this.animationTime = totalAnimationTicks;
        this.cooldownTime = cooldownTicks;
        this.attackTime = 0;

        // 移動（MOVE）と視線（LOOK）をこのゴールが制御するようフラグを設定
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    // AIがこのゴールを開始できるかどうかの判定
    @Override
    public boolean canUse() {
        LivingEntity target = this.apolloBoss.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        // クールダウン中でなければ使用可能
        return this.apolloBoss.isBeamReady() && this.apolloBoss.distanceToSqr(target) < 400.0D;
    }

    // ゴールが実行可能だが、継続すべきかの判定
    @Override
    public boolean canContinueToUse() {
        // ターゲットが存在し、攻撃時間が終わっていなければ継続
        return this.target != null && this.target.isAlive() && this.attackTime < this.animationTime;
    }

    // ゴール開始時の処理（1回だけ呼ばれる）
    @Override
    public void start() {
        System.out.println("start");
        //this.attackTime=0;
        this.target = this.apolloBoss.getTarget();
        // 移動を停止させる
        this.apolloBoss.getNavigation().stop();
        // アニメーションフラグON
        this.apolloBoss.setBeam(true);
        this.attackTime = 0;
    }

    // ゴール終了時の処理（中断や完了時）
    @Override
    public void stop() {
        System.out.println("stop");
        this.target = null;
        this.apolloBoss.setBeam(false);
        this.apolloBoss.setBeamCooldown(this.cooldownTime);
        this.attackTime = 0;
        super.stop();
    }
    // 毎tick実行されるメインロジック

    @Override
    public void tick() {
        System.out.println("[BeamGoal] tick start, attackTime=" + this.attackTime);

        if (this.target == null) {
            System.out.println("[BeamGoal] no target");
            return;
        }

        this.attackTime++;
        System.out.println("[BeamGoal] after ++, attackTime=" + this.attackTime);

        // 1. LookControl
        System.out.println("[BeamGoal] before lookAt");
        this.apolloBoss.getLookControl().setLookAt(this.target, 30.0F, 30.0F);
        System.out.println("[BeamGoal] after lookAt");

        // 2. 向き計算
        double dx = this.target.getX() - this.apolloBoss.getX();
        double dz = this.target.getZ() - this.apolloBoss.getZ();
        float targetYRot = (float) (Math.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
        System.out.println("[BeamGoal] targetYRot=" + targetYRot);

        // 3. rotLerp
        System.out.println("[BeamGoal] before rotLerp");
        float newRot = Mth.rotLerp(0.2F, this.apolloBoss.yBodyRot, targetYRot);
        System.out.println("[BeamGoal] after rotLerp newRot=" + newRot);

        // 4. 回転反映
        System.out.println("[BeamGoal] before set rot");
        this.apolloBoss.yBodyRot = newRot;
        this.apolloBoss.setYRot(newRot);
        System.out.println("[BeamGoal] after set rot");

        // 5. ビーム発射タイミング確認
        System.out.println("[BeamGoal] before beam check");
        if (this.attackTime == this.warmUpTime) {
            System.out.println("[BeamGoal] beam! (SKIPPED)");
            this.apolloBoss.performBeamAttack(this.target);
        }
        System.out.println("[BeamGoal] tick end");

        // Goal#tick は何もしないので、なくてもOK
        // super.tick();
    }
}
