package com.example.examplemod.DoNotTouch.apolloboss;

import net.minecraft.world.entity.ai.goal.Goal;

public class ApolloHealGoal extends Goal {
    private final ApolloBoss apolloBoss;

    public ApolloHealGoal(ApolloBoss apolloBoss) {
        this.apolloBoss = apolloBoss;
    }

    @Override
    public boolean canUse() {
        if (this.apolloBoss.tickCount % 60 != 0) {
            return false;
        }

        final float bossHealth = this.apolloBoss.getHealth();
        if (bossHealth < this.apolloBoss.getMaxHealth() && !this.apolloBoss.isHealing() && this.apolloBoss.getRandom().nextFloat() < 0.30f) {
            return true;
        } else if (bossHealth < this.apolloBoss.getMaxHealth() * 0.5 && !this.apolloBoss.isHealing() && this.apolloBoss.getRandom().nextFloat() < 0.50f) {
            return true;
        }

        return false;
    }

    @Override
    public void start() {
        this.apolloBoss.startHealing();
    }
}
