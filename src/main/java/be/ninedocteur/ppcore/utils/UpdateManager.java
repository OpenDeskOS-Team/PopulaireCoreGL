package be.ninedocteur.ppcore.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class UpdateManager {
    public static String extractRemoteVersion(String json, String projectName) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        if (!root.has("projets")) return null;
        JsonObject projets = root.getAsJsonObject("projets");
        if (!projets.has(projectName)) return null;
        JsonObject project = projets.getAsJsonObject(projectName);
        if (!project.has("current_version")) return null;
        return project.get("current_version").getAsString();
    }

    public static Update getUpdateInfo(String json) {
        return new Gson().fromJson(json, Update.class);
    }
}

