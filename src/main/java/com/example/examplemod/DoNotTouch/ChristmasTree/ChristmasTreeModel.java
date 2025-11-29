package com.example.examplemod.DoNotTouch.ChristmasTree;

import com.example.examplemod.ExampleMod;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

// GeckoLibのAnimatedGeoModelを継承して、モブのモデル・テクスチャ・アニメーションを指定するクラス
public class ChristmasTreeModel extends AnimatedGeoModel<ChristmasTree> {
    // モブのモデルファイル（.geo.json）の場所を指定
    @Override
    public ResourceLocation getModelLocation(ChristmasTree object) {
        return new ResourceLocation(ExampleMod.MODID, "geo/christmas_tree.geo.json");
    }
    // モブのテクスチャ（.png）の場所を指定
    @Override
    public ResourceLocation getTextureLocation(ChristmasTree object) {
        return new ResourceLocation(ExampleMod.MODID, "textures/entity/christmas_tree.png");
    }
    // アニメーションファイル（.animation.json）の場所を指定
    @Override
    public ResourceLocation getAnimationFileLocation(ChristmasTree animatable) {
        return new ResourceLocation(ExampleMod.MODID, "animations/christmas_tree.animation.json");
    }
}