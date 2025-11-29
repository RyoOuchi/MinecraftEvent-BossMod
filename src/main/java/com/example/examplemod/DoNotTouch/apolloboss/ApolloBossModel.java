package com.example.examplemod.DoNotTouch.apolloboss;

import com.example.examplemod.ExampleMod;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class ApolloBossModel extends AnimatedGeoModel<ApolloBoss>{
    @Override
    public ResourceLocation getModelLocation(ApolloBoss apolloBosEntity) {
        return new ResourceLocation(ExampleMod.MODID,"geo/apollo_boss.geo.json");
    }

    @Override
    public ResourceLocation getTextureLocation(ApolloBoss apolloBosEntity) {
        return new ResourceLocation(ExampleMod.MODID,"textures/entity/apollo_boss_texture.png");
    }

    @Override
    public ResourceLocation getAnimationFileLocation(ApolloBoss apolloBosEntity) {
        return new ResourceLocation(ExampleMod.MODID,"animations/apollo_boss.animation.json");
    }
}
