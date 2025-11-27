package be.ninedocteur.ppcore;

import be.ninedocteur.ppcore.screens.PostScreen;
import be.ninedocteur.ppcore.screens.BIOSScreen;
import be.ninedocteur.ppcore.screens.BootMenuScreen;
import be.ninedocteur.ppcore.utils.Screen;import be.ninedocteur.ppcore.utils.TextRenderer;import be.ninedocteur.ppcore.utils.Updater;import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.PixelFormat;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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

    private boolean fastBootEnabled = false;
    private int allocatedRamMB = (int)(Runtime.getRuntime().maxMemory() / (1024 * 1024)); // Par défaut, max JVM
    public static final String BUILD_DATE = "2025-11-27"; // À ajuster dynamiquement si besoin
    private String systemUUID;

    public void run() {
        loadConfigFromJson();
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

    @SuppressWarnings("unchecked")
    public void saveConfigToJson() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonObject obj = new JsonObject();
        obj.addProperty("fastBootEnabled", fastBootEnabled);
        obj.addProperty("allocatedRamMB", allocatedRamMB);
        // Save bootOrder as JSON array
        com.google.gson.JsonArray bootOrderArray = new com.google.gson.JsonArray();
        for (String disk : bootOrder) {
            bootOrderArray.add(disk);
        }
        obj.add("bootOrder", bootOrderArray);
        try (FileWriter file = new FileWriter(CONFIG_FILE)) {
            gson.toJson(obj, file);
        } catch (IOException e) {
            System.err.println("Error while saving BIOS config: " + e.getMessage());
        }
    }

    public void loadConfigFromJson() {
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
            if (jsonObject.has("fastBootEnabled")) fastBootEnabled = jsonObject.get("fastBootEnabled").getAsBoolean();
            if (jsonObject.has("allocatedRamMB")) allocatedRamMB = jsonObject.get("allocatedRamMB").getAsInt();
            if (jsonObject.has("bootOrder")) {
                bootOrder.clear();
                com.google.gson.JsonArray bootOrderArray = jsonObject.getAsJsonArray("bootOrder");
                for (int i = 0; i < bootOrderArray.size(); i++) {
                    bootOrder.add(bootOrderArray.get(i).getAsString());
                }
            }
        } catch (IOException e) {
            // File missing or invalid: use defaults
        }
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
            // Générer le systemUUID maintenant que le contexte OpenGL existe
            this.systemUUID = generateSystemUUID();
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

    public void restart() {
        saveConfigToJson();
        loadConfigFromJson();
        showPostScreen();
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


    public BIOS() {
        // Ne pas générer le systemUUID ici, car le contexte OpenGL n'est pas encore créé
    }
    private String generateSystemUUID() {
        String cpu = getCpuModel();
        String gpu = getGpuModel();
        String base = cpu + ":" + gpu;
        return java.util.UUID.nameUUIDFromBytes(base.getBytes()).toString();
    }
    public String getSystemUUID() {
        return systemUUID != null ? systemUUID : "(UUID non initialisé)";
    }
    public String getCpuModel() {
        String cpu = System.getenv("PROCESSOR_IDENTIFIER");
        if (cpu != null && !cpu.isEmpty()) return cpu;
        try {
            java.lang.management.OperatingSystemMXBean osBean = java.lang.management.ManagementFactory.getOperatingSystemMXBean();
            java.lang.reflect.Method method = osBean.getClass().getMethod("getName");
            return method.invoke(osBean).toString();
        } catch (Exception e) {
            return System.getProperty("os.arch");
        }
    }
    public String getGpuModel() {
        String gpu = org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_RENDERER);
        return gpu != null ? gpu : "Unknown GPU";
    }
    public boolean isFastBootEnabled() {
        return fastBootEnabled;
    }
    public void setFastBootEnabled(boolean enabled) {
        this.fastBootEnabled = enabled;
    }
    public int getAllocatedRamMB() {
        return allocatedRamMB;
    }
    public void setAllocatedRamMB(int mb) {
        this.allocatedRamMB = mb;
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
