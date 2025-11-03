package com.example.cycleborrowingsystem.controllers;

import com.example.cycleborrowingsystem.net.LandingServer;
import com.example.cycleborrowingsystem.dao.UserDao;
import com.example.cycleborrowingsystem.SceneManager;
import com.example.cycleborrowingsystem.Session;
import com.example.cycleborrowingsystem.models.User;
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
        char[] pass = passwordField.getText() == null ? new char[0] : passwordField.getText().toCharArray();
        try {
            UserDao userDao = new UserDao();
            if (!userDao.verifyCredentials(email, pass)) {
                new Alert(Alert.AlertType.ERROR, "Invalid credentials").show();
                return;
            }
            User u = userDao.findByEmail(email);
            if (u == null) {
                new Alert(Alert.AlertType.ERROR, "User not found after login").show();
                return;
            }
            Session.setCurrentUser(u);
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "Login error: " + ex.getMessage()).show();
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

                    Platform.runLater(() -> {
                        try {
                            SceneManager.switchScene("/com/example/cycleborrowingsystem/user_dashboard.fxml");
                        } catch (Exception e) {
                            new Alert(Alert.AlertType.ERROR, "Failed to open dashboard: " + e.getMessage()).show();
                        }
                    });
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

    
}
