package be.ninedocteur.ppcore.screens;

import be.ninedocteur.ppcore.BIOS;
import be.ninedocteur.ppcore.utils.Screen;
import be.ninedocteur.ppcore.utils.Updater;

import java.util.Collections;

public class BIOSScreen implements Screen {
    private final BIOS bios;

    public BIOSScreen(BIOS bios) {
        this.bios = bios;
    }

    @Override
    public void render() {
        int w = org.lwjgl.opengl.Display.getWidth();
        int h = org.lwjgl.opengl.Display.getHeight();
        org.lwjgl.opengl.GL11.glClearColor(0.07f, 0.13f, 0.45f, 1f);
        org.lwjgl.opengl.GL11.glClear(org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT);
        float tabFontSize = Math.max(18, h / 28f);
        float contentFontSize = Math.max(16, h / 36f);
        java.awt.Font tabFont, contentFont;
        try {
            tabFont = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, getClass().getResourceAsStream("/VGA.ttf")).deriveFont(tabFontSize);
            contentFont = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, getClass().getResourceAsStream("/VGA.ttf")).deriveFont(contentFontSize);
        } catch (Exception e) {
            tabFont = new java.awt.Font("Monospaced", java.awt.Font.PLAIN, (int)tabFontSize);
            contentFont = new java.awt.Font("Monospaced", java.awt.Font.PLAIN, (int)contentFontSize);
        }
        float tabX = 40;
        float tabY = 30;
        for (int i = 0; i < BIOS.getBiosTabs().length; i++) {
            String tab = BIOS.getBiosTabs()[i];
            java.awt.Color color = (i == bios.getBiosTabIndex()) ? java.awt.Color.YELLOW : java.awt.Color.WHITE;
            be.ninedocteur.ppcore.utils.TextRenderer.drawText(tab, tabX, tabY, tabFont, color);
            tabX += be.ninedocteur.ppcore.utils.TextRenderer.getTextWidth(tab, tabFont) + 60;
        }
        float boxX = 30, boxY = 60, boxW = w - 60, boxH = h - 90;
        org.lwjgl.opengl.GL11.glColor3f(1f, 1f, 1f);
        org.lwjgl.opengl.GL11.glLineWidth(2f);
        org.lwjgl.opengl.GL11.glBegin(org.lwjgl.opengl.GL11.GL_LINE_LOOP);
        org.lwjgl.opengl.GL11.glVertex2f(boxX, boxY);
        org.lwjgl.opengl.GL11.glVertex2f(boxX + boxW, boxY);
        org.lwjgl.opengl.GL11.glVertex2f(boxX + boxW, boxY + boxH);
        org.lwjgl.opengl.GL11.glVertex2f(boxX, boxY + boxH);
        org.lwjgl.opengl.GL11.glEnd();
        if (bios.getBiosTabIndex() == 0) renderBiosInfoTab(contentFont, boxX + 20, boxY + 30);
        else if (bios.getBiosTabIndex() == 1) renderBiosBootTab(contentFont, boxX + 20, boxY + 30, boxW - 40);
        else if (bios.getBiosTabIndex() == 3) renderBiosUpdatesTab(contentFont, boxX + 20, boxY + 30, boxW - 40);
        else if (bios.getBiosTabIndex() == 4) renderBiosExitTab(contentFont, boxX + 20, boxY + 30);
    }

    private void renderBiosInfoTab(java.awt.Font font, float x, float y) {
        int line = 0;
        if (bios.isIntelXeBugDetected()) {
            float warnFontSize = Math.max(14, font.getSize2D());
            java.awt.Font warnFont;
            try {
                warnFont = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, getClass().getResourceAsStream("/VGA.ttf")).deriveFont(warnFontSize);
            } catch (Exception e) {
                warnFont = new java.awt.Font("Monospaced", java.awt.Font.PLAIN, (int)warnFontSize);
            }
            float rectX = x;
            float rectY = y;
            float rectW = 700;
            java.util.List<String> lines = be.ninedocteur.ppcore.utils.TextRenderer.wrapTextToWidth(be.ninedocteur.ppcore.BIOS.getIntelXeWarning(), warnFont, rectW - 20);
            float lineHeight = warnFont.getSize2D() + 4;
            float rectH = lines.size() * lineHeight + 40;
            org.lwjgl.opengl.GL11.glColor3f(1f, 0f, 0f);
            org.lwjgl.opengl.GL11.glBegin(org.lwjgl.opengl.GL11.GL_QUADS);
            org.lwjgl.opengl.GL11.glVertex2f(rectX, rectY);
            org.lwjgl.opengl.GL11.glVertex2f(rectX + rectW, rectY);
            org.lwjgl.opengl.GL11.glVertex2f(rectX + rectW, rectY + rectH);
            org.lwjgl.opengl.GL11.glVertex2f(rectX, rectY + rectH);
            org.lwjgl.opengl.GL11.glEnd();
            org.lwjgl.opengl.GL11.glColor3f(1f, 1f, 1f);
            org.lwjgl.opengl.GL11.glLineWidth(2f);
            org.lwjgl.opengl.GL11.glBegin(org.lwjgl.opengl.GL11.GL_LINE_LOOP);
            org.lwjgl.opengl.GL11.glVertex2f(rectX, rectY);
            org.lwjgl.opengl.GL11.glVertex2f(rectX + rectW, rectY);
            org.lwjgl.opengl.GL11.glVertex2f(rectX + rectW, rectY + rectH);
            org.lwjgl.opengl.GL11.glVertex2f(rectX, rectY + rectH);
            org.lwjgl.opengl.GL11.glEnd();
            float textY = rectY + 10 + warnFont.getSize2D();
            for (String lineStr : lines) {
                float textW = be.ninedocteur.ppcore.utils.TextRenderer.getTextWidth(lineStr, warnFont);
                float textX = rectX + (rectW - textW) / 2f;
                be.ninedocteur.ppcore.utils.TextRenderer.drawText(lineStr, textX, textY, warnFont, java.awt.Color.WHITE);
                textY += lineHeight;
            }
            y += rectH + 10;
        }
        String cpuName = System.getProperty("os.arch");
        String cpuInfo = "CPU: " + cpuName;
        String gpuName = org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_RENDERER);
        String gpuInfo = "GPU: " + (gpuName != null ? gpuName : "Java OpenGL");
        String gpuDriverVersion = org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_VERSION);
        String gpuDriverInfo = "Version driver GPU: " + (gpuDriverVersion != null ? gpuDriverVersion : "N/A");
        long maxMem = Runtime.getRuntime().maxMemory() / (1024 * 1024);
        long totalMem = Runtime.getRuntime().totalMemory() / (1024 * 1024);
        long freeMem = Runtime.getRuntime().freeMemory() / (1024 * 1024);
        long usedMem = totalMem - freeMem;
        String ramInfo = String.format("RAM JVM: %d MB (utilisée: %d MB / max: %d MB)", totalMem, usedMem, maxMem);
        String javaInfo = "Java: " + System.getProperty("java.version");
        int ramPercent = (int) ((usedMem * 100) / maxMem);
        String ramUsage = String.format("Utilisation RAM: %d%%", ramPercent);
        String cpuUsage = "Utilisation CPU: N/A";
        try {
            com.sun.management.OperatingSystemMXBean osBean =
                (com.sun.management.OperatingSystemMXBean) java.lang.management.ManagementFactory.getOperatingSystemMXBean();
            double cpuLoad = osBean.getProcessCpuLoad();
            if (cpuLoad >= 0) {
                cpuUsage = String.format("Utilisation CPU: %.1f%%", cpuLoad * 100);
            }
        } catch (Exception ignored) {}
        String gpuUsage = "Utilisation GPU: N/A";
        String space = "     ";
        String header = "=== Populaire Core BIOS Informations ===";
        String version = "Version BIOS: " + be.ninedocteur.ppcore.PPCoreSharedConstant.version;
        String[] infos = {cpuInfo, gpuInfo, gpuDriverInfo, ramInfo, javaInfo, ramUsage, cpuUsage, gpuUsage, space, header, version};
        for (String info : infos) {
            be.ninedocteur.ppcore.utils.TextRenderer.drawText(info, x, y + line * (font.getSize2D() + 10), font, java.awt.Color.WHITE);
            line++;
        }
    }

    private void renderBiosUpdatesTab(java.awt.Font font, float x, float y, float w) {
        String jsonUrl = "https://raw.githubusercontent.com/OpenDeskOS-Team/OpenDesk-Updater/refs/heads/main/update_index.json";
        String project = "ppcore";
        String localVersion = be.ninedocteur.ppcore.PPCoreSharedConstant.version;
        if (bios.getUpdateMessage() == null && !bios.isUpdateInProgress()) {
            bios.setUpdateMessage("Vérification de la version distante...");
            new Thread(() -> {
                try {
                    String json = be.ninedocteur.ppcore.utils.Updater.readUrlToString(jsonUrl);
                    String remoteVersion = be.ninedocteur.ppcore.utils.UpdateManager.extractRemoteVersion(json, project);
                    boolean updateAvailable = (remoteVersion != null && !remoteVersion.equals(localVersion));
                    bios.setUpdateAvailable(updateAvailable);
                    bios.setUpdateMessage(updateAvailable ? ("Nouvelle version disponible: " + remoteVersion + " (Entrée pour mettre à jour)") : "Votre version est à jour.");
                } catch (Exception e) {
                    bios.setUpdateMessage("Erreur lors de la vérification: " + e.getMessage());
                }
            }).start();
        }
        be.ninedocteur.ppcore.utils.TextRenderer.drawText("Mise à jour PopulaireCoreGL", x, y, font, java.awt.Color.YELLOW);
        be.ninedocteur.ppcore.utils.TextRenderer.drawText("Version locale: " + localVersion, x, y + font.getSize2D() + 10, font, java.awt.Color.WHITE);
        be.ninedocteur.ppcore.utils.TextRenderer.drawText(bios.getUpdateMessage() != null ? bios.getUpdateMessage() : "", x, y + 2 * (font.getSize2D() + 10), font, java.awt.Color.CYAN);
        if (bios.isUpdateInProgress()) {
            be.ninedocteur.ppcore.utils.TextRenderer.drawText("Téléchargement et installation en cours...", x, y + 4 * (font.getSize2D() + 10), font, java.awt.Color.ORANGE);
        }
    }

    private void renderBiosBootTab(java.awt.Font font, float x, float y, float w) {
        be.ninedocteur.ppcore.utils.TextRenderer.drawText("Boot Order (haut/bas pour changer, Entrée pour sauvegarder)", x, y, font, java.awt.Color.YELLOW);
        int line = 1;
        java.util.List<java.io.File> disks = bios.getDetectedDisks();
        for (int i = 0; i < disks.size(); i++) {
            java.io.File disk = disks.get(i);
            String name = disk.getParentFile().getName() + ": " + disk.getName();
            java.awt.Color color = (i == bios.getBootOrderSelection()) ? java.awt.Color.CYAN : java.awt.Color.WHITE;
            be.ninedocteur.ppcore.utils.TextRenderer.drawText((i+1)+". "+name, x, y + line * (font.getSize2D() + 10), font, color);
            line++;
        }
        if (bios.isBootOrderChanged()) {
            be.ninedocteur.ppcore.utils.TextRenderer.drawText("(Non sauvegardé)", x, y + (line+1) * (font.getSize2D() + 10), font, java.awt.Color.ORANGE);
        }
    }

    private void renderBiosExitTab(java.awt.Font font, float x, float y) {
        String[] options = {
            "Exit saving changes (enregistre, quitte le BIOS, redémarre)",
            "Exit without saving changes (quitte le BIOS, redémarre)"
        };
        for (int i = 0; i < options.length; i++) {
            java.awt.Color color = (i == bios.getExitSelection()) ? java.awt.Color.YELLOW : java.awt.Color.WHITE;
            be.ninedocteur.ppcore.utils.TextRenderer.drawText(options[i], x, y + i * (font.getSize2D() + 20), font, color);
        }
        be.ninedocteur.ppcore.utils.TextRenderer.drawText("Utilise Haut/Bas pour choisir, Entrée pour valider.", x, y + options.length * (font.getSize2D() + 30), font, java.awt.Color.CYAN);
    }

    @Override
    public void handleInput() {
        while (org.lwjgl.input.Keyboard.next()) {
            if (org.lwjgl.input.Keyboard.getEventKeyState()) {
                int key = org.lwjgl.input.Keyboard.getEventKey();
                // Navigation entre onglets
                if (key == org.lwjgl.input.Keyboard.KEY_LEFT) {
                    bios.setBiosTabIndex((bios.getBiosTabIndex() + BIOS.getBiosTabs().length - 1) % BIOS.getBiosTabs().length);
                } else if (key == org.lwjgl.input.Keyboard.KEY_RIGHT) {
                    bios.setBiosTabIndex((bios.getBiosTabIndex() + 1) % BIOS.getBiosTabs().length);
                } else if (bios.getBiosTabIndex() == 1) { // Onglet Boot
                    if (key == org.lwjgl.input.Keyboard.KEY_UP) {
                        if (bios.getBootOrderSelection() > 0) {
                            Collections.swap(bios.getDetectedDisks(), bios.getBootOrderSelection(), bios.getBootOrderSelection() - 1);
                            bios.setBootOrderSelection(bios.getBootOrderSelection() - 1);
                            bios.setBootOrderChanged(true);
                        }
                    } else if (key == org.lwjgl.input.Keyboard.KEY_DOWN) {
                        if (bios.getBootOrderSelection() < bios.getDetectedDisks().size() - 1) {
                            Collections.swap(bios.getDetectedDisks(), bios.getBootOrderSelection(), bios.getBootOrderSelection() + 1);
                            bios.setBootOrderSelection(bios.getBootOrderSelection() + 1);
                            bios.setBootOrderChanged(true);
                        }
                    } else if (key == org.lwjgl.input.Keyboard.KEY_RETURN && bios.isBootOrderChanged()) {
                        bios.saveBootOrderConfig();
                        bios.setBootOrderChanged(false);
                    }
                } else if (bios.getBiosTabIndex() == 3 && key == org.lwjgl.input.Keyboard.KEY_RETURN && bios.isUpdateAvailable() && !bios.isUpdateInProgress()) {
                    bios.setUpdateInProgress(true);
                    bios.setUpdateMessage("Téléchargement en cours...");
                    new Thread(() -> {
                        try {
                            Updater.downloadLastVersion(
                                "https://raw.githubusercontent.com/OpenDeskOS-Team/OpenDesk-Updater/refs/heads/main/update_index.json",
                                "ppcore",
                                new java.io.File(BIOS.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath()
                            );
                            bios.setUpdateMessage("Mise à jour terminée. Redémarrage...");
                            bios.relaunchWithUpdatedJar();
                        } catch (Exception e) {
                            bios.setUpdateMessage("Erreur MAJ: " + e.getMessage());
                            bios.setUpdateInProgress(false);
                        }
                    }).start();
                } else if (bios.getBiosTabIndex() == 4) { // Onglet Exit
                    if (key == org.lwjgl.input.Keyboard.KEY_UP) {
                        bios.setExitSelection((bios.getExitSelection() + 1) % 2);
                    } else if (key == org.lwjgl.input.Keyboard.KEY_DOWN) {
                        bios.setExitSelection((bios.getExitSelection() + 1) % 2);
                    } else if (key == org.lwjgl.input.Keyboard.KEY_RETURN) {
                        if (bios.getExitSelection() == 0) {
                            bios.saveBootOrderConfig();
                            bios.restart();
                        } else {
                            bios.restart();
                        }
                    }
                }
                // ESC pour retour à l'écran de post
                if (key == org.lwjgl.input.Keyboard.KEY_ESCAPE) {
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
