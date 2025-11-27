package be.ninedocteur.ppcore;

import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class TextRenderer {
    private static Font font;

    public static void init() {
        try (InputStream is = TextRenderer.class.getResourceAsStream("/VGA.ttf")) {
            if (is == null) throw new IOException("Font not found");
            font = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(18f);
        } catch (Exception e) {
            e.printStackTrace();
            font = new Font("SansSerif", Font.PLAIN, 18);
        }
    }

    public static void drawText(String text, float x, float y) {
        if (font == null) init();
        if (text == null || text.isEmpty()) return;

        int newX = (int) x;
        int newY = (int) y - 7;

        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setFont(font);
        FontMetrics fm = g2d.getFontMetrics();
        int width = fm.stringWidth(text);
        int height = fm.getHeight();
        g2d.dispose();

        BufferedImage textImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        g2d = textImage.createGraphics();
        g2d.setComposite(AlphaComposite.Clear);
        g2d.fillRect(0, 0, width, height);
        g2d.setComposite(AlphaComposite.SrcOver);
        g2d.setFont(font);
        g2d.setColor(Color.BLACK);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.drawString(text, 0, fm.getAscent());
        g2d.dispose();

        int[] pixels = new int[width * height];
        textImage.getRGB(0, 0, width, height, pixels, 0, width);

        ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * 4);
        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            buffer.put((byte) ((pixel >> 16) & 0xFF)); // R
            buffer.put((byte) ((pixel >> 8) & 0xFF));  // G
            buffer.put((byte) (pixel & 0xFF));         // B
            buffer.put((byte) ((pixel >> 24) & 0xFF)); // A
        }
        buffer.flip();

        int textureId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1, 1, 1, 1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0, 0); GL11.glVertex2f(newX, newY);
        GL11.glTexCoord2f(1, 0); GL11.glVertex2f(newX + width, newY);
        GL11.glTexCoord2f(1, 1); GL11.glVertex2f(newX + width, newY + height);
        GL11.glTexCoord2f(0, 1); GL11.glVertex2f(newX, newY + height);
        GL11.glEnd();
        GL11.glDisable(GL11.GL_TEXTURE_2D);

        GL11.glDeleteTextures(textureId);
    }

    public static float getTextWidth(String text) {
        if (font == null) init();
        return (float) font.getStringBounds(text, new FontRenderContext(null, true, true)).getWidth();
    }

    public static float getTextHeight(String text) {
        if (font == null) init();
        return font.getSize();
    }


    public static void drawText(String text, float x, float y, Color color) {
        if (font == null) init();
        if (text == null || text.isEmpty()) return;

        int newX = (int) x;
        int newY = (int) y - 7;

        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setFont(font);
        FontMetrics fm = g2d.getFontMetrics();
        int width = fm.stringWidth(text);
        int height = fm.getHeight();
        g2d.dispose();

        BufferedImage textImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        g2d = textImage.createGraphics();
        g2d.setComposite(AlphaComposite.Clear);
        g2d.fillRect(0, 0, width, height);
        g2d.setComposite(AlphaComposite.SrcOver);
        g2d.setFont(font);
        g2d.setColor(color);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.drawString(text, 0, fm.getAscent());
        g2d.dispose();

        int[] pixels = new int[width * height];
        textImage.getRGB(0, 0, width, height, pixels, 0, width);

        ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * 4);
        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            buffer.put((byte) ((pixel >> 16) & 0xFF)); // R
            buffer.put((byte) ((pixel >> 8) & 0xFF));  // G
            buffer.put((byte) (pixel & 0xFF));         // B
            buffer.put((byte) ((pixel >> 24) & 0xFF)); // A
        }
        buffer.flip();

        int textureId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1, 1, 1, 1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0, 0); GL11.glVertex2f(newX, newY);
        GL11.glTexCoord2f(1, 0); GL11.glVertex2f(newX + width, newY);
        GL11.glTexCoord2f(1, 1); GL11.glVertex2f(newX + width, newY + height);
        GL11.glTexCoord2f(0, 1); GL11.glVertex2f(newX, newY + height);
        GL11.glEnd();
        GL11.glDisable(GL11.GL_TEXTURE_2D);

        GL11.glDeleteTextures(textureId);
    }

    public static void drawTextWithIcon(String text, float x, float y, Image icon, int iconSize) {
        int iconOffset = 0;
        if (icon != null) {
            BufferedImage iconImg = new BufferedImage(iconSize, iconSize, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = iconImg.createGraphics();
            g2d.setComposite(AlphaComposite.SrcOver);
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setBackground(new Color(0,0,0,0));
            g2d.clearRect(0, 0, iconSize, iconSize);
            g2d.drawImage(icon, 0, 0, iconSize, iconSize, null);
            g2d.dispose();

            int[] pixels = new int[iconSize * iconSize];
            iconImg.getRGB(0, 0, iconSize, iconSize, pixels, 0, iconSize);
            ByteBuffer buffer = ByteBuffer.allocateDirect(iconSize * iconSize * 4);
            for (int i = 0; i < pixels.length; i++) {
                int pixel = pixels[i];
                buffer.put((byte) ((pixel >> 16) & 0xFF)); // R
                buffer.put((byte) ((pixel >> 8) & 0xFF));  // G
                buffer.put((byte) (pixel & 0xFF));         // B
                buffer.put((byte) ((pixel >> 24) & 0xFF)); // A
            }
            buffer.flip();
            int textureId = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, iconSize, iconSize, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glColor4f(1, 1, 1, 1);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glTexCoord2f(0, 0); GL11.glVertex2f(x, y);
            GL11.glTexCoord2f(1, 0); GL11.glVertex2f(x + iconSize, y);
            GL11.glTexCoord2f(1, 1); GL11.glVertex2f(x + iconSize, y + iconSize);
            GL11.glTexCoord2f(0, 1); GL11.glVertex2f(x, y + iconSize);
            GL11.glEnd();
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glPopAttrib();
            iconOffset = iconSize + 6;
        }
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        drawText(text, x + iconOffset, y);
        GL11.glPopAttrib();
    }

    public static void setFontFromResource(String resourcePath, float size) {
        try (InputStream is = TextRenderer.class.getResourceAsStream(resourcePath)) {
            if (is == null) throw new IOException("Font not found: " + resourcePath);
            font = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(size);
        } catch (Exception e) {
            e.printStackTrace();
            if (font == null) font = new Font("SansSerif", Font.PLAIN, (int) size);
        }
    }

    public static void drawText(String text, float x, float y, Font customFont) {
        drawText(text, x, y, customFont, Color.BLACK);
    }

    public static void drawText(String text, float x, float y, Font customFont, Color color) {
        if (text == null || text.isEmpty()) return;
        if (customFont == null) {
            drawText(text, x, y);
            return;
        }
        int newX = (int) x;
        int newY = (int) y - 7;

        // Mesure
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setFont(customFont);
        FontMetrics fm = g2d.getFontMetrics();
        int width = fm.stringWidth(text);
        int height = fm.getHeight();
        g2d.dispose();
        if (width <= 0 || height <= 0) return;

        BufferedImage textImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        g2d = textImage.createGraphics();
        g2d.setComposite(AlphaComposite.Clear);
        g2d.fillRect(0, 0, width, height);
        g2d.setComposite(AlphaComposite.SrcOver);
        g2d.setFont(customFont);
        g2d.setColor(color);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.drawString(text, 0, fm.getAscent());
        g2d.dispose();

        int[] pixels = new int[width * height];
        textImage.getRGB(0, 0, width, height, pixels, 0, width);
        ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * 4);
        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            buffer.put((byte) ((pixel >> 16) & 0xFF));
            buffer.put((byte) ((pixel >> 8) & 0xFF));
            buffer.put((byte) (pixel & 0xFF));
            buffer.put((byte) ((pixel >> 24) & 0xFF));
        }
        buffer.flip();

        int textureId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1, 1, 1, 1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0, 0); GL11.glVertex2f(newX, newY);
        GL11.glTexCoord2f(1, 0); GL11.glVertex2f(newX + width, newY);
        GL11.glTexCoord2f(1, 1); GL11.glVertex2f(newX + width, newY + height);
        GL11.glTexCoord2f(0, 1); GL11.glVertex2f(newX, newY + height);
        GL11.glEnd();
        GL11.glDisable(GL11.GL_TEXTURE_2D);

        GL11.glDeleteTextures(textureId);
    }

    // Mesures avec police personnalisée
    public static float getTextWidth(String text, Font customFont) {
        if (customFont == null) return getTextWidth(text);
        return (float) customFont.getStringBounds(text, new FontRenderContext(null, true, true)).getWidth();
    }

    public static float getTextHeight(String text, Font customFont) {
        if (customFont == null) return getTextHeight(text);
        FontRenderContext frc = new FontRenderContext(null, true, true);
        return (float) customFont.getLineMetrics(text, frc).getHeight();
    }

    private static Color mcColor(char code, Color fallback) {
        return switch (Character.toLowerCase(code)) {
            case '0' -> new Color(0x000000);
            case '1' -> new Color(0x0000AA);
            case '2' -> new Color(0x00AA00);
            case '3' -> new Color(0x00AAAA);
            case '4' -> new Color(0xAA0000);
            case '5' -> new Color(0xAA00AA);
            case '6' -> new Color(0xFFAA00);
            case '7' -> new Color(0xAAAAAA);
            case '8' -> new Color(0x555555);
            case '9' -> new Color(0x5555FF);
            case 'a' -> new Color(0x55FF55);
            case 'b' -> new Color(0x55FFFF);
            case 'c' -> new Color(0xFF5555);
            case 'd' -> new Color(0xFF55FF);
            case 'e' -> new Color(0xFFFF55);
            case 'f' -> Color.WHITE;
            default -> fallback != null ? fallback : Color.WHITE;
        };
    }

    private record Seg(String text, Font font, Color color) {}

    private static java.util.List<Seg> parseFormatted(String text, Font baseFont, Color defaultColor) {
        java.util.List<Seg> segs = new java.util.ArrayList<>();
        if (text == null || text.isEmpty()) return segs;
        if (defaultColor == null) defaultColor = Color.WHITE;
        boolean bold = (baseFont.getStyle() & Font.BOLD) != 0;
        Color curColor = defaultColor;
        Font curFont = baseFont;
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '&' && i + 1 < text.length()) {
                if (buf.length() > 0) {
                    segs.add(new Seg(buf.toString(), curFont, curColor));
                    buf.setLength(0);
                }
                char code = text.charAt(++i);
                switch (Character.toLowerCase(code)) {
                    case 'l' -> {
                        bold = true;
                        curFont = baseFont.deriveFont(Font.BOLD, baseFont.getSize2D());
                    }
                    case 'r' -> {
                        bold = (baseFont.getStyle() & Font.BOLD) != 0;
                        curFont = bold ? baseFont.deriveFont(Font.BOLD, baseFont.getSize2D()) : baseFont.deriveFont(Font.PLAIN, baseFont.getSize2D());
                        curColor = defaultColor;
                    }
                    default -> {
                        Color c = mcColor(code, defaultColor);
                        curColor = c != null ? c : defaultColor;
                        curFont = bold ? baseFont.deriveFont(Font.BOLD, baseFont.getSize2D()) : baseFont.deriveFont(Font.PLAIN, baseFont.getSize2D());
                    }
                }
                continue;
            }
            buf.append(ch);
        }
        if (buf.length() > 0) segs.add(new Seg(buf.toString(), curFont, curColor));
        return segs;
    }

    public static float measureFormattedWidth(String text, Font baseFont) {
        if (text == null || text.isEmpty()) return 0f;
        java.util.List<Seg> segs = parseFormatted(text, baseFont, Color.WHITE);
        float w = 0f;
        FontRenderContext frc = new FontRenderContext(null, true, true);
        for (Seg s : segs) {
            w += (float) s.font.getStringBounds(s.text, frc).getWidth();
        }
        return w;
    }

    public static float measureFormattedHeight(String text, Font baseFont) {
        if (text == null || text.isEmpty()) return baseFont.getSize2D();
        java.util.List<Seg> segs = parseFormatted(text, baseFont, Color.WHITE);
        float h = 0f;
        FontRenderContext frc = new FontRenderContext(null, true, true);
        for (Seg s : segs) {
            float hh = (float) s.font.getLineMetrics(s.text, frc).getHeight();
            if (hh > h) h = hh;
        }
        return h > 0 ? h : baseFont.getSize2D();
    }

    public static void drawFormattedText(String text, float x, float y, Font baseFont, Color defaultColor) {
        if (text == null || text.isEmpty()) return;
        java.util.List<Seg> segs = parseFormatted(text, baseFont, defaultColor != null ? defaultColor : Color.WHITE);
        float cursorX = x;
        for (Seg s : segs) {
            if (s.text == null || s.text.isEmpty()) continue;
            // Mesure
            FontRenderContext frc = new FontRenderContext(null, true, true);
            float w = (float) s.font.getStringBounds(s.text, frc).getWidth();
            float h = (float) s.font.getLineMetrics(s.text, frc).getHeight();
            if (w <= 0 || h <= 0) continue;
            drawText(s.text, cursorX, y, s.font, s.color);
            cursorX += w;
        }
    }

    public static void setFontSize(float size) {
        if (font == null) init();
        font = font.deriveFont(size);
    }
}
