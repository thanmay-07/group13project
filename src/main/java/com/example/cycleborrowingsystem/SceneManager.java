package com.example.cycleborrowingsystem;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SceneManager {
    private static Stage primaryStage;
    public static void init(Stage stage) {
        primaryStage = stage;
    }
    public static void switchScene(String fxmlPath) throws Exception {
        Parent root = FXMLLoader.load(SceneManager.class.getResource(fxmlPath));
        Scene scene = new Scene(root);
        scene.getStylesheets().add(SceneManager.class.getResource("/styles/styles.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.sizeToScene();
        primaryStage.show();
    }
}
