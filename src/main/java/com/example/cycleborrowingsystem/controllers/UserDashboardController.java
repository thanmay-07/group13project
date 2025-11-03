package com.example.cycleborrowingsystem.controllers;

import com.example.cycleborrowingsystem.SceneManager;
import com.example.cycleborrowingsystem.dao.CycleDao;
import com.example.cycleborrowingsystem.dao.UserLocationTokenDao;
import com.example.cycleborrowingsystem.models.Cycle;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Duration;

import com.example.cycleborrowingsystem.net.LandingServer;

import java.sql.SQLException;
import java.util.List;

public class UserDashboardController {
    @FXML private TableView<Cycle> cyclesTable;
    @FXML private TableColumn<Cycle, Long> idColumn;
    @FXML private TableColumn<Cycle, String> modelColumn;
    @FXML private TableColumn<Cycle, Double> latColumn;
    @FXML private TableColumn<Cycle, Double> lonColumn;
    @FXML private TextField modelField;
    @FXML private TextField latField;
    @FXML private TextField lonField;
    @FXML private Label locationLabel;

    private final CycleDao cycleDao = new CycleDao();
    private final UserLocationTokenDao tokenDao = new UserLocationTokenDao();
    private Timeline locationTimeline;

    @FXML
    private void initialize() {
        if (idColumn != null) idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        if (modelColumn != null) modelColumn.setCellValueFactory(new PropertyValueFactory<>("model"));
        if (latColumn != null) latColumn.setCellValueFactory(new PropertyValueFactory<>("lat"));
        if (lonColumn != null) lonColumn.setCellValueFactory(new PropertyValueFactory<>("lon"));
        onRefresh();
        startLocationUpdates();
    }

    @FXML
    private void onAddOrUpdate() {
        String model = modelField.getText() == null ? "" : modelField.getText().trim();
        String latS = latField.getText() == null ? "" : latField.getText().trim();
        String lonS = lonField.getText() == null ? "" : lonField.getText().trim();
        if (model.isEmpty()) {
            new Alert(Alert.AlertType.ERROR, "Model is required").show();
            return;
        }
        try {
            double lat = Double.parseDouble(latS);
            double lon = Double.parseDouble(lonS);
            cycleDao.addOrUpdate(model, lat, lon);
            onRefresh();
        } catch (NumberFormatException ex) {
            new Alert(Alert.AlertType.ERROR, "Latitude/Longitude must be numbers").show();
        } catch (SQLException ex) {
            new Alert(Alert.AlertType.ERROR, "DB error: " + ex.getMessage()).show();
        }
    }

    @FXML
    private void onRemoveSelected() {
        Cycle selected = cyclesTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        try {
            cycleDao.deleteById(selected.getId());
            onRefresh();
        } catch (SQLException ex) {
            new Alert(Alert.AlertType.ERROR, "DB error: " + ex.getMessage()).show();
        }
    }

    @FXML
    private void onRefresh() {
        try {
            List<Cycle> list = cycleDao.listAll();
            ObservableList<Cycle> data = FXCollections.observableArrayList(list);
            cyclesTable.setItems(data);
        } catch (SQLException ex) {
            new Alert(Alert.AlertType.ERROR, "Failed to load cycles: " + ex.getMessage()).show();
        }
    }

    @FXML
    private void onBack() {
        stopLocationUpdates();
        try {
            SceneManager.switchScene("/com/example/cycleborrowingsystem/landing.fxml");
        } catch (Exception ignored) {}
    }

    private void startLocationUpdates() {
        LandingServer server = LandingServer.getInstance();
        if (server == null) return;
        final String host = server.getLocalAddress();
        final int port = 8765;
        final String token = server.getPairToken();
        if (token == null || token.isBlank()) return;

        locationTimeline = new Timeline(new KeyFrame(Duration.seconds(5), e -> {
            String latestUrl = String.format("http://%s:%d/latest?token=%s", host, port, token);
            try {
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(latestUrl).openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);
                int code = conn.getResponseCode();
                if (code != 200) return;
                try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream()))) {
                    String resp = br.lines().reduce("", (a,b) -> a + b);
                    String low = resp.replaceAll("\\s+", "");
                    int iLat = low.indexOf("\"lat\":");
                    int iLon = low.indexOf("\"lon\":");
                    if (iLat >= 0 && iLon >= 0) {
                        double lat = parseNumber(low, iLat + 6);
                        double lon = parseNumber(low, iLon + 6);
                        locationLabel.setText(String.format(java.util.Locale.US, "%.6f, %.6f", lat, lon));
                        try {
                            // Optional: persist live location to DB if user known
                            com.example.cycleborrowingsystem.models.User u = com.example.cycleborrowingsystem.Session.getCurrentUser();
                            if (u != null) tokenDao.setLocation(u.getId(), lat, lon);
                        } catch (SQLException ignore) {}
                    }
                }
            } catch (Exception ignore) {}
        }));
        locationTimeline.setCycleCount(Timeline.INDEFINITE);
        locationTimeline.play();
    }

    private void stopLocationUpdates() {
        if (locationTimeline != null) {
            locationTimeline.stop();
            locationTimeline = null;
        }
    }

    private static double parseNumber(String s, int startIdx) {
        int end = startIdx;
        while (end < s.length() && (Character.isDigit(s.charAt(end)) || s.charAt(end) == '.' || s.charAt(end) == '-' || s.charAt(end) == 'E' || s.charAt(end) == 'e' || s.charAt(end) == '+')) end++;
        return Double.parseDouble(s.substring(startIdx, end));
    }
}


