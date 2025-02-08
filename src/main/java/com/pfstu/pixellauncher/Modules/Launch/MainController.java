package com.pfstu.pixellauncher.Modules.Launch;

import com.google.gson.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainController {
    @FXML private TabPane tabPane;
    @FXML private TextField versionText;
    @FXML private TextArea logArea;
    @FXML private TextArea downloadLogArea;

    private final VersionManager versionManager = new VersionManager();
    private DownloadManager downloadManager;

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    @FXML
    public void initialize() throws IOException, InterruptedException {
        loadVersions();
    }

    private void loadVersions() {
        try {
            List<String> versions = versionManager.fetchVersions();
            for (String version : versions) {
                executorService.submit(() -> addVersionBox(version));
            }
        } catch (IOException e) {
            log("无法加载版本列表: " + e.getMessage());
        }
    }

    private void addVersionBox(String version) {
        VBox versionBox = new VBox(10);
        versionBox.setAlignment(Pos.CENTER);

        Label versionLabel = new Label("版本: " + version);
        versionLabel.setStyle("-fx-font-size: 18px; -fx-font-family: 'Segoe UI', Tahoma, sans-serif;");

        Button downloadButton = new Button("下载版本");
        downloadButton.setPrefWidth(200);
        downloadButton.setOnAction(event -> handleButtonAction(event, version, "download"));

        TextArea downloadLogAreaForTab = new TextArea();
        downloadLogAreaForTab.setEditable(false);
        downloadLogAreaForTab.setWrapText(true);
        downloadLogAreaForTab.setPrefHeight(100);
        downloadLogAreaForTab.setPrefWidth(300);
        downloadLogAreaForTab.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 10px; -fx-border-color: #d0d0d0; -fx-border-radius: 10px; -fx-padding: 10px; -fx-font-size: 14px; -fx-font-family: 'Segoe UI', Tahoma, sans-serif; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 5);");

        Button launchButton = new Button("启动游戏");
        launchButton.setPrefWidth(200);
        launchButton.setOnAction(event -> handleButtonAction(event, version, "launch"));

        TextArea logAreaForTab = new TextArea();
        logAreaForTab.setEditable(false);
        logAreaForTab.setWrapText(true);
        logAreaForTab.setPrefHeight(200);
        logAreaForTab.setPrefWidth(500);
        logAreaForTab.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 10px; -fx-border-color: #d0d0d0; -fx-border-radius: 10px; -fx-padding: 10px; -fx-font-size: 14px; -fx-font-family: 'Segoe UI', Tahoma, sans-serif; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 5);");

        versionBox.getChildren().addAll(versionLabel, downloadButton, downloadLogAreaForTab, launchButton, logAreaForTab);

        Tab versionTab = new Tab(version);
        versionTab.setContent(versionBox);

        Platform.runLater(() -> tabPane.getTabs().add(versionTab));
    }

    private void handleButtonAction(ActionEvent event, String version, String actionType) {
        if ("download".equals(actionType)) {
            System.out.println("Success");
            downloadVersionForTab(version);
        } else if ("launch".equals(actionType)) {
            launchVersionForTab(version);
        }
    }

    private void downloadVersionForTab(String version) {
        executorService.submit(() -> {
            Platform.runLater(() -> {
                try {
                    DownloadManager downloadManager = new DownloadManager(version, this);
                    downloadManager.downloadVersion();
                } catch (IOException e) {
                    log("下载版本 " + version + " 时出错: " + e.getMessage());
                } catch (InterruptedException e) {
                    log("下载版本 " + version + " 时出错: " + e.getMessage());
                }
            });
        });
    }

    private void launchVersionForTab(String version) {
        executorService.submit(() -> {
            try {
                GameLauncher launcher = new GameLauncher(
                        DownloadManager.DOWNLOAD_DIR,
                        version,
                        "Steve",
                        findLogAreaForVersion(version)
                );
                launcher.launch();
                Platform.runLater(() -> {
                    log("版本 " + version + " 启动成功！");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    log("启动版本 " + version + " 时出错: " + e.getMessage());
                });
            }
        });
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

    @FXML
    private void downloadVersion(ActionEvent actionEvent) {
        String version = versionText.getText();
        if (version == null || version.isEmpty()) {
            log("请输入版本！");
            return;
        }
        handleButtonAction(actionEvent, version, "download");
    }

    @FXML
    private void launchGame(ActionEvent actionEvent) {
        String version = versionText.getText();
        if (version == null || version.isEmpty()) {
            log("请输入版本！");
            return;
        }
        handleButtonAction(actionEvent, version, "launch");
    }

    // 关闭线程池
    @FXML
    private void closeApplication(ActionEvent actionEvent) {
        executorService.shutdown();
        // 其他关闭逻辑
    }

    @Override
    protected void finalize() throws Throwable {
        executorService.shutdown();
        super.finalize();
    }

    private TextArea findLogAreaForVersion(String version) {
        for (Tab tab : tabPane.getTabs()) {
            if (tab.getText().equals(version)) {
                VBox versionBox = (VBox) tab.getContent();
                for (int i = 0; i < versionBox.getChildren().size(); i++) {
                    if (versionBox.getChildren().get(i) instanceof TextArea) {
                        TextArea logAreaForTab = (TextArea) versionBox.getChildren().get(i + 2); // 假设 logAreaForTab 是第三个 TextArea
                        return logAreaForTab;
                    }
                }
            }
        }
        return null; // 如果找不到对应的 TextArea，返回 null
    }
}
