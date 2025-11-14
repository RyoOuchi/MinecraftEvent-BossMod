package com.example.examplemod.Screens.Screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;

import java.util.ArrayList;
import java.util.List;

// 複数行対応のコードエディタウィジェット
public class MultilineCodeEditor extends AbstractWidget implements GuiEventListener {
    private final List<String> lines = new ArrayList<>(); // コードの各行
    private int cursorLine = 0; // 現在のカーソル行
    private int cursorCol = 0;  // 現在のカーソル列
    private int scrollOffset = 0; // 表示のスクロール位置（何行目から描画するか）
    //public static final String[] CUSTOM_COMMANDS = {"spawn"};

    public MultilineCodeEditor(int x, int y, int width, int height) {
        super(x, y, width, height, TextComponent.EMPTY);
        lines.add(""); // 最初は空の1行だけ
    }

    // 文字が入力されたときの処理
    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (chr == '\t') { // タブはスペース4つに変換
            insertText("    ");
            return true;
        } else if (chr >= 32 && chr != 127) { // 表示可能なASCII文字のみ受け付ける
            insertText(String.valueOf(chr));
            return true;
        }
        return false;
    }

    // 現在のカーソル位置にテキストを挿入
    private void insertText(String text) {
        String line = lines.get(cursorLine);
        String before = line.substring(0, cursorCol);
        String after = line.substring(cursorCol);
        lines.set(cursorLine, before + text + after);
        cursorCol += text.length();
    }

    // Backspace の処理（1文字削除）
    private void deleteCharBeforeCursor() {

        if (cursorCol > 0) {
            String line = lines.get(cursorLine);
            String before = line.substring(0, cursorCol - 1);
            String after = line.substring(cursorCol);
            lines.set(cursorLine, before + after);
            cursorCol--;
        } else if (cursorLine > 0) {
            String current = lines.remove(cursorLine);
            cursorLine--;
            cursorCol = lines.get(cursorLine).length();
            lines.set(cursorLine, lines.get(cursorLine) + current);
        }
    }

    // Delete の処理（後ろ1文字削除）
    private void deleteCharAfterCursor() {
        String line = lines.get(cursorLine);
        if (cursorCol < line.length()) {
            String before = line.substring(0, cursorCol);
            String after = line.substring(cursorCol + 1);
            lines.set(cursorLine, before + after);
        } else if (cursorLine < lines.size() - 1) {
            String next = lines.remove(cursorLine + 1);
            lines.set(cursorLine, line + next);
        }
    }

    // Enterキー処理：改行を挿入
    private void insertNewLine() {
        final String line = lines.get(cursorLine);
        String before = line.substring(0, cursorCol);
        String after = line.substring(cursorCol);
        lines.set(cursorLine, before);

        // Determine the correct indentation for the new line based on previous line
        String indent = getIndentationForLine(cursorLine);

        // Check if the current line is a block command (e.g., if, while)
        if (isBlockCommand(line)) {
            indent = indent + "    "; // Increase indentation after block command
        }

        lines.add(cursorLine + 1, indent + after);

        cursorLine++;
        cursorCol = indent.length();
    }

    // Checks if the line contains a block command (if, while, for, etc.)
    private boolean isBlockCommand(String line) {
        String trimmedLine = line.trim().toLowerCase();

        // List of block commands to check (can be expanded with more keywords)
        String[] blockCommands = {"if", "while"};

        for (String command : blockCommands) {
            if (trimmedLine.startsWith(command + " ")) {
                return true; // If the line starts with a block command
            }
        }
        return false;
    }

    private String getIndentationForLine(int lineIndex) {
        int level = 0;

        for (int i = 0; i <= lineIndex; i++) {
            String currentLine = lines.get(i);

            int indentLevel = countIndentation(currentLine);

            if (indentLevel > level) {
                level++;
            } else if (indentLevel < level) {
                level--;
            }
        }

        return "    ".repeat(Math.max(0, level));
    }

    private int countIndentation(String line) {
        int count = 0;
        while (count < line.length() && line.charAt(count) == ' ') {
            count++;
        }
        return count / 4;
    }


    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        switch (keyCode) {
            case 258 -> {
                insertText("    ");
                return true;
            }
            case 256 -> {
                Minecraft.getInstance().setScreen(null);
                return true;
            }
            case 265 -> { // ↑
                if (cursorLine > 0) {
                    cursorLine--;
                    cursorCol = Math.min(cursorCol, lines.get(cursorLine).length());
                }
                return true;
            }
            case 264 -> { // ↓
                if (cursorLine < lines.size() - 1) {
                    cursorLine++;
                    cursorCol = Math.min(cursorCol, lines.get(cursorLine).length());
                }
                return true;
            }
            case 263 -> { // ←
                if (cursorCol > 0) {
                    cursorCol--;
                } else if (cursorLine > 0) {
                    cursorLine--;
                    cursorCol = lines.get(cursorLine).length();
                }
                return true;
            }
            case 262 -> { // →
                if (cursorCol < lines.get(cursorLine).length()) {
                    cursorCol++;
                } else if (cursorLine < lines.size() - 1) {
                    cursorLine++;
                    cursorCol = 0;
                }
                return true;
            }
            case 259 -> { // Backspace
                deleteCharBeforeCursor();
                return true;
            }
            case 261 -> { // Delete
                deleteCharAfterCursor();
                return true;
            }
            case 257, 335 -> { // Enter
                insertNewLine();
                return true;
            }
        }
        return false;
    }

    // マウスホイールでスクロール処理
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int lineHeight = Minecraft.getInstance().font.lineHeight;
        int visibleLines = height / lineHeight;

        if (delta > 0 && scrollOffset > 0) scrollOffset--;
        else if (delta < 0 && scrollOffset + visibleLines < lines.size()) scrollOffset++;

        return true;
    }

    @Override
    public void render(PoseStack stack, int mouseX, int mouseY, float partialTicks) {
        fill(stack, x, y, x + width, y + height, 0xFF1E1E1E); // Background

        int lineHeight = Minecraft.getInstance().font.lineHeight;
        int visibleLines = height / lineHeight;

        // Render each line
        for (int i = 0; i < visibleLines && (i + scrollOffset) < lines.size(); i++) {
            String line = lines.get(i + scrollOffset);
            int lineX = x + 4; // X offset for the start of the line
            int wordX = lineX;

            // Split line into words (this includes spaces as well)
            String[] words = line.split("(?<=\\s)|(?=\\s)"); // Split by word or space
            for (String word : words) {
                if (word.equals("    ")) { // Detect tab (4 spaces)
                    // Move to the next tab stop
                    wordX += Minecraft.getInstance().font.width("    "); // Add 4 spaces width for the tab
                } else if (word.equals(" ")) {
                    // Handle regular spaces normally
                    wordX += Minecraft.getInstance().font.width(" ");
                } else {

                    Minecraft.getInstance().font.draw(stack, word, wordX, y + 4 + (i * lineHeight), 0xFFFFFF);

                    wordX += Minecraft.getInstance().font.width(word);
                }
            }
        }

        if ((System.currentTimeMillis() / 500) % 2 == 0) {
            if (cursorLine >= scrollOffset && cursorLine < scrollOffset + visibleLines) {
                int cursorY = y + 4 + (cursorLine - scrollOffset) * lineHeight;
                String line = lines.get(cursorLine);
                int cursorX = x + 4 + Minecraft.getInstance().font.width(line.substring(0, Math.min(cursorCol, line.length())));
                fill(stack, cursorX, cursorY, cursorX + 1, cursorY + lineHeight, 0xFFFFFFFF);
            }
        }
    }

    @Override
    public Component getMessage() {
        return new TextComponent(getText());
    }

    public String getText() {
        return String.join("\n", lines);
    }

    public void setText(String content) {
        lines.clear();
        lines.addAll(List.of(content.split("\n")));
        cursorLine = lines.size() - 1;
        cursorCol = lines.get(cursorLine).length();
    }

    @Override
    public void updateNarration(NarrationElementOutput pNarrationElementOutput) {}

}
