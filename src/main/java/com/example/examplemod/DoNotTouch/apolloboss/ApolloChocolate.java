package com.example.examplemod.DoNotTouch.apolloboss;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ApolloChocolate extends Item {
    private MobEffect[] effects = {
            MobEffects.ABSORPTION,
            MobEffects.HEAL,
            MobEffects.HEALTH_BOOST,
            MobEffects.REGENERATION
    };

    public ApolloChocolate() {
        super(new Properties().tab(CreativeModeTab.TAB_FOOD).food(new FoodProperties.Builder()
                .alwaysEat()
                .nutrition(10)
                .saturationMod(2.0F)
                .build()));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        if (livingEntity instanceof ApolloBoss apolloBoss) {
            for (MobEffect mobEffect : effects) {
                apolloBoss.addEffect(new MobEffectInstance(mobEffect, 200, 2));
            }
        }
        return super.finishUsingItem(stack, level, livingEntity);
    }
}
