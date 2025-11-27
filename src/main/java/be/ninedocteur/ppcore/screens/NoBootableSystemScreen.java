package be.ninedocteur.ppcore.screens;

import be.ninedocteur.ppcore.BIOS;
import be.ninedocteur.ppcore.utils.Screen;

public class NoBootableSystemScreen implements Screen {
    private final BIOS bios;

    public NoBootableSystemScreen(BIOS bios) {
        this.bios = bios;
    }

    @Override
    public void render() {
        int w = org.lwjgl.opengl.Display.getWidth();
        int h = org.lwjgl.opengl.Display.getHeight();
        float fontSize = Math.max(18, h / 28f);
        java.awt.Font font;
        try {
            font = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, getClass().getResourceAsStream("/VGA.ttf")).deriveFont(fontSize);
        } catch (Exception e) {
            font = new java.awt.Font("Monospaced", java.awt.Font.PLAIN, (int)fontSize);
        }
        be.ninedocteur.ppcore.utils.TextRenderer.drawText("No bootable system found!", 20, 30, font, java.awt.Color.RED);
    }

    @Override
    public void handleInput() {
        while (org.lwjgl.input.Keyboard.next()) {
            if (org.lwjgl.input.Keyboard.getEventKeyState()) {
                int key = org.lwjgl.input.Keyboard.getEventKey();
                if (key == org.lwjgl.input.Keyboard.KEY_F1) {
                    bios.restart();
                    return;
                } else if (key == org.lwjgl.input.Keyboard.KEY_ESCAPE) {
                    bios.showPostScreen();
                    return;
                }
                if (key == org.lwjgl.input.Keyboard.KEY_F11) {
                    bios.toggleFullscreen();
                }
            }
        }
    }
}
