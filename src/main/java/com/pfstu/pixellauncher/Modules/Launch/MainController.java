package com.pfstu.pixellauncher.Modules.Launch;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;

import java.io.IOException;
import java.util.List;

public class MainController {
    @FXML private ComboBox<String> versionComboBox;
    @FXML private TextArea logArea;
    @FXML private TextArea downloadLogArea;

    private final VersionManager versionManager = new VersionManager();
    private DownloadManager downloadManager;
//    private final GameLauncher gameLauncher = new GameLauncher();

    @FXML
    public void initialize() {
        loadVersions();
        downloadManager = new DownloadManager("1.8.8", this); // 初始化 DownloadManager
    }

    private void loadVersions() {
        try {
            List<String> versions = versionManager.fetchVersions();
            versionComboBox.getItems().addAll(versions);
            versionComboBox.setOnAction(event -> {
                String selectedVersion = versionComboBox.getValue();
                if (selectedVersion != null) {
                    downloadManager = new DownloadManager(selectedVersion, this);
                }
            });
        } catch (IOException e) {
            log("无法加载版本列表: " + e.getMessage());
        }
    }

    @FXML
    private void downloadVersion(ActionEvent actionEvent) {
        String version = versionComboBox.getValue();
        if (version == null) {
            log("请选择版本！");
            return;
        }
        new Thread(() -> {
            try {
                downloadManager.downloadVersion();
                log("下载完成！");
            } catch (Exception e) {
                log("下载失败: " + e.getMessage());
            }
        }).start();
    }

    @FXML
    private void launchGame() {
        System.out.println("launchGame");
        String version = versionComboBox.getValue();
        if (version == null) {
            log("请选择版本！");
            return;
        }

        new Thread(() -> {
            try {
                GameLauncher launcher = new GameLauncher(
                        DownloadManager.DOWNLOAD_DIR,
                        versionComboBox.getValue(),
                        "Steve"
                );
                System.out.println("launching game");
                launcher.launch();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    public void log(String message) {
        if (logArea != null) {
            Platform.runLater(() -> logArea.appendText(message + "\n"));
        }
    }

    public void logUpdate(String message) {
        Platform.runLater(() -> {
            if (downloadLogArea != null) {
                downloadLogArea.appendText(message + "\n");
            } else {
                System.err.println("downloadLogArea is not initialized yet.");
            }
        });
    }
}