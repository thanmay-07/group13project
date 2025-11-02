package com.example.cycleborrowingsystem.controllers;

import com.example.cycleborrowingsystem.CycleStore;
import com.example.cycleborrowingsystem.net.LandingServer;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.application.Platform;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class UserLoginController {
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;

    @FXML
    private void onLogin() {
        String email = emailField.getText();
        if (email == null || email.isBlank()) {
            new Alert(Alert.AlertType.ERROR, "Enter email").show();
            return;
        }

        LandingServer server = LandingServer.getInstance();
        if (server == null) {
            new Alert(Alert.AlertType.ERROR, "Location server not running").show();
            return;
        }

        String host = server.getLocalAddress();
        String token = server.getPairToken();
        int port = 8765;
        String latestUrl = String.format("http://%s:%d/latest?token=%s", host, port, token);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                try {
                    URL url = new URL(latestUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    int code = conn.getResponseCode();
                    if (code != 200) {
                        Platform.runLater(() -> new Alert(Alert.AlertType.WARNING, "No location shared yet. Please open pairing link on mobile and share location.").show());
                        return null;
                    }
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    br.close();
                    String resp = sb.toString();
                    double lat = Double.NaN;
                    double lon = Double.NaN;
                    String low = resp.replaceAll("\\s+", "");
                    int iLat = low.indexOf("\"lat\":");
                    int iLon = low.indexOf("\"lon\":");
                    if (iLat >= 0) {
                        int start = iLat + 6;
                        int end = start;
                        while (end < low.length() && (Character.isDigit(low.charAt(end)) || low.charAt(end) == '.' || low.charAt(end) == '-' || low.charAt(end) == 'E' || low.charAt(end) == 'e' || low.charAt(end) == '+')) end++;
                        String latS = low.substring(start, end);
                        lat = Double.parseDouble(latS);
                    }
                    if (iLon >= 0) {
                        int start = iLon + 6;
                        int end = start;
                        while (end < low.length() && (Character.isDigit(low.charAt(end)) || low.charAt(end) == '.' || low.charAt(end) == '-' || low.charAt(end) == 'E' || low.charAt(end) == 'e' || low.charAt(end) == '+')) end++;
                        String lonS = low.substring(start, end);
                        lon = Double.parseDouble(lonS);
                    }
                    if (Double.isNaN(lat) || Double.isNaN(lon)) {
                        Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, "Failed to parse latest location from server").show());
                        return null;
                    }

                    List<CycleStore.Cycle> cycles = CycleStore.load();
                    List<CycleDistance> nearby = new ArrayList<>();
                    for (CycleStore.Cycle c : cycles) {
                        double dkm = haversine(lat, lon, c.lat, c.lon);
                        if (dkm <= 3.0) nearby.add(new CycleDistance(c, dkm));
                    }
                    if (nearby.isEmpty()) {
                        String msg = String.format(Locale.US, "Logged in\nYour last shared location: %.6f, %.6f\nNo cycles found within 3 km.", lat, lon);
                        Platform.runLater(() -> new Alert(Alert.AlertType.INFORMATION, msg).show());
                        return null;
                    }
                    nearby.sort((a,b) -> Double.compare(a.distKm, b.distKm));
                    StringBuilder out = new StringBuilder();
                    out.append(String.format(Locale.US, "Logged in\nYour last shared location: %.6f, %.6f\n\nNearby cycles:\n", lat, lon));
                    for (CycleDistance cd : nearby) {
                        out.append(String.format(Locale.US, "%s — %.2f km away\n", cd.cycle.model, cd.distKm));
                    }
                    Platform.runLater(() -> new Alert(Alert.AlertType.INFORMATION, out.toString()).show());
                } catch (Exception ex) {
                    Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, "Error contacting local server: " + ex.getMessage()).show());
                }
                return null;
            }
        };
        new Thread(task).start();
    }

    @FXML
    private void onBack() {
        try {
            com.example.cycleborrowingsystem.SceneManager.switchScene("/com/example/cycleborrowingsystem/landing.fxml");
        } catch (Exception ignored) {}
    }

    private static class CycleDistance {
        final CycleStore.Cycle cycle;
        final double distKm;
        CycleDistance(CycleStore.Cycle cycle, double distKm) { this.cycle = cycle; this.distKm = distKm; }
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }
}
