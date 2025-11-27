package be.ninedocteur.ppcore;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class Main {
    public static void main(String[] args) {
        NativeExtractor.extractAndLoadNatives();
        TextRenderer.init();
        boolean biosRequested = false;
        boolean showBootText = true;
        long start = System.currentTimeMillis();
        System.out.println("Appuyez sur F2 ou DELETE pour entrer dans le BIOS, F12 pour le boot menu...");
        while (System.currentTimeMillis() - start < 5000) {
            try { Thread.sleep(50); } catch (InterruptedException ignored) {}
            if (org.lwjgl.input.Keyboard.isCreated()) {
                org.lwjgl.input.Keyboard.poll();
                if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_F2) ||
                    org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_DELETE)) {
                    biosRequested = true;
                    break;
                }
                if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_F12)) {
                    new BIOS().run();
                    return;
                }
            } else {
                try { org.lwjgl.input.Keyboard.create(); } catch (Exception ignored) {}
            }
        }
        showBootText = false;
        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
        if (biosRequested) {
            new BIOS().run();
            return;
        }
        if (bootExternalOS(args)) {
            return;
        }
        new BIOS().run();
    }

    /**
     * Tente de booter un OS externe depuis /cd ou /disks (priorité CD),
     * charge dynamiquement le JAR, cherche une classe IOS et exécute startOS.
     * @return true si un OS a été booté, false sinon (continue BIOS)
     */
    public static boolean bootExternalOS(String[] args) {
        try {
            File root = new File(System.getProperty("user.dir"));
            File cdDir = new File(root, "cd");
            File disksDir = new File(root, "disks");
            cdDir.mkdirs();
            disksDir.mkdirs();
            System.out.println("Recherche de JAR bootables dans: " + cdDir.getAbsolutePath());
            File jarToBoot = findBootableJar(cdDir);
            if (jarToBoot == null) {
                System.out.println("Aucun JAR trouvé dans /cd, recherche dans /disks: " + disksDir.getAbsolutePath());
                jarToBoot = findBootableJar(disksDir);
            }
            if (jarToBoot == null) {
                System.out.println("Aucun OS bootable trouvé dans /cd ou /disks");
                return false;
            }
            System.out.println("Boot sur: " + jarToBoot.getAbsolutePath());
            try (JarFile jarFile = new JarFile(jarToBoot)) {
                URL[] urls = { jarToBoot.toURI().toURL() };
                try (URLClassLoader cl = new URLClassLoader(urls, Main.class.getClassLoader())) {
                    for (JarEntry entry : java.util.Collections.list(jarFile.entries())) {
                        String name = entry.getName();
                        if (name.endsWith(".class")) {
                            String className = name.replace('/', '.').substring(0, name.length() - 6);
                            System.out.println("Analyse de la classe: " + className);
                            try {
                                Class<?> clazz = cl.loadClass(className);
                                for (Class<?> iface : clazz.getInterfaces()) {
                                    System.out.println("  Interface trouvée: " + iface.getName());
                                    if (iface.getName().equals("be.ninedocteur.ppcore.IOS")) {
                                        System.out.println("  Implémentation IOS détectée: " + className);
                                        Object osInstance = clazz.getDeclaredConstructor().newInstance();
                                        Method startOS = clazz.getMethod("startOS", String[].class);
                                        System.out.println("  Méthode startOS trouvée, lancement...");
                                        startOS.invoke(osInstance, (Object) args);
                                        Method displayLoop = clazz.getMethod("displayLoop");
                                        System.out.println("  Méthode displayLoop trouvée, démarrage de la boucle d'affichage...");
                                        org.lwjgl.opengl.Display.makeCurrent();
                                        while (!org.lwjgl.opengl.Display.isCloseRequested()) {
                                            displayLoop.invoke(osInstance);
                                            org.lwjgl.opengl.Display.update();
                                        }
                                        System.out.println("  Boucle d'affichage terminée.");
                                        return true;
                                    }
                                }
                            } catch (Throwable t) {
                                System.out.println("  Erreur lors de l'analyse de la classe: " + className + " : " + t);
                            }
                        }
                    }
                }
            }
            System.out.println("Aucune classe IOS trouvée dans le JAR: " + jarToBoot.getName());
        } catch (IOException e) {
            System.out.println("Erreur boot externe: " + e.getMessage());
        }
        return false;
    }

    private static File findBootableJar(File dir) {
        File[] jars = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".jar"));
        if (jars != null && jars.length > 0) {
            return jars[0];
        }
        return null;
    }
}
