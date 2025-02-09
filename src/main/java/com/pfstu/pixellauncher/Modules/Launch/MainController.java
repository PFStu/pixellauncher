package com.pfstu.pixellauncher.Modules.Launch;

import com.pfstu.pixellauncher.Modules.Download.DownloadManager;
import com.pfstu.pixellauncher.Modules.Download.GameLauncher;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class MainController {
    @FXML private ComboBox<String> versionComboBox;
    @FXML private TextField usernameField;
    @FXML private TextArea logArea;

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    @FXML
    public void initialize() {
        loadDownloadedVersions();
    }

    private void loadDownloadedVersions() {
        File downloadDir = new File(DownloadManager.DOWNLOAD_DIR);
        if (downloadDir.exists() && downloadDir.isDirectory()) {
            List<String> versions = Arrays.stream(downloadDir.listFiles(File::isDirectory))
                    .map(File::getName)
                    .collect(Collectors.toList());
            Platform.runLater(() -> versionComboBox.setItems(FXCollections.observableArrayList(versions)));
        }
    }

    @FXML
    private void launchGame(ActionEvent event) {
        String version = versionComboBox.getValue();
        String username = usernameField.getText();

        if (version == null || version.isEmpty()) {
            log("请选择版本！");
            return;
        }
        if (username == null || username.isEmpty()) {
            log("请输入用户名！");
            return;
        }

        executorService.submit(() -> {
            try {
                GameLauncher launcher = new GameLauncher(
                        DownloadManager.DOWNLOAD_DIR,
                        version,
                        username,
                        logArea
                );
                launcher.launch();
                log("游戏已启动！");
            } catch (Exception e) {
                log("启动失败: " + e.getMessage());
            }
        });
    }

    private void log(String message) {
        Platform.runLater(() -> logArea.appendText(message + "\n"));
    }

    public void logUpdate(String version, String message) {
        Platform.runLater(() -> {
            if (logArea != null) {
                logArea.appendText(message + "\n");
            } else {
                System.err.println("logArea is not initialized yet.");
            }
        });
    }

    public void shutdown() {
        executorService.shutdown();
    }
}