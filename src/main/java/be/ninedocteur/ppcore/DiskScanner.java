package be.ninedocteur.ppcore;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DiskScanner {
    public List<File> scanDisks() {
        List<File> detectedDisks = new ArrayList<>();
        File disksDir = new File(System.getProperty("user.dir"), "disks");
        File cdDir = new File(System.getProperty("user.dir"), "cd");
        disksDir.mkdirs();
        cdDir.mkdirs();
        File[] diskJars = disksDir.listFiles((d, name) -> name.toLowerCase().endsWith(".jar"));
        File[] cdJars = cdDir.listFiles((d, name) -> name.toLowerCase().endsWith(".jar"));
        if (diskJars != null) Collections.addAll(detectedDisks, diskJars);
        if (cdJars != null) Collections.addAll(detectedDisks, cdJars);
        return detectedDisks;
    }
}
