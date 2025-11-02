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
            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setTitle("Admin");
            a.setHeaderText("Welcome Admin");
            a.setContentText("Admin dashboard not implemented yet.");
            a.show();
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
