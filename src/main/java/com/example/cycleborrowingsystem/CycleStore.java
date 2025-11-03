package com.example.cycleborrowingsystem;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Simple persistent store for cycles. Uses a small CSV-like file in the user's home directory.
 * Format per line: model|lat|lon
 */
public class CycleStore {
    public static final String STORE_FILENAME = ".cbs_cycles.csv";

    public static class Cycle {
        public final String model;
        public final double lat;
        public final double lon;

        public Cycle(String model, double lat, double lon) {
            this.model = model;
            this.lat = lat;
            this.lon = lon;
        }
    }

    private static Path storePath() {
        String home = System.getProperty("user.home");
        return Paths.get(home, STORE_FILENAME);
    }

    public static List<Cycle> load() {
        Path p = storePath();
        if (!Files.exists(p)) {
            return defaultSample();
        }
        try {
            List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
            List<Cycle> out = new ArrayList<>();
            for (String l : lines) {
                if (l == null) continue;
                String s = l.trim();
                if (s.isEmpty()) continue;
                String[] parts = s.split("\\|", 3);
                if (parts.length != 3) continue;
                try {
                    String model = parts[0];
                    double lat = Double.parseDouble(parts[1]);
                    double lon = Double.parseDouble(parts[2]);
                    out.add(new Cycle(model, lat, lon));
                } catch (NumberFormatException ignored) {}
            }
            if (out.isEmpty()) return defaultSample();
            return out;
        } catch (IOException e) {
            return defaultSample();
        }
    }

    public static void save(List<Cycle> cycles) throws IOException {
        if (cycles == null) return;
        Path p = storePath();
        List<String> lines = new ArrayList<>();
        for (Cycle c : cycles) {
            lines.add(String.format("%s|%.6f|%.6f", c.model.replaceAll("\\|", "_"), c.lat, c.lon));
        }
        Files.write(p, lines, StandardCharsets.UTF_8);
    }

    public static void addOrUpdate(Cycle c) throws IOException {
        List<Cycle> list = new ArrayList<>(load());
        boolean found = false;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).model.equalsIgnoreCase(c.model)) {
                list.set(i, c);
                found = true;
                break;
            }
        }
        if (!found) list.add(c);
        save(list);
    }

    public static List<Cycle> defaultSample() {
        List<Cycle> list = new ArrayList<>();
        list.add(new Cycle("Red-Servant", 17.387140, 78.491684));
        list.add(new Cycle("Blue-Rider", 17.395000, 78.492000));
        list.add(new Cycle("Green-Spin", 17.380000, 78.480000));
        return Collections.unmodifiableList(list);
    }
}
