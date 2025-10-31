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
            new ResourceLocation("textures/gui/icons.png"); // vanilla texture sheet
    private float rotationAngle = 0f; // current rotation in degrees

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

        // Center of screen
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Draw title
        drawCenteredString(poseStack, this.font, "Loading...", centerX, centerY - 40, 0xFFFFFF);

        // --- Loading circle animation ---
        RenderSystem.setShaderTexture(0, LOADING_TEXTURE);
        RenderSystem.enableBlend();

        // save transform
        poseStack.pushPose();
        poseStack.translate(centerX, centerY, 0);
        poseStack.mulPose(com.mojang.math.Vector3f.ZP.rotationDegrees(rotationAngle));

        // draw one of the "icons" as a circle piece (e.g., the "heart" icon region)
        // We'll use a small cropped area (16x16 region)
        blit(poseStack, -8, -8, 0, 0, 16, 16, 256, 256);

        poseStack.popPose();

        // update rotation
        rotationAngle = (rotationAngle + (partialTicks * 10f)) % 360f;

        super.render(poseStack, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
