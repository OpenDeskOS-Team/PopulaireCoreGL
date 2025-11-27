package be.ninedocteur.ppcore;

import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.PixelFormat;

import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;
import java.util.stream.Collectors;

public class BIOS {
    private boolean fullscreen = true;
    private final int windowedWidth = 1280;
    private final int windowedHeight = 720;
    private boolean biosMode = false;
    private int biosTabIndex = 0;
    private static final String[] BIOS_TABS = {"Informations", "Boot", "Advanced", "Updates", "Exit"};
    private boolean updateAvailable = false;
    private String remoteVersion = null;
    private boolean updateInProgress = false;
    private String updateMessage = null;
    private List<File> detectedDisks = new ArrayList<>();
    private int bootOrderSelection = 0;
    private static final String CONFIG_FILE = "config.json";
    private List<String> bootOrder = new ArrayList<>();
    private boolean bootOrderChanged = false;
    private boolean showBootText = true;
    private long splashStartTime = 0;
    private boolean noBootableSystem = false;
    private boolean forceRedraw = false;
    private int exitSelection = 0; // 0: save & exit, 1: exit without saving
    private boolean bootMenuActive = false;
    private int bootMenuSelection = 0;

    private boolean intelXeBugDetected = false;
    private static final String INTEL_XE_WARNING = "Warning! We detected that you have a Intel Xe Graphics Card, please note that a bug has been detected on version 32.0.101.7077 of the GPU Driver which may cause problems with OpenGL";

    public void run() {
        loadBootOrderConfig();
        scanDisks();
        init();
        detectIntelXeBug();
        splashStartTime = System.currentTimeMillis();
        showBootText = true;
        loop();
        Display.destroy();
    }

