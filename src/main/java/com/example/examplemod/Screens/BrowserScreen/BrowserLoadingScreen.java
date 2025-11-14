package com.example.examplemod.Screens.BrowserScreen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class BrowserLoadingScreen extends Screen {

    private static final ResourceLocation LOADING_TEXTURE =
            new ResourceLocation("textures/gui/icons.png");
    private float rotationAngle = 0f;

    public BrowserLoadingScreen() {
        super(new TextComponent("Loading..."));
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(poseStack);

        Minecraft mc = Minecraft.getInstance();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        drawCenteredString(poseStack, this.font, "Loading...", centerX, centerY - 40, 0xFFFFFF);

        RenderSystem.setShaderTexture(0, LOADING_TEXTURE);
        RenderSystem.enableBlend();

        poseStack.pushPose();
        poseStack.translate(centerX, centerY, 0);
        poseStack.mulPose(com.mojang.math.Vector3f.ZP.rotationDegrees(rotationAngle));

        blit(poseStack, -8, -8, 0, 0, 16, 16, 256, 256);

        poseStack.popPose();

        rotationAngle = (rotationAngle + (partialTicks * 10f)) % 360f;

        super.render(poseStack, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
