package com.pfstu.pixellauncher.Modules.Menu;

import com.pfstu.pixellauncher.Main;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

import java.io.IOException;

public class MainController {
    @FXML
    public void switchToDownloadWindow(ActionEvent actionEvent) throws IOException {
        new com.pfstu.pixellauncher.Main().switchWindow("Download", "MainView.fxml");
    }

    @FXML
    public void switchToLaunchWindow(ActionEvent actionEvent) throws IOException {
        new com.pfstu.pixellauncher.Main().switchWindow("Launch", "MainView.fxml");
    }
}
