package be.ninedocteur.ppcore.screens;

import be.ninedocteur.ppcore.BIOS;
import be.ninedocteur.ppcore.utils.Screen;
import be.ninedocteur.ppcore.utils.Updater;

import java.util.Collections;

public class BIOSScreen implements Screen {
    private final BIOS bios;
    private int advancedSelection = 0; // 0: Secure Boot, 1: Fast Boot, 2: RAM
    private boolean advancedMiniGuiOpen = false;
    private int miniGuiValueIndex = 0;
    private static final int[] RAM_CHOICES_MB = {2048, 4096, 8192, 16384, 32768, 65536};

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
        else if (bios.getBiosTabIndex() == 2) renderBiosAdvancedTab(contentFont, boxX + 20, boxY + 30, w, h);
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
        String cpuModel = bios.getCpuModel();
        String cpuInfo = "CPU Model: " + cpuModel;
        String gpuModel = bios.getGpuModel();
        String gpuInfo = "GPU: " + gpuModel;
        String gpuDriverVersion = org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_VERSION);
        String gpuDriverInfo = "GPU Driver Version: " + (gpuDriverVersion != null ? gpuDriverVersion : "N/A");
        int screenW = org.lwjgl.opengl.Display.getWidth();
        int screenH = org.lwjgl.opengl.Display.getHeight();
        String screenInfo = "Screen Size: " + screenW + "x" + screenH;
        String buildDate = "Build Date: " + be.ninedocteur.ppcore.BIOS.BUILD_DATE;
        long totalRam = Runtime.getRuntime().maxMemory() / (1024 * 1024); // JVM RAM
        long installedRam = 0;
        try {
            java.lang.management.OperatingSystemMXBean osBean = java.lang.management.ManagementFactory.getOperatingSystemMXBean();
            if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                installedRam = ((com.sun.management.OperatingSystemMXBean) osBean).getTotalPhysicalMemorySize() / (1024 * 1024);
            }
        } catch (Exception e) {
            installedRam = 0;
        }
        String ramInfo = "Installed RAM: " + installedRam + " MB";
        String ramAllocated = "RAM Allocated to PopulaireCoreGL: " + bios.getAllocatedRamMB() + " MB";
        int diskCount = bios.getDetectedDisks().size();
        String diskInfo = "Disks Detected: " + diskCount;
        String uuidInfo = "System UUID: " + bios.getSystemUUID();
        String[] infos = {
            cpuInfo,
            gpuInfo,
            gpuDriverInfo,
            screenInfo,
            buildDate,
            ramInfo,
            ramAllocated,
            diskInfo,
            uuidInfo
        };
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
            bios.setUpdateMessage("Checking remote version...");
            new Thread(() -> {
                try {
                    String json = be.ninedocteur.ppcore.utils.Updater.readUrlToString(jsonUrl);
                    String remoteVersion = be.ninedocteur.ppcore.utils.UpdateManager.extractRemoteVersion(json, project);
                    boolean updateAvailable = (remoteVersion != null && !remoteVersion.equals(localVersion));
                    bios.setUpdateAvailable(updateAvailable);
                    bios.setUpdateMessage(updateAvailable ? ("New version available: " + remoteVersion + " (Enter to update)") : "Your version is up to date.");
                } catch (Exception e) {
                    bios.setUpdateMessage("Error while checking: " + e.getMessage());
                }
            }).start();
        }
        be.ninedocteur.ppcore.utils.TextRenderer.drawText("PopulaireCoreGL Update", x, y, font, java.awt.Color.YELLOW);
        be.ninedocteur.ppcore.utils.TextRenderer.drawText("Local version: " + localVersion, x, y + font.getSize2D() + 10, font, java.awt.Color.WHITE);
        be.ninedocteur.ppcore.utils.TextRenderer.drawText(bios.getUpdateMessage() != null ? bios.getUpdateMessage() : "", x, y + 2 * (font.getSize2D() + 10), font, java.awt.Color.CYAN);
        if (bios.isUpdateInProgress()) {
            be.ninedocteur.ppcore.utils.TextRenderer.drawText("Downloading and installing...", x, y + 4 * (font.getSize2D() + 10), font, java.awt.Color.ORANGE);
        }
    }

    private void renderBiosBootTab(java.awt.Font font, float x, float y, float w) {
        be.ninedocteur.ppcore.utils.TextRenderer.drawText("Boot Order (up/down to change, Enter to save)", x, y, font, java.awt.Color.YELLOW);
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
            be.ninedocteur.ppcore.utils.TextRenderer.drawText("(Not saved)", x, y + (line+1) * (font.getSize2D() + 10), font, java.awt.Color.ORANGE);
        }
    }

    private void renderBiosAdvancedTab(java.awt.Font font, float x, float y, int screenW, int screenH) {
        String[] options = {
            "Secure Boot: [AVAILABLE SOON]",
            "Fast Boot: " + (bios.isFastBootEnabled() ? "ENABLED" : "DISABLED"),
            "RAM: " + bios.getAllocatedRamMB() + " MB"
        };
        for (int i = 0; i < options.length; i++) {
            java.awt.Color color = (i == advancedSelection) ? java.awt.Color.YELLOW : java.awt.Color.WHITE;
            be.ninedocteur.ppcore.utils.TextRenderer.drawText(options[i], x, y + i * (font.getSize2D() + 20), font, color);
        }
        be.ninedocteur.ppcore.utils.TextRenderer.drawText("Use Up/Down to navigate, Enter to edit.", x, y + options.length * (font.getSize2D() + 30), font, java.awt.Color.CYAN);
        // Mini-GUI
        if (advancedMiniGuiOpen) {
            float rectW = 400, rectH = 120;
            float rectX = (screenW - rectW) / 2f;
            float rectY = (screenH - rectH) / 2f;
            org.lwjgl.opengl.GL11.glColor3f(0.1f, 0.2f, 0.7f);
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
            String label = "";
            if (advancedSelection == 1) {
                label = "Fast Boot: " + (miniGuiValueIndex == 1 ? "ENABLED" : "DISABLED");
            } else if (advancedSelection == 2) {
                label = "RAM: " + RAM_CHOICES_MB[miniGuiValueIndex] + " MB";
            }
            be.ninedocteur.ppcore.utils.TextRenderer.drawText("Edit", rectX + 30, rectY + 30, font, java.awt.Color.YELLOW);
            be.ninedocteur.ppcore.utils.TextRenderer.drawText(label, rectX + 30, rectY + 60, font, java.awt.Color.WHITE);
            // Shorter help text, centered
            String helpText = "←/→ change, Enter validate, Esc cancel";
            float helpTextWidth = be.ninedocteur.ppcore.utils.TextRenderer.getTextWidth(helpText, font);
            float helpTextX = rectX + (rectW - helpTextWidth) / 2f;
            be.ninedocteur.ppcore.utils.TextRenderer.drawText(helpText, helpTextX, rectY + 90, font, java.awt.Color.CYAN);
        }
    }

    private void renderBiosExitTab(java.awt.Font font, float x, float y) {
        String[] options = {
            "Exit saving changes (save, exit BIOS, reboot)",
            "Exit without saving changes (exit BIOS, reboot)"
        };
        for (int i = 0; i < options.length; i++) {
            java.awt.Color color = (i == bios.getExitSelection()) ? java.awt.Color.YELLOW : java.awt.Color.WHITE;
            be.ninedocteur.ppcore.utils.TextRenderer.drawText(options[i], x, y + i * (font.getSize2D() + 20), font, color);
        }
        be.ninedocteur.ppcore.utils.TextRenderer.drawText("Use Up/Down to select, Enter to validate.", x, y + options.length * (font.getSize2D() + 30), font, java.awt.Color.CYAN);
    }

    @Override
    public void handleInput() {
        while (org.lwjgl.input.Keyboard.next()) {
            if (org.lwjgl.input.Keyboard.getEventKeyState()) {
                int key = org.lwjgl.input.Keyboard.getEventKey();
                // Navigation entre onglets
                if (!advancedMiniGuiOpen) {
                    if (key == org.lwjgl.input.Keyboard.KEY_LEFT) {
                        bios.setBiosTabIndex((bios.getBiosTabIndex() + BIOS.getBiosTabs().length - 1) % BIOS.getBiosTabs().length);
                        return;
                    } else if (key == org.lwjgl.input.Keyboard.KEY_RIGHT) {
                        bios.setBiosTabIndex((bios.getBiosTabIndex() + 1) % BIOS.getBiosTabs().length);
                        return;
                    }
                }
                // Onglet Advanced
                if (bios.getBiosTabIndex() == 2) {
                    if (!advancedMiniGuiOpen) {
                        if (key == org.lwjgl.input.Keyboard.KEY_UP) {
                            advancedSelection = (advancedSelection + 2) % 3;
                        } else if (key == org.lwjgl.input.Keyboard.KEY_DOWN) {
                            advancedSelection = (advancedSelection + 1) % 3;
                        } else if (key == org.lwjgl.input.Keyboard.KEY_RETURN) {
                            if (advancedSelection == 1) { // Fast Boot
                                miniGuiValueIndex = bios.isFastBootEnabled() ? 1 : 0;
                                advancedMiniGuiOpen = true;
                            } else if (advancedSelection == 2) { // RAM
                                int current = bios.getAllocatedRamMB();
                                miniGuiValueIndex = 0;
                                for (int i = 0; i < RAM_CHOICES_MB.length; i++) {
                                    if (RAM_CHOICES_MB[i] >= current) { miniGuiValueIndex = i; break; }
                                }
                                advancedMiniGuiOpen = true;
                            }
                        }
                    } else {
                        if (advancedSelection == 1) { // Fast Boot
                            if (key == org.lwjgl.input.Keyboard.KEY_LEFT || key == org.lwjgl.input.Keyboard.KEY_RIGHT) {
                                miniGuiValueIndex = 1 - miniGuiValueIndex;
                            } else if (key == org.lwjgl.input.Keyboard.KEY_RETURN) {
                                bios.setFastBootEnabled(miniGuiValueIndex == 1);
                                bios.saveConfigToJson();
                                advancedMiniGuiOpen = false;
                            } else if (key == org.lwjgl.input.Keyboard.KEY_ESCAPE) {
                                advancedMiniGuiOpen = false;
                            }
                        } else if (advancedSelection == 2) { // RAM
                            int maxIdx = RAM_CHOICES_MB.length - 1;
                            if (key == org.lwjgl.input.Keyboard.KEY_LEFT && miniGuiValueIndex > 0) {
                                miniGuiValueIndex--;
                            } else if (key == org.lwjgl.input.Keyboard.KEY_RIGHT && miniGuiValueIndex < maxIdx) {
                                miniGuiValueIndex++;
                            } else if (key == org.lwjgl.input.Keyboard.KEY_RETURN) {
                                bios.setAllocatedRamMB(RAM_CHOICES_MB[miniGuiValueIndex]);
                                bios.saveConfigToJson();
                                advancedMiniGuiOpen = false;
                            } else if (key == org.lwjgl.input.Keyboard.KEY_ESCAPE) {
                                advancedMiniGuiOpen = false;
                            }
                        }
                    }
                }
                if (!advancedMiniGuiOpen) {
                    if (bios.getBiosTabIndex() == 1) {

                    } else if (bios.getBiosTabIndex() == 3 && key == org.lwjgl.input.Keyboard.KEY_RETURN && bios.isUpdateAvailable() && !bios.isUpdateInProgress()) {

                    } else if (bios.getBiosTabIndex() == 4) { // Exit tab
                        if (key == org.lwjgl.input.Keyboard.KEY_UP) {
                            bios.setExitSelection((bios.getExitSelection() + 1) % 2);
                        } else if (key == org.lwjgl.input.Keyboard.KEY_DOWN) {
                            bios.setExitSelection((bios.getExitSelection() + 1) % 2);
                        } else if (key == org.lwjgl.input.Keyboard.KEY_RETURN) {
                            if (bios.getExitSelection() == 0) {
                                bios.saveConfigToJson();
                            }
                            org.lwjgl.opengl.Display.destroy();
                            System.exit(0);
                        }
                    }
                }
                if (key == org.lwjgl.input.Keyboard.KEY_ESCAPE && !advancedMiniGuiOpen) {
                    bios.showPostScreen();
                    return;
                }
                if (key == org.lwjgl.input.Keyboard.KEY_F11 && !advancedMiniGuiOpen) {
                    bios.toggleFullscreen();
                }
            }
        }
    }
}
