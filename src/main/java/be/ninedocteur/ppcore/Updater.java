package be.ninedocteur.ppcore;

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
        String jarUrl = extractJarUrl(json, projectName);
        if (jarUrl == null) throw new IOException("Aucune version trouvée pour le projet: " + projectName);
        File tempFile = File.createTempFile("ppcore_update", ".jar");
        downloadFile(jarUrl, tempFile);
        File target = new File(jarTargetPath);
        Files.move(tempFile.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
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

    private static String extractJarUrl(String json, String projectName) {
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
        String currentVersion = projectBlock.substring(cvQuote1 + 1, cvQuote2);
        String vhKey = "\"version_history\"";
        int vhIdx = projectBlock.indexOf(vhKey);
        if (vhIdx < 0) return null;
        int vhStart = projectBlock.indexOf('{', vhIdx);
        int vhEnd = projectBlock.indexOf('}', vhStart);
        if (vhStart < 0 || vhEnd < 0) return null;
        String vhBlock = projectBlock.substring(vhStart, vhEnd);
        int verIdx = vhBlock.indexOf('"' + currentVersion + '"');
        if (verIdx < 0) return null;
        int verStart = vhBlock.indexOf('{', verIdx);
        int verEnd = vhBlock.indexOf('}', verStart);
        if (verStart < 0 || verEnd < 0) return null;
        String verBlock = vhBlock.substring(verStart, verEnd);
        String fuKey = "\"file_url\"";
        int fuIdx = verBlock.indexOf(fuKey);
        if (fuIdx < 0) return null;
        int fuColon = verBlock.indexOf(':', fuIdx);
        int fuQuote1 = verBlock.indexOf('"', fuColon);
        int fuQuote2 = verBlock.indexOf('"', fuQuote1 + 1);
        if (fuColon < 0 || fuQuote1 < 0 || fuQuote2 < 0) return null;
        return verBlock.substring(fuQuote1 + 1, fuQuote2);
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
}
