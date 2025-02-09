package com.pfstu.pixellauncher.Modules.Menu;

import com.pfstu.pixellauncher.Main;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public class MainController {
    private Main mainApp;

    // 设置 Main 实例的引用
    public void setMainApp(Main mainApp) {
        this.mainApp = mainApp;
    }

    @FXML
    public void switchToDownloadWindow(ActionEvent actionEvent) {
        mainApp.switchWindow("Download", "MainView.fxml");
    }

    @FXML
    public void switchToLaunchWindow(ActionEvent actionEvent) {
        mainApp.switchWindow("Launch", "MainView.fxml");
    }
}