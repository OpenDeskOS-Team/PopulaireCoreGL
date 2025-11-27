package be.ninedocteur.ppcore;

import be.ninedocteur.ppcore.screens.PostScreen;
import be.ninedocteur.ppcore.screens.BIOSScreen;
import be.ninedocteur.ppcore.screens.BootMenuScreen;
import be.ninedocteur.ppcore.screens.NoBootableSystemScreen;
import be.ninedocteur.ppcore.utils.Screen;import be.ninedocteur.ppcore.utils.TextRenderer;import be.ninedocteur.ppcore.utils.Updater;import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.PixelFormat;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import be.ninedocteur.ppcore.utils.UpdateManager;

public class BIOS {
    private boolean fullscreen = true;
    private final int windowedWidth = 1280;
    private final int windowedHeight = 720;
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
    private boolean forceRedraw = false;
    private int exitSelection = 0; // 0: save & exit, 1: exit without saving
    private int bootMenuSelection = 0;

    private boolean intelXeBugDetected = false;
    private static final String INTEL_XE_WARNING = "Warning! We detected that you have a Intel Xe Graphics Card, please note that a bug has been detected on version 32.0.101.7077 of the GPU Driver which may cause problems with OpenGL";


    private BootOrderManager bootOrderManager = new BootOrderManager();
    private DiskScanner diskScanner = new DiskScanner();
    private Screen currentScreen;

    public void run() {
        bootOrderManager.loadBootOrderConfig();
        detectedDisks = diskScanner.scanDisks();
        bootOrder = bootOrderManager.getBootOrder();
        bootOrderManager.sortDetectedDisks(detectedDisks);
        init();
        detectIntelXeBug();
        splashStartTime = System.currentTimeMillis();
        showBootText = true;
        currentScreen = new PostScreen(this, intelXeBugDetected, INTEL_XE_WARNING, showBootText);
        loop();
        Display.destroy();
    }

    private void detectIntelXeBug() {
        String gpuName = GL11.glGetString(GL11.GL_RENDERER);
        String glVersion = GL11.glGetString(GL11.GL_VERSION);
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

    public void toggleFullscreen() {
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
            if (currentScreen != null) {
                currentScreen.render();
            }
            Display.update();
            if (currentScreen != null) {
                currentScreen.handleInput();
            }
            // Transition vers BIOSScreen, BootMenuScreen, etc. à ajouter plus tard
        }
    }

    public void renderSplashScreen() {
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
            List<String> lines = TextRenderer.wrapTextToWidth(INTEL_XE_WARNING, warnFont, rectW - 20);
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

    public void showPostScreen() {
        showBootText = true;
        splashStartTime = System.currentTimeMillis();
        currentScreen = new PostScreen(this, intelXeBugDetected, INTEL_XE_WARNING, showBootText);
    }
    public void showBiosScreen() {
        currentScreen = new BIOSScreen(this);
    }
    public void showBootMenuScreen() {
        currentScreen = new BootMenuScreen(this);
    }
    public void showNoBootableSystemScreen() {
        currentScreen = new NoBootableSystemScreen(this);
    }

    private void handleInput() {
        // La gestion de l'input est maintenant entièrement déléguée à l'écran courant
        if (currentScreen != null) {
            currentScreen.handleInput();
        }
    }

    public void restart() {
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
        forceRedraw = true;
        bootOrderManager.loadBootOrderConfig();
        detectedDisks = diskScanner.scanDisks();
        bootOrder = bootOrderManager.getBootOrder();
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
            while (Keyboard.next()) {
                if (Keyboard.getEventKeyState() && Keyboard.getEventKey() == Keyboard.KEY_F1) {
                    restart();
                    return;
                }
            }
        }
        // DO NOT CALL Display.destroy() here
    }


    public int getBiosTabIndex() { return biosTabIndex; }
    public void setBiosTabIndex(int value) { this.biosTabIndex = value; }
    public static String[] getBiosTabs() { return BIOS_TABS; }
    public int getBootOrderSelection() { return bootOrderSelection; }
    public void setBootOrderSelection(int value) { this.bootOrderSelection = value; }
    public boolean isBootOrderChanged() { return bootOrderChanged; }
    public void setBootOrderChanged(boolean value) { this.bootOrderChanged = value; }
    public boolean isUpdateAvailable() { return updateAvailable; }
    public void setUpdateAvailable(boolean value) { this.updateAvailable = value; }
    public boolean isUpdateInProgress() { return updateInProgress; }
    public void setUpdateInProgress(boolean value) { this.updateInProgress = value; }
    public String getUpdateMessage() { return updateMessage; }
    public void setUpdateMessage(String value) { this.updateMessage = value; }
    public int getExitSelection() { return exitSelection; }
    public void setExitSelection(int value) { this.exitSelection = value; }
    public void saveBootOrderConfig() { bootOrderManager.saveBootOrderConfig(detectedDisks); bootOrder = bootOrderManager.getBootOrder(); }
    public void relaunchWithUpdatedJar() { try { String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java"; String jarPath = new File(BIOS.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath(); new ProcessBuilder(javaBin, "-jar", jarPath).start(); System.exit(0); } catch (Exception e) { updateMessage = "Erreur lors du redémarrage: " + e.getMessage(); } }

    // Getters pour accès depuis d'autres classes
    public boolean isIntelXeBugDetected() {
        return intelXeBugDetected;
    }
    public static String getIntelXeWarning() {
        return INTEL_XE_WARNING;
    }
    public int getBootMenuSelection() {
        return bootMenuSelection;
    }
    public void setBootMenuSelection(int selection) {
        this.bootMenuSelection = selection;
    }
    public List<File> getDetectedDisks() {
        return detectedDisks;
    }
    public void setDetectedDisks(List<File> disks) {
        this.detectedDisks = disks;
    }
}
