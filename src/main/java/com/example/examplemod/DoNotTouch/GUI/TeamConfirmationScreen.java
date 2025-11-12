package com.example.examplemod.DoNotTouch.GUI;

import com.example.examplemod.DoNotTouch.Packets.ConfirmTeamPacket;
import com.example.examplemod.ExampleMod;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;

public class TeamConfirmationScreen extends Screen {
    private final String selectedTeam;
    private final Screen previousScreen;

    public TeamConfirmationScreen(String selectedTeam, Screen previousScreen) {
        super(new TextComponent("チーム確認"));
        this.selectedTeam = selectedTeam;
        this.previousScreen = previousScreen;
    }

    @Override
    protected void init() {
        super.init();

        int buttonWidth = 100;
        int buttonHeight = 20;
        int spacing = 20;

        int x = (this.width / 2) - (buttonWidth / 2);
        int y = this.height / 2;

        this.addRenderableWidget(new Button(
                x, y, buttonWidth, buttonHeight,
                new TextComponent("決定"),
                button -> onConfirm()
        ));

        this.addRenderableWidget(new Button(
                x, y + buttonHeight + spacing, buttonWidth, buttonHeight,
                new TextComponent("戻る"),
                button -> onBack()
        ));
    }

    private void onConfirm() {
        // TODO: Send packet or save team selection
        System.out.println("✅ Confirmed team: " + selectedTeam);
        ExampleMod.CHANNEL.sendToServer(new ConfirmTeamPacket(selectedTeam));
        this.onClose();
    }

    private void onBack() {
        Minecraft.getInstance().setScreen(previousScreen);
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(poseStack);

        drawCenteredString(poseStack, this.font, "チーム確認", this.width / 2, 40, 0xFFFFFF);
        drawCenteredString(poseStack, this.font,
                selectedTeam + " を選択しました。よろしいですか？",
                this.width / 2, this.height / 2 - 40, 0xFFFFFF);

        super.render(poseStack, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}