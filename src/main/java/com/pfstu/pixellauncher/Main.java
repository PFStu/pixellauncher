package com.pfstu.pixellauncher;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        new FXMLLoader();
        Scene scene = new Scene(Objects.requireNonNull(FXMLLoader.load(Objects.requireNonNull(getClass().getResource("Menu/MainView.fxml")))));
        stage.setTitle("Pixel Launcher Dev");
        stage.setScene(scene);
        stage.show();
    }

    public static void switchWindow(String fxml) throws IOException {
        //TODO: 切换窗口逻辑
    }
    public static void main(String[] args) {
        launch(args);
    }
}