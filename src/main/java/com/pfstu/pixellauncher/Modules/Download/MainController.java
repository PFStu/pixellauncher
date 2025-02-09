// DownloadController.java
package com.pfstu.pixellauncher.Modules.Download;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainController {
    @FXML private TabPane tabPane;
    @FXML private TextField versionText;
    @FXML private TextArea logArea;

    private final VersionManager versionManager = new VersionManager();
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final Map<String, TextArea> versionLogAreas = new HashMap<>();

    @FXML
    public void initialize() {
        executorService.submit(this::loadVersions);
    }

    private void loadVersions() {
        try {
            List<String> versions = versionManager.fetchVersions();
            for (String version : versions) {
                Platform.runLater(() -> addVersionBox(version));
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

        versionBox.getChildren().addAll(versionLabel, downloadButton, downloadLogAreaForTab);

        Tab versionTab = new Tab(version);
        versionTab.setContent(versionBox);

        versionLogAreas.put(version, downloadLogAreaForTab);
        tabPane.getTabs().add(versionTab);
    }

    private void handleButtonAction(ActionEvent event, String version, String actionType) {
        if ("download".equals(actionType)) {
            executorService.submit(() -> downloadVersionForTab(version));
        }
    }

    private void downloadVersionForTab(String version) {
        try {
            DownloadManager downloadManager = new DownloadManager(version, this);
            downloadManager.downloadVersion();
        } catch (IOException | InterruptedException e) {
            logUpdate(version, "下载版本 " + version + " 时出错: " + e.getMessage());
        }
    }

    public void log(String message) {
        Platform.runLater(() -> logArea.appendText(message + "\n"));
    }

    public void logUpdate(String version, String message) {
        Platform.runLater(() -> {
            TextArea logArea = versionLogAreas.get(version);
            if (logArea != null) {
                logArea.appendText(message + "\n");
            } else {
                System.err.println("logArea for version " + version + " is not initialized yet.");
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
    private void closeApplication(ActionEvent actionEvent) {
        executorService.shutdown();
    }

    @Override
    protected void finalize() throws Throwable {
        executorService.shutdown();
        super.finalize();
    }
}