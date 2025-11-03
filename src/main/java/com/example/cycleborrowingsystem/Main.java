package com.example.cycleborrowingsystem;

import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        SceneManager.init(stage);
        SceneManager.switchScene("/com/example/cycleborrowingsystem/landing.fxml");
    }
    public static void main(String[] args) {
        launch(args);
    }
}
