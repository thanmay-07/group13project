package com.example.cycleborrowingsystem.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class UserSignupController {
    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;

    @FXML
    private void onSignup() {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Sign Up");
        a.setHeaderText("Signup placeholder");
        a.setContentText("Implement signup + DB in next step.");
        a.show();
    }

    @FXML
    private void onBack() {
        try {
            com.example.cycleborrowingsystem.SceneManager.switchScene("/com/example/cycleborrowingsystem/landing.fxml");
        } catch (Exception ignored) {}
    }
}
