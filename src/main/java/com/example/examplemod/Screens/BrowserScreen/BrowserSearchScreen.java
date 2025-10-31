package com.example.examplemod.Screens.BrowserScreen;

import com.example.examplemod.Blocks.BrowserBlock.BrowserBlockEntity;
import com.example.examplemod.Packet.RequestUrlPacket;
import com.example.examplemod.ExampleMod;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;

public class BrowserSearchScreen extends Screen {
    private EditBox urlField;
    private final BrowserBlockEntity blockEntity;

    public BrowserSearchScreen(BrowserBlockEntity blockEntity) {
        super(new TextComponent("Browser"));
        this.blockEntity = blockEntity;
    }

    @Override
    protected void init() {
        super.init();

        int textBoxWidth = 200;
        int textBoxHeight = 20;
        int x = (this.width - textBoxWidth) / 2;
        int y = this.height / 2 - textBoxHeight / 2;

        urlField = new EditBox(this.font, x, y, textBoxWidth, textBoxHeight, new TextComponent("Enter URL"));
        this.addRenderableWidget(urlField);

        this.addRenderableWidget(new Button(x, y + 30, 60, 20, new TextComponent("Go"), button -> {
            String url = urlField.getValue();
            if (!url.isEmpty()) {
                this.minecraft.player.sendMessage(new TextComponent("Requesting: " + url), this.minecraft.player.getUUID());

                byte[] urlBytes = blockEntity.convertUrlToByte(url);

                Minecraft.getInstance().setScreen(new BrowserLoadingScreen());
                ExampleMod.CHANNEL.sendToServer(new RequestUrlPacket(urlBytes, blockEntity.getBlockPos()));
            }
        }));
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(poseStack);
        drawCenteredString(poseStack, this.font, "Enter URL", this.width / 2, this.height / 2 - 40, 0xFFFFFF);

        super.render(poseStack, mouseX, mouseY, partialTicks);

        urlField.render(poseStack, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (urlField.keyPressed(keyCode, scanCode, modifiers) || urlField.canConsumeInput()) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (urlField.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
