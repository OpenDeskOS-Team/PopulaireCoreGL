package be.ninedocteur.ppcore;

import java.io.*;
import java.util.Locale;

public class NativeExtractor {
    private static boolean loaded = false;

    public static void extractAndLoadNatives() {
        if (loaded) return;
        loaded = true;
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        String arch = System.getProperty("os.arch").toLowerCase(Locale.ROOT);
        String nativesFolder = null;
        if (os.contains("win")) {
            nativesFolder = arch.contains("64") ? "natives/windows64" : "natives/windows32";
        } else if (os.contains("mac")) {
            nativesFolder = "natives/macos";
        } else if (os.contains("nux") || os.contains("nix")) {
            nativesFolder = arch.contains("64") ? "natives/linux64" : "natives/linux32";
        } else {
            throw new RuntimeException("OS non supporté pour l'extraction des natives LWJGL");
        }
        String[] nativeLibs = getNativeLibs(os, arch);
        File libsDir = new File("libs");
        if (!libsDir.exists()) {
            libsDir.mkdirs();
        }
        try {
            for (String lib : nativeLibs) {
                File outFile = new File(libsDir, lib);
                if (!outFile.exists()) {
                    extractResourceToFile(nativesFolder + "/" + lib, outFile);
                }
            }
            System.setProperty("org.lwjgl.librarypath", libsDir.getAbsolutePath());
            System.setProperty("java.library.path", libsDir.getAbsolutePath());
            for (String lib : nativeLibs) {
                try {
                    System.load(new File(libsDir, lib).getAbsolutePath());
                } catch (UnsatisfiedLinkError ignored) {}
            }
        } catch (IOException e) {
            throw new RuntimeException("Erreur extraction natives LWJGL", e);
        }
    }

    private static void extractResourceToFile(String resourcePath, File outFile) throws IOException {
        try (InputStream in = NativeExtractor.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) throw new FileNotFoundException(resourcePath);
            try (OutputStream out = new FileOutputStream(outFile)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
            }
        }
    }

    private static String[] getNativeLibs(String os, String arch) {
        if (os.contains("win")) {
            return arch.contains("64") ?
                new String[]{"lwjgl64.dll", "OpenAL64.dll", "jinput-dx8_64.dll"} :
                new String[]{"lwjgl.dll", "OpenAL32.dll", "jinput-dx8.dll"};
        } else if (os.contains("mac")) {
            return new String[]{"liblwjgl.dylib", "libopenal.dylib"};
        } else if (os.contains("nux") || os.contains("nix")) {
            return arch.contains("64") ?
                new String[]{"liblwjgl.so", "libopenal.so"} :
                new String[]{"liblwjgl.so", "libopenal.so"};
        }
        throw new RuntimeException("OS non supporté pour natives");
    }
}
