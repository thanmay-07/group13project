package com.example.cycleborrowingsystem.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class AdminController {
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;

    @FXML
    private void onLogin() {
        String email = emailField.getText();
        String pass = passwordField.getText();
        if ("admin@cbs.local".equals(email) && "Admin@1234".equals(pass)) {
            try {
                com.example.cycleborrowingsystem.SceneManager.switchScene("/com/example/cycleborrowingsystem/admin_dashboard.fxml");
            } catch (Exception e) {
                Alert a = new Alert(Alert.AlertType.ERROR);
                a.setTitle("Admin");
                a.setHeaderText("Navigation error");
                a.setContentText("Failed to open admin dashboard: " + e.getMessage());
                a.show();
            }
        } else {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("Admin");
            a.setHeaderText("Authentication failed");
            a.setContentText("Invalid credentials.");
            a.show();
        }
    }

    @FXML
    private void onBack() {
        try {
            com.example.cycleborrowingsystem.SceneManager.switchScene("/com/example/cycleborrowingsystem/landing.fxml");
        } catch (Exception ignored) {}
    }
}
