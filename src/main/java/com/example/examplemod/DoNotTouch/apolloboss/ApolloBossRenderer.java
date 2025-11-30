package com.example.examplemod.DoNotTouch.apolloboss;

import com.example.examplemod.ExampleMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib3.geo.render.built.GeoBone;
import software.bernie.geckolib3.geo.render.built.GeoModel;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;
import software.bernie.geckolib3.renderers.geo.GeoLayerRenderer;
import software.bernie.geckolib3.util.RenderUtils;

public class ApolloBossRenderer extends GeoEntityRenderer<ApolloBoss> {
    public ApolloBossRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new ApolloBossModel());
        this.shadowRadius = 1.2F;
        this.addLayer(new GeoLayerRenderer<>(this) {
            @Override
            public void render(PoseStack poseStack, MultiBufferSource multiBufferSource, int i, ApolloBoss apolloBoss, float v, float v1, float v2, float v3, float v4, float v5) {
                // ここでApolloBossクラスで右手（MainHand）にセットしたアイテムを紐づけている
                ItemStack itemStack = apolloBoss.getMainHandItem();
                if (itemStack.isEmpty()) {
                    return;
                }
                ResourceLocation modelLocation = this.getEntityModel().getModelLocation(apolloBoss);
                GeoModel geoModel = this.getEntityModel().getModel(modelLocation);
                String boneName = "arm2";
                GeoBone geoBone = geoModel.getBone(boneName).orElse(null);
                if (geoBone == null) {
                    return;
                }
                poseStack.pushPose();

                RenderUtils.translate(geoBone, poseStack);
                RenderUtils.rotate(geoBone, poseStack);
                RenderUtils.scale(geoBone, poseStack);

                poseStack.mulPose(Vector3f.XP.rotationDegrees(-90.0F));
                poseStack.translate(1D, 0D, 0.8D);

                Minecraft.getInstance().getItemRenderer().renderStatic(
                        itemStack,
                        ItemTransforms.TransformType.THIRD_PERSON_RIGHT_HAND,
                        i,
                        OverlayTexture.NO_OVERLAY,
                        poseStack,
                        multiBufferSource,
                        apolloBoss.getId()
                );
                poseStack.popPose();
            }
        });
    }

    @Override
    public ResourceLocation getTextureLocation(ApolloBoss instance) {
        return new ResourceLocation(ExampleMod.MODID, "textures/entity/apollo_boss_texture.png");
    }

    @Override
    public RenderType getRenderType(ApolloBoss animatable, float partialTicks, PoseStack stack, @Nullable MultiBufferSource renderTypeBuffer, @Nullable VertexConsumer vertexBuilder, int packedLightIn, ResourceLocation textureLocation) {
        stack.scale(1.0F, 1.0F, 1.0F);
        return super.getRenderType(animatable, partialTicks, stack, renderTypeBuffer, vertexBuilder, packedLightIn, textureLocation);
    }
}
