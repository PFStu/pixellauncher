package com.pfstu.pixellauncher;

import com.pfstu.pixellauncher.Modules.Menu.MainController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Orientation;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {
    private SplitPane splitPane = new SplitPane();
    private Parent root1;

    // 添加静态实例以便控制器访问
    private static Main instance;

    public Main() {
        instance = this;
    }

    public static Main getInstance() {
        return instance;
    }

    @Override
    public void start(Stage stage) throws IOException {
        // 加载菜单栏
        FXMLLoader loader1 = new FXMLLoader(
                getClass().getResource("/com/pfstu/pixellauncher/Modules/Menu/MainView.fxml")
        );
        root1 = loader1.load();

        // 获取菜单控制器并传递 Main 实例
        MainController menuController = loader1.getController();
        menuController.setMainApp(this);

        // 加载默认右侧界面（例如下载界面）
        FXMLLoader loader2 = new FXMLLoader(
                getClass().getResource("/com/pfstu/pixellauncher/Modules/Download/MainView.fxml")
        );
        Parent root2 = loader2.load();

        splitPane.getItems().addAll(root1, root2);
        splitPane.setOrientation(Orientation.HORIZONTAL);
        splitPane.setDividerPositions(0.1);

        Scene scene = new Scene(splitPane, 800, 600);
        stage.setTitle("Pixel Launcher Dev");
        stage.setScene(scene);
        stage.show();
    }

    public void switchWindow(String window, String fxmlPath) {
        Platform.runLater(() -> {
            try {
                // 使用绝对路径加载 FXML
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/com/pfstu/pixellauncher/Modules/" + window + "/" + fxmlPath)
                );
                Parent newRoot = loader.load();

                splitPane.getItems().clear();
                splitPane.getItems().addAll(root1, newRoot);
                splitPane.setDividerPositions(0.1);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}