    private void detectIntelXeBug() {
        String gpuName = org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_RENDERER);
        String glVersion = org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_VERSION);
        if (gpuName != null && gpuName.contains("Intel(R) Iris(R) Xe Graphics") && glVersion != null && glVersion.contains("32.0.101.7077")) {
            intelXeBugDetected = true;
        }
    }

    private void init() {
        try {
            setDisplayMode(fullscreen);
            Display.setTitle("PopulaireCore BIOS");
            Display.create(new PixelFormat().withAlphaBits(8));
            setOrtho();
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        } catch (LWJGLException e) {
            throw new RuntimeException(e);
        }
    }

    private void setDisplayMode(boolean fullscreen) throws LWJGLException {
        if (fullscreen) {
            DisplayMode displayMode = Display.getDesktopDisplayMode();
            Display.setDisplayModeAndFullscreen(displayMode);
        } else {
            Display.setDisplayMode(new DisplayMode(windowedWidth, windowedHeight));
            Display.setFullscreen(false);
        }
    }

    private void setOrtho() {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(0, Display.getWidth(), Display.getHeight(), 0, -1, 1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
    }

    private void toggleFullscreen() {
        try {
            fullscreen = !fullscreen;
            setDisplayMode(fullscreen);
            setOrtho();
            TextRenderer.init();
            System.out.println("[DEBUG] Passage en " + (fullscreen ? "plein écran" : "fenêtré") + ", TextRenderer réinitialisé.");
            forceRedraw = true;
        } catch (LWJGLException e) {
            throw new RuntimeException(e);
        }
    }

    private void loop() {
        boolean booted = false;
        while (!Display.isCloseRequested()) {
            if (Display.wasResized()) {
                setOrtho();
                forceRedraw = true;
            }
            if (forceRedraw) {
                GL11.glClearColor(0f, 0f, 0f, 1f);
                GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
                setOrtho();
                forceRedraw = false;
            }
            GL11.glClearColor(0f, 0f, 0f, 1f);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            setOrtho();
            if (bootMenuActive) {
                renderBootMenu();
            } else if (noBootableSystem) {
                renderNoBootableSystem();
            } else if (!biosMode) {
                long elapsed = System.currentTimeMillis() - splashStartTime;
                showBootText = elapsed < 5000;
                renderSplashScreen();
                if (!showBootText && !booted && elapsed > 6000) {
                    if (Main.bootExternalOS(new String[0])) {
                        break;
                    } else {
                        noBootableSystem = true;
                    }
                    booted = true;
                }
            } else {
                renderBiosScreen();
            }
            Display.update();
            handleInput();
        }
    }

    private List<String> wrapTextToWidth(String text, java.awt.Font font, float maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            float lineWidth = TextRenderer.getTextWidth(testLine, font);
            if (lineWidth > maxWidth && currentLine.length() > 0) {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            } else {
                if (currentLine.length() > 0) currentLine.append(" ");
                currentLine.append(word);
            }
        }
        if (currentLine.length() > 0) lines.add(currentLine.toString());
        return lines;
    }

    private void renderSplashScreen() {
        int w = Display.getWidth();
        int h = Display.getHeight();
        float mainFontSize = Math.max(24, h / 10f);
        float infoFontSize = Math.max(12, h / 30f);
        java.awt.Font mainFont = null;
        java.awt.Font infoFont = null;
        try {
            mainFont = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, getClass().getResourceAsStream("/VGA.ttf")).deriveFont(mainFontSize);
            infoFont = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, getClass().getResourceAsStream("/VGA.ttf")).deriveFont(infoFontSize);
        } catch (Exception e) {
            mainFont = new java.awt.Font("Monospaced", java.awt.Font.PLAIN, (int)mainFontSize);
            infoFont = new java.awt.Font("Monospaced", java.awt.Font.PLAIN, (int)infoFontSize);
        }
        if (intelXeBugDetected) {
            float warnFontSize = Math.max(14, h / 40f);
            java.awt.Font warnFont;
            try {
                warnFont = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, getClass().getResourceAsStream("/VGA.ttf")).deriveFont(warnFontSize);
            } catch (Exception e) {
                warnFont = new java.awt.Font("Monospaced", java.awt.Font.PLAIN, (int)warnFontSize);
            }
            float rectX = 40;
            float rectY = 10;
            float rectW = w - 80;
            List<String> lines = wrapTextToWidth(INTEL_XE_WARNING, warnFont, rectW - 20);
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
                TextRenderer.drawText(line, textX, textY, warnFont, java.awt.Color.WHITE);
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
        TextRenderer.drawText(mainText, mainX, mainY, mainFont, new java.awt.Color(0,255,0));
        if (showBootText) {
            TextRenderer.drawText(infoText, infoX, infoY, infoFont, java.awt.Color.WHITE);
        }
    }

    private void renderBiosScreen() {
        int w = Display.getWidth();
        int h = Display.getHeight();
        GL11.glClearColor(0.07f, 0.13f, 0.45f, 1f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
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
        for (int i = 0; i < BIOS_TABS.length; i++) {
            String tab = BIOS_TABS[i];
            java.awt.Color color = (i == biosTabIndex) ? java.awt.Color.YELLOW : java.awt.Color.WHITE;
            TextRenderer.drawText(tab, tabX, tabY, tabFont, color);
            tabX += TextRenderer.getTextWidth(tab, tabFont) + 60;
        }
        float boxX = 30, boxY = 60, boxW = w - 60, boxH = h - 90;
        GL11.glColor3f(1f, 1f, 1f);
        GL11.glLineWidth(2f);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2f(boxX, boxY);
        GL11.glVertex2f(boxX + boxW, boxY);
        GL11.glVertex2f(boxX + boxW, boxY + boxH);
        GL11.glVertex2f(boxX, boxY + boxH);
        GL11.glEnd();
        if (biosTabIndex == 0) renderBiosInfoTab(contentFont, boxX + 20, boxY + 30);
        else if (biosTabIndex == 1) renderBiosBootTab(contentFont, boxX + 20, boxY + 30, boxW - 40);
        else if (biosTabIndex == 3) renderBiosUpdatesTab(contentFont, boxX + 20, boxY + 30, boxW - 40);
        else if (biosTabIndex == 4) renderBiosExitTab(contentFont, boxX + 20, boxY + 30);
    }

    private void renderBiosInfoTab(java.awt.Font font, float x, float y) {
        int line = 0;
        if (intelXeBugDetected) {
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
            List<String> lines = wrapTextToWidth(INTEL_XE_WARNING, warnFont, rectW - 20);
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
            for (String lineStr : lines) {
                float textW = TextRenderer.getTextWidth(lineStr, warnFont);
                float textX = rectX + (rectW - textW) / 2f;
                TextRenderer.drawText(lineStr, textX, textY, warnFont, java.awt.Color.WHITE);
                textY += lineHeight;
            }
            y += rectH + 10;
        }
        String cpuName = System.getProperty("os.arch");
        String cpuInfo = "CPU: " + cpuName;
        String gpuName = org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_RENDERER);
        String gpuInfo = "GPU: " + (gpuName != null ? gpuName : "Java OpenGL");

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
        String version = "Version BIOS: " + PPCoreSharedConstant.version;
        String[] infos = {cpuInfo, gpuInfo, ramInfo, javaInfo, ramUsage, cpuUsage, gpuUsage, space, header, version};
        for (String info : infos) {
            TextRenderer.drawText(info, x, y + line * (font.getSize2D() + 10), font, java.awt.Color.WHITE);
            line++;
        }
    }

    private void renderBiosUpdatesTab(java.awt.Font font, float x, float y, float w) {
        String jsonUrl = "https://raw.githubusercontent.com/OpenDeskOS-Team/OpenDesk-Updater/refs/heads/main/update_index.json";
        String project = "ppcore";
        String localVersion = PPCoreSharedConstant.version;
        if (remoteVersion == null && !updateInProgress) {
            updateMessage = "Vérification de la version distante...";

            new Thread(() -> {
                try {
                    String json = Updater.readUrlToString(jsonUrl);
                    remoteVersion = Updater.extractRemoteVersion(json, project);
                    updateAvailable = (remoteVersion != null && !remoteVersion.equals(localVersion));
                    updateMessage = updateAvailable ? ("Nouvelle version disponible: " + remoteVersion + " (Entrée pour mettre à jour)") : "Votre version est à jour.";
                } catch (Exception e) {
                    updateMessage = "Erreur lors de la vérification: " + e.getMessage();
                }
            }).start();
        }
        TextRenderer.drawText("Mise à jour PopulaireCoreGL", x, y, font, java.awt.Color.YELLOW);
        TextRenderer.drawText("Version locale: " + localVersion, x, y + font.getSize2D() + 10, font, java.awt.Color.WHITE);
        TextRenderer.drawText("Version distante: " + (remoteVersion != null ? remoteVersion : "..."), x, y + 2 * (font.getSize2D() + 10), font, java.awt.Color.WHITE);
        TextRenderer.drawText(updateMessage != null ? updateMessage : "", x, y + 3 * (font.getSize2D() + 10), font, java.awt.Color.CYAN);
        if (updateInProgress) {
            TextRenderer.drawText("Téléchargement et installation en cours...", x, y + 5 * (font.getSize2D() + 10), font, java.awt.Color.ORANGE);
        }
    }

    private void renderBiosBootTab(java.awt.Font font, float x, float y, float w) {
        TextRenderer.drawText("Boot Order (haut/bas pour changer, Entrée pour sauvegarder)", x, y, font, java.awt.Color.YELLOW);
        int line = 1;
        for (int i = 0; i < detectedDisks.size(); i++) {
            File disk = detectedDisks.get(i);
            String name = disk.getParentFile().getName() + ": " + disk.getName();
            java.awt.Color color = (i == bootOrderSelection) ? java.awt.Color.CYAN : java.awt.Color.WHITE;
            TextRenderer.drawText((i+1)+". "+name, x, y + line * (font.getSize2D() + 10), font, color);
            line++;
        }
        if (bootOrderChanged) {
            TextRenderer.drawText("(Non sauvegardé)", x, y + (line+1) * (font.getSize2D() + 10), font, java.awt.Color.ORANGE);
        }
    }

    private void renderBiosExitTab(java.awt.Font font, float x, float y) {
        String[] options = {
            "Exit saving changes (enregistre, quitte le BIOS, redémarre)",
            "Exit without saving changes (quitte le BIOS, redémarre)"
        };
        for (int i = 0; i < options.length; i++) {
            java.awt.Color color = (i == exitSelection) ? java.awt.Color.YELLOW : java.awt.Color.WHITE;
            TextRenderer.drawText(options[i], x, y + i * (font.getSize2D() + 20), font, color);
        }
        TextRenderer.drawText("Utilise Haut/Bas pour choisir, Entrée pour valider.", x, y + options.length * (font.getSize2D() + 30), font, java.awt.Color.CYAN);
    }

    private void renderNoBootableSystem() {
        int w = Display.getWidth();
        int h = Display.getHeight();
        float fontSize = Math.max(18, h / 28f);
        java.awt.Font font;
        try {
            font = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, getClass().getResourceAsStream("/VGA.ttf")).deriveFont(fontSize);
        } catch (Exception e) {
            font = new java.awt.Font("Monospaced", java.awt.Font.PLAIN, (int)fontSize);
        }
        TextRenderer.drawText("No bootable system found!", 20, 30, font, java.awt.Color.RED);
    }

    private void renderBootMenu() {
        int w = Display.getWidth();
        int h = Display.getHeight();
        float fontSize = Math.max(18, h / 28f);
        java.awt.Font font;
        try {
            font = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, getClass().getResourceAsStream("/VGA.ttf")).deriveFont(fontSize);
        } catch (Exception e) {
            font = new java.awt.Font("Monospaced", java.awt.Font.PLAIN, (int)fontSize);
        }
        String title = "Boot Menu (F12)";
        TextRenderer.drawText(title, 40, 40, font, java.awt.Color.YELLOW);
        TextRenderer.drawText("Utilise Haut/Bas pour choisir, Entrée pour booter", 40, 80, font, java.awt.Color.CYAN);
        for (int i = 0; i < detectedDisks.size(); i++) {
            File disk = detectedDisks.get(i);
            String name = disk.getParentFile().getName() + ": " + disk.getName();
            java.awt.Color color = (i == bootMenuSelection) ? java.awt.Color.GREEN : java.awt.Color.WHITE;
            TextRenderer.drawText((i+1)+". "+name, 60, 120 + i * (font.getSize2D() + 10), font, color);
        }
    }

    private void handleInput() {
        while (Keyboard.next()) {
            if (Keyboard.getEventKeyState()) {
                if (bootMenuActive) {
                    if (Keyboard.getEventKey() == Keyboard.KEY_UP) {
                        if (bootMenuSelection > 0) bootMenuSelection--;
                    } else if (Keyboard.getEventKey() == Keyboard.KEY_DOWN) {
                        if (bootMenuSelection < detectedDisks.size() - 1) bootMenuSelection++;
                    } else if (Keyboard.getEventKey() == Keyboard.KEY_RETURN && detectedDisks.size() > 0) {

                        File disk = detectedDisks.get(bootMenuSelection);
                        Main.bootExternalOS(new String[]{disk.getAbsolutePath()});
                        bootMenuActive = false;
                    } else if (Keyboard.getEventKey() == Keyboard.KEY_ESCAPE) {
                        bootMenuActive = false;
                    }
                    return;
                }
                if (!biosMode && (Keyboard.getEventKey() == Keyboard.KEY_F2 || Keyboard.getEventKey() == Keyboard.KEY_DELETE)) {
                    biosMode = true;
                } else if (!biosMode && Keyboard.getEventKey() == Keyboard.KEY_F12) {
                    bootMenuActive = true;
                    bootMenuSelection = 0;
                } else if (biosMode) {
                    if (Keyboard.getEventKey() == Keyboard.KEY_LEFT) {
                        biosTabIndex = (biosTabIndex + BIOS_TABS.length - 1) % BIOS_TABS.length;
                    } else if (Keyboard.getEventKey() == Keyboard.KEY_RIGHT) {
                        biosTabIndex = (biosTabIndex + 1) % BIOS_TABS.length;
                    } else if (biosTabIndex == 1) {

                        if (Keyboard.getEventKey() == Keyboard.KEY_UP) {
                            if (bootOrderSelection > 0) {
                                Collections.swap(detectedDisks, bootOrderSelection, bootOrderSelection - 1);
                                bootOrderSelection--;
                                bootOrderChanged = true;
                            }
                        } else if (Keyboard.getEventKey() == Keyboard.KEY_DOWN) {
                            if (bootOrderSelection < detectedDisks.size() - 1) {
                                Collections.swap(detectedDisks, bootOrderSelection, bootOrderSelection + 1);
                                bootOrderSelection++;
                                bootOrderChanged = true;
                            }
                        } else if (Keyboard.getEventKey() == Keyboard.KEY_RETURN && bootOrderChanged) {
                            saveBootOrderConfig();
                            bootOrderChanged = false;
                        }
                    } else if (biosTabIndex == 3 && Keyboard.getEventKey() == Keyboard.KEY_RETURN && updateAvailable && !updateInProgress) {

                        updateInProgress = true;
                        updateMessage = "Téléchargement en cours...";
                        new Thread(() -> {
                            try {
                                Updater.downloadLastVersion(
                                    "https://raw.githubusercontent.com/OpenDeskOS-Team/OpenDesk-Updater/refs/heads/main/update_index.json",
                                    "ppcore",
                                    new java.io.File(BIOS.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath()
                                );
                                updateMessage = "Mise à jour terminée. Redémarrage...";
                                relaunchWithUpdatedJar();
                            } catch (Exception e) {
                                updateMessage = "Erreur MAJ: " + e.getMessage();
                                updateInProgress = false;
                            }
                        }).start();
                    } else if (biosTabIndex == 4) {

                        if (Keyboard.getEventKey() == Keyboard.KEY_UP) {
                            exitSelection = (exitSelection + 1) % 2;
                        } else if (Keyboard.getEventKey() == Keyboard.KEY_DOWN) {
                            exitSelection = (exitSelection + 1) % 2;
                        } else if (Keyboard.getEventKey() == Keyboard.KEY_RETURN) {
                            if (exitSelection == 0) {
                                saveBootOrderConfig();
                                restart();
                            } else {
                                restart();
                            }
                        }
                    }
                }
                if (Keyboard.getEventKey() == Keyboard.KEY_F11) {
                    toggleFullscreen();
                }
            }
        }
    }

    public void restart() {
        biosMode = false;
        biosTabIndex = 0;
        updateAvailable = false;
        remoteVersion = null;
        updateInProgress = false;
        updateMessage = null;
        detectedDisks.clear();
        bootOrderSelection = 0;
        bootOrderChanged = false;
        showBootText = true;
        splashStartTime = System.currentTimeMillis();
        noBootableSystem = false;
        forceRedraw = true;
        loadBootOrderConfig();
        scanDisks();
        // DO NOT CALL init() neither Display.create()
        splashStartTime = System.currentTimeMillis();
        showBootText = true;
        loop();
    }

    public void shutdown() {
        Display.setTitle("PopulaireCore BIOS - Shutdown");
        while (!Display.isCloseRequested()) {
            GL11.glClearColor(0f, 0f, 0f, 1f);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            Display.update();
            while (org.lwjgl.input.Keyboard.next()) {
                if (org.lwjgl.input.Keyboard.getEventKeyState() && org.lwjgl.input.Keyboard.getEventKey() == org.lwjgl.input.Keyboard.KEY_F1) {
                    restart();
                    return;
                }
            }
        }
        // DO NOT CALL Display.destroy() here
    }

    public static String extractRemoteVersion(String json, String projectName) {
        String projetsKey = "\"projets\"";
        int projetsIdx = json.indexOf(projetsKey);
        if (projetsIdx < 0) return null;
        int projectIdx = json.indexOf('"' + projectName + '"', projetsIdx);
        if (projectIdx < 0) return null;
        int projectStart = json.indexOf('{', projectIdx);
        int projectEnd = json.indexOf('}', projectStart);
        if (projectStart < 0 || projectEnd < 0) return null;
        String projectBlock = json.substring(projectStart, projectEnd);
        String cvKey = "\"current_version\"";
        int cvIdx = projectBlock.indexOf(cvKey);
        if (cvIdx < 0) return null;
        int cvColon = projectBlock.indexOf(':', cvIdx);
        int cvQuote1 = projectBlock.indexOf('"', cvColon);
        int cvQuote2 = projectBlock.indexOf('"', cvQuote1 + 1);
        if (cvColon < 0 || cvQuote1 < 0 || cvQuote2 < 0) return null;
        return projectBlock.substring(cvQuote1 + 1, cvQuote2);
    }

    private void relaunchWithUpdatedJar() {
        try {
            String javaBin = System.getProperty("java.home") + java.io.File.separator + "bin" + java.io.File.separator + "java";
            String jarPath = new java.io.File(BIOS.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
            new ProcessBuilder(javaBin, "-jar", jarPath).start();
            System.exit(0);
        } catch (Exception e) {
            updateMessage = "Erreur lors du redémarrage: " + e.getMessage();
        }
    }

    private void scanDisks() {
        detectedDisks.clear();
        File disksDir = new File(System.getProperty("user.dir"), "disks");
        File cdDir = new File(System.getProperty("user.dir"), "cd");
        disksDir.mkdirs();
        cdDir.mkdirs();
        File[] diskJars = disksDir.listFiles((d, name) -> name.toLowerCase().endsWith(".jar"));
        File[] cdJars = cdDir.listFiles((d, name) -> name.toLowerCase().endsWith(".jar"));
        if (diskJars != null) Collections.addAll(detectedDisks, diskJars);
        if (cdJars != null) Collections.addAll(detectedDisks, cdJars);
        if (!bootOrder.isEmpty()) {
            detectedDisks.sort(Comparator.comparingInt(f -> {
                String diskPath = f.getAbsolutePath().replace("/", "\\").toLowerCase();
                int idx = -1;
                for (int i = 0; i < bootOrder.size(); i++) {
                    String orderPath = bootOrder.get(i).replace("/", "\\").toLowerCase();
                    if (diskPath.equals(orderPath)) {
                        idx = i;
                        break;
                    }
                }
                return idx >= 0 ? idx : Integer.MAX_VALUE;
            }));
        }
    }

    private void loadBootOrderConfig() {
        bootOrder.clear();
        File config = new File(System.getProperty("user.dir"), CONFIG_FILE);
        if (!config.exists()) return;
        try (FileReader fr = new FileReader(config)) {
            StringBuilder sb = new StringBuilder();
            int c;
            while ((c = fr.read()) != -1) sb.append((char)c);
            String json = sb.toString();
            int keyIdx = json.indexOf("\"bootOrder\"");
            if (keyIdx >= 0) {
                int arrStart = json.indexOf('[', keyIdx);
                int arrEnd = json.indexOf(']', arrStart);
                if (arrStart >= 0 && arrEnd > arrStart) {
                    String arr = json.substring(arrStart + 1, arrEnd);
                    for (String s : arr.split(",")) {
                        String path = s.trim().replaceAll("^\"|\"$", "");
                        if (!path.isEmpty()) bootOrder.add(path);
                    }
                }
            }
        } catch (IOException ignored) {}
    }

    private void saveBootOrderConfig() {
        bootOrder = detectedDisks.stream().map(File::getAbsolutePath).collect(Collectors.toList());
        File config = new File(System.getProperty("user.dir"), CONFIG_FILE);
        try (FileWriter fw = new FileWriter(config)) {
            fw.write("{\n  \"bootOrder\": [\n");
            for (int i = 0; i < bootOrder.size(); i++) {
                fw.write("    \"" + bootOrder.get(i).replace("\\", "\\\\") + "\"");
                if (i < bootOrder.size() - 1) fw.write(",\n");
            }
            fw.write("\n  ]\n}\n");
        } catch (IOException ignored) {}
    }
}
