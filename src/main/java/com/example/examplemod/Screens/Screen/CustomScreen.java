package com.example.examplemod.Screens.Screen;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.Packet.SaveCodePacket;
import com.example.examplemod.Screens.TerminalScreen.TerminalScreen;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.level.block.entity.BlockEntity;

public class CustomScreen extends Screen {
    private MultilineCodeEditor codeEditor;
    private final BlockPos pos;
    private final String code;
    private final BlockEntity blockEntity;
    private final String fileName;
    private EditBox titleBox;

    public CustomScreen(BlockPos pos, String code, BlockEntity blockEntity, String fileName) {
        super(new TextComponent("Aporogram Scripting Language"));
        this.pos = pos;
        this.code = code;
        this.blockEntity = blockEntity;
        this.fileName = fileName;
    }

    @Override
    protected void init() {
        int padding = 20;
        int editorX = padding;
        int editorWidth = this.width - 2 * padding;
        int editorHeight = this.height - 90;

        titleBox = new EditBox(this.font, padding, padding, this.width - 2 * padding, 20, new TextComponent("File name"));
        titleBox.setMaxLength(128);
        titleBox.setValue(fileName);
        this.addRenderableWidget(titleBox);

        int editorY = titleBox.y + titleBox.getHeight() + 15;

        // --- Code Editor ---
        codeEditor = new MultilineCodeEditor(editorX, editorY, editorWidth, editorHeight);
        codeEditor.setText(this.code);
        this.addRenderableWidget(codeEditor);
        this.setFocused(codeEditor);
        this.setInitialFocus(codeEditor);

        // --- Save Button ---
        int buttonWidth = 80;
        int buttonHeight = 20;
        int buttonX = this.width - padding - buttonWidth;
        int buttonY = editorY + editorHeight + 10;

        this.addRenderableWidget(new Button(buttonX, buttonY, buttonWidth, buttonHeight,
                new TextComponent("Save"), (button) -> {
            saveFile();
            Minecraft.getInstance().setScreen(null);
        }));

        int terminalButtonWidth = 100;
        int terminalButtonHeight = 20;
        int terminalButtonX = padding;
        int terminalButtonY = buttonY;

        this.addRenderableWidget(new Button(terminalButtonX, terminalButtonY, terminalButtonWidth, terminalButtonHeight,
                new TextComponent("Open Terminal"), (button) -> {
            saveFile();
            String code = codeEditor.getText();
            String fileName = titleBox.getValue().isEmpty() ? "untitled.text" : titleBox.getValue();
            Minecraft.getInstance().setScreen(new TerminalScreen(new CustomScreen(pos, code, blockEntity, fileName), this.pos, this.blockEntity));
        }));
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.getFocused() == titleBox) {
            return titleBox.charTyped(codePoint, modifiers);
        } else if (this.getFocused() == codeEditor) {
            return codeEditor.charTyped(codePoint, modifiers);
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.getFocused() == titleBox) {
            return titleBox.keyPressed(keyCode, scanCode, modifiers);
        } else if (this.getFocused() == codeEditor) {
            return codeEditor.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void saveFile() {
        String code = codeEditor.getText();
        String fileName = titleBox.getValue().isEmpty() ? "untitled.text" : titleBox.getValue();
        System.out.println("Code saved:\n" + code);
        ExampleMod.CHANNEL.sendToServer(new SaveCodePacket(fileName, code, this.pos));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return codeEditor.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void render(PoseStack stack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(stack);
        super.render(stack, mouseX, mouseY, partialTicks);
        if (titleBox.getValue().isEmpty() && !titleBox.isFocused()) {
            drawString(
                    stack,
                    this.font,
                    "Enter filename...",
                    titleBox.x + 4,
                    titleBox.y + (titleBox.getHeight() - 8) / 2,
                    0x808080 // gray color
            );
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean titleFocused = titleBox.mouseClicked(mouseX, mouseY, button);
        boolean codeFocused = codeEditor.mouseClicked(mouseX, mouseY, button);

        if (titleFocused) {
            this.setFocused(titleBox);
            return true;
        } else if (codeFocused) {
            this.setFocused(codeEditor);
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }


    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
