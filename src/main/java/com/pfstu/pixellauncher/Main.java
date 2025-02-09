package com.pfstu.pixellauncher;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Orientation;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class Main extends Application {
    SplitPane splitPane = new SplitPane();
    Parent root1;

    public Main() throws IOException {
    }

    @Override
    public void start(Stage stage) throws IOException {
        // 菜单栏
        FXMLLoader loader1 = new FXMLLoader(getClass().getResource("Modules/Menu/MainView.fxml"));
        root1 = loader1.load();

        // 下载
        FXMLLoader loader2 = new FXMLLoader(getClass().getResource("Modules/Download/MainView.fxml"));
        Parent root2 = loader2.load();
        // 启动
        FXMLLoader loader3 = new FXMLLoader(getClass().getResource("Modules/Launch/MainView.fxml"));
        Parent root3 = loader3.load();

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

    @FXML
    public void switchWindow(String Window, String fxml) throws IOException {
        Platform.runLater(() -> {
            Parent rootSelected = null;
            try {
                rootSelected = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("Modules/" + Window + "/" + fxml)));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            splitPane.getItems().clear(); // 清空现有内容
            splitPane.getItems().addAll(root1, rootSelected);
            splitPane.setDividerPositions(0.1);
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
