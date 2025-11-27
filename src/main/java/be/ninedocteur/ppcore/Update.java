package be.ninedocteur.ppcore;

import java.util.Map;

public class Update {
    public Map<String, UpdateProject> projets;

    public static class UpdateProject {
        public String repository;
        public String current_version;
        public Map<String, UpdateVersion> version_history;
    }

    public static class UpdateVersion {
        public String title;
        public String description;
        public String date;
        public String file_url;
    }
}

