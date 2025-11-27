package be.ninedocteur.ppcore;

import com.google.gson.Gson;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class Updater {
    /**
     * Télécharge la dernière version du JAR pour le projet donné depuis un index JSON et remplace le fichier cible.
     * @param jsonUrl URL de l'index JSON
     * @param projectName Nom du projet (ex: "ppcore")
     * @param jarTargetPath Chemin du fichier JAR à remplacer
     * @throws IOException en cas d'erreur réseau ou fichier
     */
    public static void downloadLastVersion(String jsonUrl, String projectName, String jarTargetPath) throws IOException {
        String json = readUrlToString(jsonUrl);
        String jarUrl = extractJarUrlWithGson(json, projectName);
        if (jarUrl == null) throw new IOException("Aucune version trouvée pour le projet: " + projectName);
        File tempFile = File.createTempFile("ppcore_update", ".jar");
        downloadFile(jarUrl, tempFile);
        File target = new File(jarTargetPath);
        try {
            Files.move(tempFile.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            // Si le JAR est en cours d'utilisation, on tente la mise à jour différée via un script batch
            prepareAndLaunchUpdateScript(tempFile, target);
            return;
        }
    }

    private static void prepareAndLaunchUpdateScript(File tempFile, File targetJar) throws IOException {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            // Script batch Windows
            File script = File.createTempFile("update_launcher", ".bat");
            String scriptContent = "@echo off\r\n"
                + "setlocal\r\n"
                + "set JAR_PATH=\"" + targetJar.getAbsolutePath() + "\"\r\n"
                + "set NEW_JAR=\"" + tempFile.getAbsolutePath() + "\"\r\n"
                + ":loop\r\n"
                + "del %JAR_PATH% >nul 2>&1\r\n"
                + "if exist %JAR_PATH% (\r\n"
                + "    timeout /t 2 >nul\r\n"
                + "    goto loop\r\n"
                + ")\r\n"
                + "move /Y %NEW_JAR% %JAR_PATH%\r\n"
                + "start \"\" java -jar %JAR_PATH%\r\n"
                + "endlocal\r\n";
            try (FileWriter fw = new FileWriter(script)) {
                fw.write(scriptContent);
            }
            new ProcessBuilder("cmd", "/c", script.getAbsolutePath()).start();
        } else {
            // Script shell Linux/macOS
            File script = File.createTempFile("update_launcher", ".sh");
            String scriptContent = "#!/bin/sh\n"
                + "JAR_PATH=\"" + targetJar.getAbsolutePath() + "\"\n"
                + "NEW_JAR=\"" + tempFile.getAbsolutePath() + "\"\n"
                + "while [ -f $JAR_PATH ] && lsof $JAR_PATH >/dev/null 2>&1; do\n"
                + "  sleep 2\n"
                + "done\n"
                + "mv -f $NEW_JAR $JAR_PATH\n"
                + "nohup java -jar $JAR_PATH &\n";
            try (FileWriter fw = new FileWriter(script)) {
                fw.write(scriptContent);
            }
            script.setExecutable(true);
            new ProcessBuilder("sh", script.getAbsolutePath()).start();
        }
        System.exit(0);
    }

    private static String extractJarUrlWithGson(String json, String projectName) {
        Update update = new Gson().fromJson(json, Update.class);
        if (update == null || update.projets == null) return null;
        Update.UpdateProject project = update.projets.get(projectName);
        if (project == null || project.current_version == null || project.version_history == null) return null;
        Update.UpdateVersion version = project.version_history.get(project.current_version);
        if (version == null || version.file_url == null) return null;
        return version.file_url;
    }

    static String readUrlToString(String urlStr) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestProperty("User-Agent", "Updater/1.0");
        try (InputStream in = conn.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return out.toString("UTF-8");
        }
    }

    private static void downloadFile(String urlStr, File dest) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestProperty("User-Agent", "Updater/1.0");
        try (InputStream in = conn.getInputStream(); FileOutputStream out = new FileOutputStream(dest)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        }
    }

    public static String extractRemoteVersion(String json, String projectName) {
        Update update = new Gson().fromJson(json, Update.class);
        if (update == null || update.projets == null) return null;
        Update.UpdateProject project = update.projets.get(projectName);
        if (project == null || project.current_version == null) return null;
        return project.current_version;
    }
}
