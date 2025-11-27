package be.ninedocteur.ppcore;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class BootOrderManager {
    private static final String CONFIG_FILE = "config.json";
    private List<String> bootOrder = new ArrayList<>();

    public List<String> getBootOrder() {
        return bootOrder;
    }

    public void loadBootOrderConfig() {
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

    public void saveBootOrderConfig(List<java.io.File> detectedDisks) {
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

    public void sortDetectedDisks(List<File> detectedDisks) {
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
}
