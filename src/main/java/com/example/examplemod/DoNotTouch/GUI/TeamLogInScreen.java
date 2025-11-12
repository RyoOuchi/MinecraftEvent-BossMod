package com.example.examplemod.DoNotTouch.GUI;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;

public class TeamLogInScreen extends Screen {

    private static final String[] TEAMS = {
            "Team A", "Team B", "Team C", "Team D", "Team E", "Team F", "Team G"
    };

    public TeamLogInScreen() {
        super(new TextComponent(""));
    }

    @Override
    protected void init() {
        super.init();

        int buttonWidth = 120;
        int buttonHeight = 20;
        int spacing = 10;

        int totalHeight = (buttonHeight + spacing) * TEAMS.length - spacing;
        int startY = (this.height / 2) - (totalHeight / 2);

        for (int i = 0; i < TEAMS.length; i++) {
            int x = (this.width / 2) - (buttonWidth / 2);
            int y = startY + i * (buttonHeight + spacing);
            String teamName = TEAMS[i];

            this.addRenderableWidget(new Button(x, y, buttonWidth, buttonHeight,
                    new TextComponent(teamName),
                    button -> onTeamSelected(teamName)
            ));
        }
    }

    private void onTeamSelected(String teamName) {
        System.out.println("[TeamLogInScreen] Selected team: " + teamName);
        Minecraft.getInstance().setScreen(new TeamConfirmationScreen(teamName, this));
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(poseStack);
        drawCenteredString(poseStack, this.font, this.title, this.width / 2, 30, 0xFFFFFF);
        super.render(poseStack, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}