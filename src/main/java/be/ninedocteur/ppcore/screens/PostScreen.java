package be.ninedocteur.ppcore.screens;

import be.ninedocteur.ppcore.BIOS;
import be.ninedocteur.ppcore.utils.Screen;
import be.ninedocteur.ppcore.utils.TextRenderer;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import java.awt.*;
import java.util.List;

public class PostScreen implements Screen {
    private final boolean intelXeBugDetected;
    private final String intelXeWarning;
    private final boolean showBootText;
    private final BIOS bios;

    public PostScreen(BIOS bios, boolean intelXeBugDetected, String intelXeWarning, boolean showBootText) {
        this.bios = bios;
        this.intelXeBugDetected = intelXeBugDetected;
        this.intelXeWarning = intelXeWarning;
        this.showBootText = showBootText;
    }

    @Override
    public void render() {
        int w = Display.getWidth();
        int h = Display.getHeight();
        float mainFontSize = Math.max(24, h / 10f);
        float infoFontSize = Math.max(12, h / 30f);
        Font mainFont, infoFont;
        try {
            mainFont = Font.createFont(Font.TRUETYPE_FONT, getClass().getResourceAsStream("/VGA.ttf")).deriveFont(mainFontSize);
            infoFont = Font.createFont(Font.TRUETYPE_FONT, getClass().getResourceAsStream("/VGA.ttf")).deriveFont(infoFontSize);
        } catch (Exception e) {
            mainFont = new Font("Monospaced", Font.PLAIN, (int)mainFontSize);
            infoFont = new Font("Monospaced", Font.PLAIN, (int)infoFontSize);
        }
        if (intelXeBugDetected) {
            float warnFontSize = Math.max(14, h / 40f);
            Font warnFont;
            try {
                warnFont = Font.createFont(Font.TRUETYPE_FONT, getClass().getResourceAsStream("/VGA.ttf")).deriveFont(warnFontSize);
            } catch (Exception e) {
                warnFont = new Font("Monospaced", Font.PLAIN, (int)warnFontSize);
            }
            float rectX = 40;
            float rectY = 10;
            float rectW = w - 80;
            java.util.List<String> lines = be.ninedocteur.ppcore.utils.TextRenderer.wrapTextToWidth(intelXeWarning, warnFont, rectW - 20);
            float lineHeight = warnFont.getSize2D() + 4;
            float rectH = lines.size() * lineHeight + 40;
            GL11.glColor3f(1f, 0f, 0f);
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex2f(rectX, rectY);
            GL11.glVertex2f(rectX + rectW, rectY);
            GL11.glVertex2f(rectX + rectW, rectY + rectH);
            GL11.glVertex2f(rectX, rectY + rectH);
            GL11.glEnd();
            GL11.glColor3f(1f, 1f, 1f);
            GL11.glLineWidth(2f);
            GL11.glBegin(GL11.GL_LINE_LOOP);
            GL11.glVertex2f(rectX, rectY);
            GL11.glVertex2f(rectX + rectW, rectY);
            GL11.glVertex2f(rectX + rectW, rectY + rectH);
            GL11.glVertex2f(rectX, rectY + rectH);
            GL11.glEnd();
            float textY = rectY + 10 + warnFont.getSize2D();
            for (String line : lines) {
                float textW = TextRenderer.getTextWidth(line, warnFont);
                float textX = rectX + (rectW - textW) / 2f;
                TextRenderer.drawText(line, textX, textY, warnFont, Color.WHITE);
                textY += lineHeight;
            }
        }
        String mainText = "PopulaireCore";
        float mainTextWidth = TextRenderer.getTextWidth(mainText, mainFont);
        float mainTextHeight = TextRenderer.getTextHeight(mainText, mainFont);
        float mainX = (w - mainTextWidth) / 2f;
        float mainY = (h - mainTextHeight) / 2f;
        String infoText = "Press F2 or DELETE to enter setup. Press F12 to enter boot menu.";
        float infoTextWidth = TextRenderer.getTextWidth(infoText, infoFont);
        float infoTextHeight = TextRenderer.getTextHeight(infoText, infoFont);
        float infoX = (w - infoTextWidth) / 2f;
        float infoY = h - infoTextHeight - 40;
        TextRenderer.drawText(mainText, mainX, mainY, mainFont, new Color(0,255,0));
        if (showBootText) {
            TextRenderer.drawText(infoText, infoX, infoY, infoFont, Color.WHITE);
        }
    }

    @Override
    public void handleInput() {
        while (org.lwjgl.input.Keyboard.next()) {
            if (org.lwjgl.input.Keyboard.getEventKeyState()) {
                int key = org.lwjgl.input.Keyboard.getEventKey();
                if (key == org.lwjgl.input.Keyboard.KEY_F2 || key == org.lwjgl.input.Keyboard.KEY_DELETE) {
                    bios.showBiosScreen();
                    return;
                } else if (key == org.lwjgl.input.Keyboard.KEY_F12) {
                    bios.showBootMenuScreen();
                    return;
                }
            }
        }
    }
}
