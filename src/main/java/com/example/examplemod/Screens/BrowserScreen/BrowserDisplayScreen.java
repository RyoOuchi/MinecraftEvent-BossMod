package com.example.examplemod.Screens.BrowserScreen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.util.FormattedCharSequence;

import java.awt.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BrowserDisplayScreen extends Screen {
    private final String content;
    private final List<FormattedLine> lines = new ArrayList<>();
    private int scrollOffset = 0;
    private static final int LINE_HEIGHT = 11;
    private static final int PADDING = 12;

    public BrowserDisplayScreen(String content) {
        super(new TextComponent("Browser Display"));
        this.content = content;
    }

    @Override
    protected void init() {
        super.init();
        parseHTMLLikeContent(content);

        this.addRenderableWidget(new Button(
                PADDING, this.height - 25, 60, 20,
                new TextComponent("Back"),
                (button) -> onClose()
        ));
    }

    /** Parse basic HTML-like tags and split lines **/
    private void parseHTMLLikeContent(String input) {
        lines.clear();

        // Replace HTML line breaks
        input = input.replace("<br>", "\n");

        // Split into lines manually
        String[] rawLines = input.split("\n");

        for (String raw : rawLines) {
            Component styled = parseStyledText(raw);
            List<FormattedCharSequence> wrapped = this.font.split(styled, this.width - 2 * PADDING);
            for (FormattedCharSequence seq : wrapped) {
                lines.add(new FormattedLine(seq, null)); // clickable link = null by default
            }
        }
    }

    /** Parse simple HTML-like tags (<b>, <i>, <h1>, <color>, <a href>) **/
    private Component parseStyledText(String text) {
        MutableComponent out = new TextComponent("");

        // STYLE STATE
        TextColor currentColor = TextColor.fromRgb(0xFFFFFF);
        boolean bold = false;
        boolean italic = false;
        boolean underline = false;
        String currentHref = null; // if inside <a href="...">

        Pattern tokenPat = Pattern.compile("(<[^>]+>|[^<]+)");
        Matcher m = tokenPat.matcher(text);

        while (m.find()) {
            String token = m.group();

            if (token.startsWith("<")) {
                // --- TAGS ---
                if (token.equalsIgnoreCase("<b>")) {
                    bold = true;
                } else if (token.equalsIgnoreCase("</b>")) {
                    bold = false;
                } else if (token.equalsIgnoreCase("<i>")) {
                    italic = true;
                } else if (token.equalsIgnoreCase("</i>")) {
                    italic = false;
                } else if (token.equalsIgnoreCase("<h1>")) {
                    // simple header styling: gold + bold
                    currentColor = TextColor.fromRgb(0xFFD700);
                    bold = true;
                } else if (token.equalsIgnoreCase("</h1>")) {
                    currentColor = TextColor.fromRgb(0xFFFFFF);
                    bold = false;
                } else if (token.equalsIgnoreCase("<h2>")) {
                    currentColor = TextColor.fromRgb(0xFFFF55);
                    bold = true;
                } else if (token.equalsIgnoreCase("</h2>")) {
                    currentColor = TextColor.fromRgb(0xFFFFFF);
                    bold = false;
                } else if (token.toLowerCase().startsWith("<color=")) {
                    // <color=#RRGGBB>
                    int hash = token.indexOf('#');
                    int gt = token.indexOf('>', hash >= 0 ? hash : 0);
                    if (hash != -1 && gt != -1) {
                        String hex = token.substring(hash + 1, gt);
                        try {
                            int rgb = Integer.parseInt(hex, 16);
                            currentColor = TextColor.fromRgb(rgb);
                        } catch (NumberFormatException ignored) {
                            currentColor = TextColor.fromRgb(0xFFFFFF);
                        }
                    }
                } else if (token.equalsIgnoreCase("</color>")) {
                    currentColor = TextColor.fromRgb(0xFFFFFF);
                } else if (token.toLowerCase().startsWith("<a ")) {
                    // <a href="...">
                    Matcher hrefM = Pattern.compile("href=\"([^\"]+)\"").matcher(token);
                    if (hrefM.find()) {
                        currentHref = hrefM.group(1);
                        underline = true; // visual cue
                    }
                } else if (token.equalsIgnoreCase("</a>")) {
                    currentHref = null;
                    underline = false;
                } else if (token.equalsIgnoreCase("<br>")) {
                    out.append("\n");
                }
                // ignore unknown tags
            } else {
                // --- PLAIN TEXT CHUNK WITH CURRENT STYLE ---
                Style style = Style.EMPTY.withColor(currentColor)
                        .withBold(bold)
                        .withItalic(italic)
                        .withUnderlined(underline);

                if (currentHref != null) {
                    style = style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, currentHref));
                }

                out.append(new TextComponent(token).setStyle(style));
            }
        }

        return out;
    }


    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(poseStack);

        // Title
        drawCenteredString(poseStack, this.font, "🪟 Minecraft Browser", this.width / 2, 10, 0xFFFFFF);

        // Content area
        int yStart = 30;
        int visibleLines = (this.height - yStart - 40) / LINE_HEIGHT;

        for (int i = 0; i < visibleLines; i++) {
            int index = i + scrollOffset;
            if (index >= lines.size()) break;
            FormattedLine line = lines.get(index);
            drawString(poseStack, this.font, line.text, PADDING, yStart + i * LINE_HEIGHT, 0xDDDDDD);
        }

        super.render(poseStack, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int totalLines = lines.size();
        int visibleLines = (this.height - 60) / LINE_HEIGHT;
        int maxOffset = Math.max(0, totalLines - visibleLines);

        if (delta > 0) scrollOffset = Math.max(scrollOffset - 1, 0);
        else if (delta < 0) scrollOffset = Math.min(scrollOffset + 1, maxOffset);

        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int yStart = 30;
            int lineIndex = (int) ((mouseY - yStart) / LINE_HEIGHT) + scrollOffset;
            if (lineIndex >= 0 && lineIndex < lines.size()) {
                FormattedLine line = lines.get(lineIndex);
                if (line.link != null) {
                    try {
                        Desktop.getDesktop().browse(new URI(line.link));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(null);
    }

    /** Helper class for each rendered line **/
    private static class FormattedLine {
        public final FormattedCharSequence text;
        public final String link;

        public FormattedLine(FormattedCharSequence text, String link) {
            this.text = text;
            this.link = link;
        }
    }
}