package be.ninedocteur.ppcore.screens;

import be.ninedocteur.ppcore.BIOS;
import be.ninedocteur.ppcore.Main;
import be.ninedocteur.ppcore.utils.Screen;

import java.io.File;

public class BootMenuScreen implements Screen {
    private final BIOS bios;

    public BootMenuScreen(BIOS bios) {
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
        String title = "Boot Menu (F12)";
        be.ninedocteur.ppcore.utils.TextRenderer.drawText(title, 40, 40, font, java.awt.Color.YELLOW);
        be.ninedocteur.ppcore.utils.TextRenderer.drawText("Utilise Haut/Bas pour choisir, Entrée pour booter", 40, 80, font, java.awt.Color.CYAN);
        java.util.List<java.io.File> disks = bios.getDetectedDisks();
        for (int i = 0; i < disks.size(); i++) {
            java.io.File disk = disks.get(i);
            String name = disk.getParentFile().getName() + ": " + disk.getName();
            java.awt.Color color = (i == bios.getBootMenuSelection()) ? java.awt.Color.GREEN : java.awt.Color.WHITE;
            be.ninedocteur.ppcore.utils.TextRenderer.drawText((i+1)+". "+name, 60, 120 + i * (font.getSize2D() + 10), font, color);
        }
    }

    @Override
    public void handleInput() {
        while (org.lwjgl.input.Keyboard.next()) {
            if (org.lwjgl.input.Keyboard.getEventKeyState()) {
                int key = org.lwjgl.input.Keyboard.getEventKey();
                if (key == org.lwjgl.input.Keyboard.KEY_UP) {
                    if (bios.getBootMenuSelection() > 0) bios.setBootMenuSelection(bios.getBootMenuSelection() - 1);
                } else if (key == org.lwjgl.input.Keyboard.KEY_DOWN) {
                    if (bios.getBootMenuSelection() < bios.getDetectedDisks().size() - 1) bios.setBootMenuSelection(bios.getBootMenuSelection() + 1);
                } else if (key == org.lwjgl.input.Keyboard.KEY_RETURN && !bios.getDetectedDisks().isEmpty()) {
                    java.io.File disk = bios.getDetectedDisks().get(bios.getBootMenuSelection());
                    Main.bootExternalOS(new String[]{disk.getAbsolutePath()});
                    // Après boot, retour à l'écran de post
                    bios.showPostScreen();
                    return;
                } else if (key == org.lwjgl.input.Keyboard.KEY_ESCAPE) {
                    bios.showPostScreen();
                    return;
                }
                // Plein écran
                if (key == org.lwjgl.input.Keyboard.KEY_F11) {
                    bios.toggleFullscreen();
                }
            }
        }
    }
}
