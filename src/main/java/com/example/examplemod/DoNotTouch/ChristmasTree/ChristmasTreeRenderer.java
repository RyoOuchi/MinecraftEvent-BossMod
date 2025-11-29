package com.example.examplemod.DoNotTouch.ChristmasTree;

import com.example.examplemod.ExampleMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib3.model.AnimatedGeoModel;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;

// GeckoLibのGeoEntityRendererを継承して、モブのレンダリングを定義するクラス
public class ChristmasTreeRenderer extends GeoEntityRenderer<ChristmasTree> {
    public ChristmasTreeRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new ChristmasTreeModel());
        this.shadowRadius = 0.3f;   // モブの影のサイズを設定
    }

    // モブのテクスチャファイルの場所を指定（オーバーライドしなくてもModel側の設定が使われるが、念のため明示）
    @Override
    public ResourceLocation getTextureLocation(ChristmasTree instance) {
        return new ResourceLocation(ExampleMod.MODID, "textures/entity/christmas_tree.png");
    }

    @Override
    public RenderType getRenderType(ChristmasTree animatable, float partialTicks, PoseStack stack, @Nullable MultiBufferSource renderTypeBuffer, @Nullable VertexConsumer vertexBuilder, int packedLightIn, ResourceLocation textureLocation) {
        stack.scale(1.0f, 1.0f, 1.0f);  // モブの大きさを変更する場合ここを編集（例：stack.scale(1.5f, 1.5f, 1.5f);）
        return super.getRenderType(animatable, partialTicks, stack, renderTypeBuffer, vertexBuilder, packedLightIn, textureLocation);
    }

    @Override
    public void render(ChristmasTree entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);

        spawnStarParticles(entity, "top-star");
    }

    private void spawnStarParticles(ChristmasTree entity, String boneName) {
        // Optional: reduce spam – only run every 2 ticks
        if (entity.tickCount % 20 != 0) return;

        var provider = this.getGeoModelProvider();
        var model = provider.getModel(provider.getModelLocation(entity));
        var optBone = model.getBone(boneName);

        optBone.ifPresent(bone -> {
            float localX = bone.getPivotX();
            float localY = bone.getPivotY() + 0.2F;
            float localZ = bone.getPivotZ();

            float offX = localX / 16.0F;
            float offY = localY / 16.0F;
            float offZ = localZ / 16.0F;

            double yawRad = Math.toRadians(-entity.getYRot());
            double cos = Math.cos(yawRad);
            double sin = Math.sin(yawRad);

            double rotatedX = offX * cos - offZ * sin;
            double rotatedZ = offX * sin + offZ * cos;

            double centerX = entity.getX() + rotatedX;
            double centerY = entity.getY() + offY;
            double centerZ = entity.getZ() + rotatedZ;

            double outerR = 0.35D;
            double innerR = 0.18D;

            for (int i = 0; i < 10; i++) {
                double angleDeg = entity.getYRot() + i * 36.0D;
                double angleRad = Math.toRadians(angleDeg);
                double r = (i % 2 == 0) ? outerR : innerR;

                double dx = r * Math.cos(angleRad);
                double dz = r * Math.sin(angleRad);

                entity.level.addParticle(
                        ParticleTypes.GLOW,
                        centerX + dx,
                        centerY,
                        centerZ + dz,
                        0.0D, 0.01D, 0.0D
                );
            }

            entity.level.addParticle(
                    ParticleTypes.GLOW,
                    centerX,
                    centerY,
                    centerZ,
                    0.0D, 0.02D, 0.0D
            );
        });
    }
}