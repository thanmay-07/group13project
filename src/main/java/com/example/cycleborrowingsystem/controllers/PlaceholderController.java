package com.example.cycleborrowingsystem.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class PlaceholderController {
    @FXML private Button backButton;
    @FXML
    private void initialize() {
        backButton.setOnAction(e -> {
            try {
                com.example.cycleborrowingsystem.SceneManager.switchScene("/com/example/cycleborrowingsystem/landing.fxml");
            } catch (Exception ignored) {}
        });
    }
}
