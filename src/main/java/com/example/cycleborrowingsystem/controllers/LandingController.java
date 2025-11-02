package com.example.cycleborrowingsystem.controllers;

import com.example.cycleborrowingsystem.net.LandingServer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

import java.awt.Desktop;
import java.net.URI;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;
import com.example.cycleborrowingsystem.Session;
import com.example.cycleborrowingsystem.dao.UserLocationTokenDao;
import com.example.cycleborrowingsystem.models.User;
import com.example.cycleborrowingsystem.net.LandingServer.BiConsumerOnLocation;

public class LandingController implements Initializable {
    @FXML private Button adminPanelBtn;
    @FXML private Button userPanelBtn;
    @FXML private TextField pairingField;
    @FXML private Button addCycleBtn;
    @FXML private Button copyBtn;
    @FXML private Button openBtn;
    @FXML private Label locationStatusLabel;
    @FXML private TextField manualLocationField;
    @FXML private Button loginButton;
    @FXML private Button signupButton;

    private LandingServer landingServer;
    private final int serverPort = 8765;
    public static final String ADMIN_ID = "admin@cbs.local";
    public static final String ADMIN_PASSWORD = "Admin@1234";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        pairingField.setEditable(false);
        pairingField.setOnMouseClicked(e -> pairingField.selectAll());
        copyBtn.setOnAction(e -> copyPairing());
        openBtn.setOnAction(e -> openPairing());
        if (addCycleBtn != null) addCycleBtn.setOnAction(e -> addOrUpdateCycleFromManual());
        adminPanelBtn.setOnAction(e -> openAdminLogin());
        userPanelBtn.setOnAction(e -> openUserLogin());
        loginButton.setOnAction(e -> openUserLogin());
        signupButton.setOnAction(e -> openUserSignup());
        // save manual location when field loses focus
        if (manualLocationField != null) {
            manualLocationField.focusedProperty().addListener((obs, oldV, newV) -> { if (!newV) saveManualLocation(); });
        }
        startServer();
    }

    private void addOrUpdateCycleFromManual() {
        // Show a small dialog with two fields: model and coordinates (prefill coords from manualLocationField)
        try {
            String prefill = manualLocationField.getText();
            javafx.scene.control.Dialog<java.lang.Void> dialog = new javafx.scene.control.Dialog<>();
            dialog.setTitle("Add / Update Cycle");

            javafx.scene.control.ButtonType okType = javafx.scene.control.ButtonType.OK;
            dialog.getDialogPane().getButtonTypes().addAll(okType, javafx.scene.control.ButtonType.CANCEL);

            javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
            grid.setHgap(8);
            grid.setVgap(8);

            javafx.scene.control.Label mLabel = new javafx.scene.control.Label("Model:");
            javafx.scene.control.TextField modelField = new javafx.scene.control.TextField();
            javafx.scene.control.Label cLabel = new javafx.scene.control.Label("Coordinates (lat, lon):");
            javafx.scene.control.TextField coordsField = new javafx.scene.control.TextField(prefill == null ? "" : prefill);

            grid.add(mLabel, 0, 0);
            grid.add(modelField, 1, 0);
            grid.add(cLabel, 0, 1);
            grid.add(coordsField, 1, 1);

            dialog.getDialogPane().setContent(grid);

            // Validate input when OK is pressed
            dialog.setResultConverter(dialogButton -> null);
            dialog.showAndWait();
            java.util.Optional<javafx.scene.control.ButtonType> result = dialog.getDialogPane().getButtonTypes().stream()
                .filter(bt -> dialog.getDialogPane().lookupButton(bt).isPressed())
                .findFirst();
            if (result.isPresent() && result.get() == okType) {
                String model = modelField.getText() == null ? "" : modelField.getText().trim();
                String coords = coordsField.getText() == null ? "" : coordsField.getText().trim();
                if (model.isEmpty()) {
                    new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "Model name is required").show();
                    return;
                }
                String[] parts = coords.split(",");
                if (parts.length < 2) {
                    new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "Coordinates must be in format: lat, lon").show();
                    return;
                }
                double lat, lon;
                try {
                    lat = Double.parseDouble(parts[0].trim());
                    lon = Double.parseDouble(parts[1].trim());
                } catch (NumberFormatException ex) {
                    new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "Invalid number format for coordinates.").show();
                    return;
                }

                com.example.cycleborrowingsystem.CycleStore.Cycle c = new com.example.cycleborrowingsystem.CycleStore.Cycle(model, lat, lon);
                try {
                    com.example.cycleborrowingsystem.CycleStore.addOrUpdate(c);
                    new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION, "Cycle saved: " + model).show();
                } catch (Exception ex) {
                    new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "Failed to save cycle: " + ex.getMessage()).show();
                }
            }
        } catch (Exception ex) {
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "Error: " + ex.getMessage()).show();
        }
    }

    private void startServer() {
        try {
            landingServer = LandingServer.getInstance();
            BiConsumerOnLocation locationHandler = (lat, lon) -> {
                String coords = String.format("%.6f, %.6f", lat, lon);
                Platform.runLater(() -> {
                    locationStatusLabel.setText("Location received from phone: " + coords);
                    manualLocationField.setText(coords);
                    // Always try to save location for logged-in user
                    User cur = Session.getCurrentUser();
                    if (cur != null) {
                        try {
                            UserLocationTokenDao tokDao = new UserLocationTokenDao();
                            tokDao.createTableIfNotExists();
                            tokDao.setLocation(cur.getId(), lat, lon);
                        } catch (SQLException ignore) {}
                    }
                });
            };
            
            if (landingServer == null) {
                landingServer = new LandingServer(serverPort, locationHandler);
                landingServer.start();
            } else {
                landingServer.setOnLocation(locationHandler);
            }

            String host = landingServer.getLocalAddress();
            String url = "http://" + host + ":" + serverPort + "/pair?token=" + landingServer.getPairToken();
            pairingField.setText(url);

            LandingServer.LastLocation last = landingServer.getLastLocation(landingServer.getPairToken());
            if (last != null) {
                String coords = String.format("%.6f, %.6f", last.lat, last.lon);
                locationStatusLabel.setText("Last location: " + coords);
                manualLocationField.setText(coords);
            } else {
                locationStatusLabel.setText("Server running. Use the URL above on your phone.");
                manualLocationField.setText("");
            }
            // load saved manual location for logged-in user, or local fallback
            User cur = Session.getCurrentUser();
            if (cur != null) {
                try {
                    UserLocationTokenDao tokenDao = new UserLocationTokenDao();
                    tokenDao.createTableIfNotExists();
                    Double[] loc = tokenDao.getLocation(cur.getId());
                    if (loc != null && loc[0] != null && loc[1] != null) {
                        manualLocationField.setText(String.format("%.6f, %.6f", loc[0], loc[1]));
                        locationStatusLabel.setText("Loaded saved manual location");
                    }
                } catch (SQLException ignore) {}
            } else {
                try {
                    java.nio.file.Path f = java.nio.file.Paths.get(System.getProperty("user.home"), ".cbs_manual_location");
                    if (java.nio.file.Files.exists(f)) {
                        String txt = java.nio.file.Files.readString(f, java.nio.charset.StandardCharsets.UTF_8).trim();
                        if (!txt.isBlank()) manualLocationField.setText(txt);
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            locationStatusLabel.setText("Failed to start server: " + e.getMessage());
            manualLocationField.setText("");
        }
    }

    private void copyPairing() {
        ClipboardContent content = new ClipboardContent();
        content.putString(pairingField.getText());
        Clipboard.getSystemClipboard().setContent(content);
    }

    private void openPairing() {
        try {
            String url = pairingField.getText();
            if (url == null || !url.startsWith("http")) return;
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(new URI(url));
        } catch (Exception ignored) {}
    }

    public String getCurrentToken() {
        if (landingServer == null) return null;
        return landingServer.getPairToken();
    }

    private void openAdminLogin() {
        try {
            com.example.cycleborrowingsystem.SceneManager.switchScene("/com/example/cycleborrowingsystem/admin_login.fxml");
        } catch (Exception ignored) {}
    }

    private void openUserLogin() {
        try {
            com.example.cycleborrowingsystem.SceneManager.switchScene("/com/example/cycleborrowingsystem/login.fxml");
        } catch (Exception ignored) {}
    }

    private void openUserSignup() {
        try {
            com.example.cycleborrowingsystem.SceneManager.switchScene("/com/example/cycleborrowingsystem/signup.fxml");
        } catch (Exception ignored) {}
    }

    public void dispose() {
    }

    private void saveManualLocation() {
        try {
            User cur = Session.getCurrentUser();
            String txt = manualLocationField.getText();
            // always persist locally so signup/login can import it later
            try {
                java.nio.file.Path f = java.nio.file.Paths.get(System.getProperty("user.home"), ".cbs_manual_location");
                if (txt == null || txt.isBlank()) {
                    try { java.nio.file.Files.deleteIfExists(f); } catch (Exception ignored) {}
                } else {
                    java.nio.file.Files.writeString(f, txt.trim(), java.nio.charset.StandardCharsets.UTF_8);
                }
            } catch (Exception ignored) {}

            // if logged-in, also save into user's DB record
            if (cur != null) {
                UserLocationTokenDao tokDao = new UserLocationTokenDao();
                tokDao.createTableIfNotExists();
                if (txt == null || txt.isBlank()) {
                    tokDao.setLocation(cur.getId(), null, null);
                    return;
                }
                if (!txt.contains(",")) return;
                String[] parts = txt.split(",");
                if (parts.length < 2) return;
                try {
                    Double lat = Double.parseDouble(parts[0].trim());
                    Double lon = Double.parseDouble(parts[1].trim());
                    tokDao.setLocation(cur.getId(), lat, lon);
                    locationStatusLabel.setText("Manual location saved");
                } catch (NumberFormatException ex) {
                    // ignore invalid input
                }
            }
        } catch (SQLException ignore) {}
    }
}
