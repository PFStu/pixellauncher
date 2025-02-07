package com.pfstu.pixellauncher;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Orientation;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        new FXMLLoader();
        // 菜单栏
        FXMLLoader loader1 = new FXMLLoader(getClass().getResource("Modules/Menu/MainView.fxml"));
        Parent root1 = loader1.load();
        // 主界面
        FXMLLoader loader2 = new FXMLLoader(getClass().getResource("Modules/Launch/MainView.fxml"));
        Parent root2 = loader2.load();

        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(root1, root2);
        splitPane.setOrientation(Orientation.HORIZONTAL);
        splitPane.setStyle("-fx-background-color: transparent;");
        splitPane.lookupAll(".split-pane-divider").forEach(divider -> {
            divider.setMouseTransparent(true);
        });
        splitPane.setDividerPositions(0.1);
        Scene mainScene = new Scene(splitPane);
        stage.setTitle("Pixel Launcher Dev");
        stage.setScene(mainScene);
        stage.show();
    }

    public static void switchWindow(String fxml) throws IOException {
        //TODO: 切换窗口逻辑
    }
    public static void main(String[] args) {
        launch(args);
    }
}