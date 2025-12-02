package com.example.demo;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Parent;

public class App extends Application {
    private MainController controller;

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/MainView.fxml"));
        Parent root = loader.load();
        controller = loader.getController();

        Scene scene = new Scene(root, 1000, 700);
        primaryStage.setTitle("Product Management - MongoDB (JavaFX)");
        primaryStage.setScene(scene);
        primaryStage.show();

        primaryStage.setOnCloseRequest(evt -> {
            if (controller != null) {
                try { controller.close(); } catch (Exception ignored) {}
            }
        });
    }

    public static void main(String[] args) {
        launch();
    }
}