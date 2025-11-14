package com.example.examplemod.Screens.TerminalScreen;

import com.example.examplemod.Blocks.ServerBlock.ServerBlockEntity;
import com.example.examplemod.ExampleMod;
import com.example.examplemod.Packet.TerminalCommandPacket;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;

public class TerminalScreen extends Screen {
    private final Screen parent;
    private final BlockPos pos;
    private final BlockEntity blockEntity;

    private final List<String> log = new ArrayList<>();
    private EditBox input;
    private double scrollOffset = 0;
    private final int visibleLines = 14;
    private double maxScroll = 0;

    private boolean connected = false;
    private String currentPath = "/";
    private static final String PROMPT_SUFFIX = " > ";

    public TerminalScreen(Screen parent, BlockPos pos, BlockEntity blockEntity) {
        super(new TextComponent("Aporogram Terminal"));
        this.parent = parent;
        this.pos = pos;
        this.blockEntity = blockEntity;
    }

    @Override
    protected void init() {
        int padding = 20;
        int inputHeight = 20;

        final long blinkInterval = 500; // ms
        final long[] lastBlinkTime = {System.currentTimeMillis()};
        final boolean[] showCursor = {true};

        input = new EditBox(this.font, padding, this.height - padding - inputHeight,
                this.width - 2 * padding, inputHeight, new TextComponent("")) {

            @Override
            public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
                long now = System.currentTimeMillis();
                if (now - lastBlinkTime[0] > blinkInterval) {
                    showCursor[0] = !showCursor[0];
                    lastBlinkTime[0] = now;
                }

                TerminalScreen.this.font.draw(
                        poseStack,
                        this.getValue(),
                        this.x + 2,
                        this.y + (this.height - 8) / 2,
                        0xE0E0E0
                );

                if (this.isFocused() && showCursor[0]) {
                    int textWidth = TerminalScreen.this.font.width(this.getValue());
                    int cursorX = this.x + 2 + textWidth;
                    int cursorY = this.y + 3;
                    TerminalScreen.this.font.draw(poseStack, "_", cursorX, cursorY + 2, 0xFF00FF55);
                }
            }
        };

        input.setBordered(false);
        input.setMaxLength(256);
        input.setTextColor(0xE0E0E0);
        input.setTextColorUneditable(0xE0E0E0);

        this.addRenderableWidget(input);
        log.add(">> Terminal ready. Type 'help' for commands.");
    }

    private void executeCommand(String cmd) {
        log.add(currentPath + PROMPT_SUFFIX + cmd);
        if (cmd.isBlank()) return;
        switch (cmd.toLowerCase()) {
            case "exit" -> {
                Minecraft.getInstance().setScreen(parent);
                return;
            }
            case "help" -> {
                if (!connected) {
                    log.add("Available commands: help, clear, connect, exit");
                } else {
                    ExampleMod.CHANNEL.sendToServer(
                            new TerminalCommandPacket(pos.below(), "help")
                    );
                }
            }
            case "clear" -> log.clear();
            case "connect" -> connectToServer();
            default -> {
                if (!connected) {
                    log.add("   Not connected to any server. Type 'connect' first.");
                    break;
                }
                final Level level = blockEntity.getLevel();
                if (level == null) {
                    log.add("   Level is null.");
                    break;
                }

                ExampleMod.CHANNEL.sendToServer(
                        new TerminalCommandPacket(pos.below(), cmd)
                );
            }
        }
        updateScroll();
    }

    private void updateScroll() {
        maxScroll = Math.max(0, log.size() - visibleLines);
        scrollOffset = maxScroll;
    }

    public void addLogLine(String line) {
        if (!line.isBlank()) log.add(line);
        updateScroll();
    }

    public void setPromptPath(String newPath) {
        if (newPath != null && !newPath.isBlank()) this.currentPath = newPath;
    }

    private void connectToServer() {
        log.add("   Connecting to server at " + pos.below() + "...");
        final Level level = blockEntity.getLevel();
        if (level == null) {
            log.add("   Connection failed: Level is null.");
            return;
        }

        final BlockEntity belowEntity = level.getBlockEntity(pos.below());
        if (belowEntity instanceof ServerBlockEntity) {
            log.add("   Connected successfully to server.");
            connected = true;
            setPromptPath("/");
        } else {
            log.add("   No server block found below.");
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.getFocused() == input && keyCode == 257) {
            String command = input.getValue().trim();
            if (!command.isEmpty()) executeCommand(command);
            input.setValue("");
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scrollOffset = Math.max(0, Math.min(scrollOffset - delta, maxScroll));
        return true;
    }

    @Override
    public void render(PoseStack stack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(stack);
        super.render(stack, mouseX, mouseY, partialTicks);

        int logX = 20;
        int logY = 50;
        int startLine = (int) scrollOffset;
        int endLine = Math.min(startLine + visibleLines, log.size());

        // draw logs
        for (int i = startLine; i < endLine; i++) {
            drawString(stack, this.font, log.get(i), logX, logY + (i - startLine) * 10, 0xE0E0E0);
        }

        String prompt = "(base) " + currentPath + PROMPT_SUFFIX;
        int promptX = 20;
        int promptWidth = this.font.width(prompt);

        drawString(stack, this.font, prompt, 20, input.y + (input.getHeight() / 2) - 4, 0x00FF55);

        int inputX = promptX + promptWidth + 4;
        int inputY = this.height - 40 + 6;
        input.x = inputX;
        input.y = inputY;
        input.setWidth(this.width - inputX - 20);

        input.render(stack, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
