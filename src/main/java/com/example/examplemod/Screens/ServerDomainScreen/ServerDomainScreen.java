package com.example.examplemod.Screens.ServerDomainScreen;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.Packet.AddServerPacket;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;

public class ServerDomainScreen extends Screen {

    private EditBox domainInput;
    private Button confirmButton;
    private BlockPos serverBlockPos;

    public ServerDomainScreen(final BlockPos serverBlockPos) {
        super(new TextComponent("Add Server"));
        this.serverBlockPos = serverBlockPos;
    }

    @Override
    protected void init() {
        super.init();

        // 🧭 Center positions
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // 📝 Create text box (EditBox)
        domainInput = new EditBox(
                this.font,
                centerX - 100, // x position
                centerY - 20,  // y position
                200,           // width
                20,            // height
                new TextComponent("Enter domain")
        );

        domainInput.setMaxLength(64);
        domainInput.setValue("");
        this.addRenderableWidget(domainInput);

        // 🔘 Create button
        confirmButton = new Button(
                centerX - 50,
                centerY + 20,
                100,
                20,
                new TextComponent("Add Server"),
                button -> onConfirmPressed()
        );

        this.addRenderableWidget(confirmButton);
    }

    private void onConfirmPressed() {
        String enteredDomain = domainInput.getValue().trim();

        if (enteredDomain.isEmpty()) {
            System.out.println("⚠️ No domain entered!");
            return;
        }

        System.out.println("✅ Added server domain: " + enteredDomain);

        // TODO: You can call ServerSavedData.get(level).addServer(enteredDomain, pos);
        // or send a packet to the server side here.

        ExampleMod.CHANNEL.sendToServer(new AddServerPacket(enteredDomain, serverBlockPos));

        this.minecraft.setScreen(null); // closes the screen
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(poseStack);
        drawCenteredString(poseStack, this.font, "Enter Server Domain", this.width / 2, this.height / 2 - 50, 0xFFFFFF);
        super.render(poseStack, mouseX, mouseY, partialTick);
        domainInput.render(poseStack, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
