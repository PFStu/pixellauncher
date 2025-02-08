package com.pfstu.pixellauncher.Modules.Launch;

import com.google.gson.*;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DownloadManager {
    private static final String VERSION_MANIFEST_URL = "https://launchermeta.mojang.com/mc/game/version_manifest.json";
    public static final String OS_NAME = System.getProperty("os.name").toLowerCase(Locale.ROOT);
    public static final String DOWNLOAD_DIR = Paths.get(System.getProperty("user.dir"), "Downloads", ".minecraft").toString();

    private final HttpClient client;
    private final String versionId;
    private final MainController mainController; // 使用传入的 MainController 实例

    public DownloadManager(String versionId, MainController mainController) throws IOException, InterruptedException {
        System.out.println("接受");
        this.client = HttpClient.newHttpClient();
        this.versionId = versionId;
        this.mainController = mainController;
        createDirectories();
        downloadVersion();
    }

    private void createDirectories() {
        new File(DOWNLOAD_DIR, "versions").mkdirs();
        new File(DOWNLOAD_DIR, "libraries").mkdirs();
        new File(DOWNLOAD_DIR, "assets").mkdirs();
    }

    public void downloadVersion() throws IOException, InterruptedException {
        // 1. 获取版本清单
        JsonObject versionManifest = fetchJson(VERSION_MANIFEST_URL);
        JsonArray versions = versionManifest.getAsJsonArray("versions");

        // 2. 查找指定版本
        JsonObject targetVersion = findVersion(versions, versionId);
        if (targetVersion == null) {
            throw new RuntimeException("Version " + versionId + " not found");
        }

        // 3. 下载版本配置文件
        String versionUrl = targetVersion.get("url").getAsString();
        System.out.println("DOWNLOADING VERSION CONFIG: " + versionUrl);
        JsonObject versionConfig = fetchJson(versionUrl);
        saveVersionJson(versionConfig);

        // 4. 下载客户端JAR
        downloadClientJar(versionConfig);

        // 5. 下载资源文件
        downloadAssets(versionConfig);

        // 6. 下载依赖库
        downloadLibraries(versionConfig);
    }

    private JsonObject findVersion(JsonArray versions, String targetId) {
        for (JsonElement element : versions) {
            JsonObject version = element.getAsJsonObject();
            if (version.get("id").getAsString().equals(targetId)) {
                return version;
            }
        }
        return null;
    }

    private JsonObject fetchJson(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Failed to fetch " + url + ": " + response.statusCode());
        }
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }

    private void saveVersionJson(JsonObject versionConfig) throws IOException {
        Path versionDir = Paths.get(DOWNLOAD_DIR, "versions", versionId);
        Files.createDirectories(versionDir);
        Path jsonFile = versionDir.resolve(versionId + ".json");
        Files.writeString(jsonFile, versionConfig.toString());
    }

    private void downloadClientJar(JsonObject versionConfig) throws IOException, InterruptedException {
        JsonObject downloads = versionConfig.getAsJsonObject("downloads");
        JsonObject client = downloads.getAsJsonObject("client");
        String url = client.get("url").getAsString();
        String sha1 = client.get("sha1").getAsString();
        Path jarPath = Paths.get(DOWNLOAD_DIR, "versions", versionId, versionId + ".jar");
        downloadFile(url, jarPath, sha1);
    }

    private void downloadAssets(JsonObject versionConfig) throws IOException, InterruptedException {
        JsonObject assetIndex = versionConfig.getAsJsonObject("assetIndex");
        String assetsUrl = assetIndex.get("url").getAsString();
        String assetsId = assetIndex.get("id").getAsString();
        Path assetsDir = Paths.get(DOWNLOAD_DIR, "assets", "objects");
        Files.createDirectories(assetsDir);

        JsonObject assetsIndex = fetchJson(assetsUrl);
        JsonObject objects = assetsIndex.getAsJsonObject("objects");

        // 遍历 assets 对象
        for (Map.Entry<String, JsonElement> entry : objects.entrySet()) {
            JsonObject asset = entry.getValue().getAsJsonObject();
            String hash = asset.get("hash").getAsString();
            String assetPath = hash.substring(0, 2) + "/" + hash;
            Path target = assetsDir.resolve(assetPath);
            if (!Files.exists(target)) {
                String url = "https://resources.download.minecraft.net/" + assetPath;
                downloadFile(url, target, hash);
            }
        }
    }

    private void downloadLibraries(JsonObject versionConfig) throws IOException, InterruptedException {
        JsonArray libraries = versionConfig.getAsJsonArray("libraries");
        for (JsonElement element : libraries) {
            JsonObject lib = element.getAsJsonObject();
            if (!isLibraryAllowed(lib)) continue;

            JsonObject downloads = lib.getAsJsonObject("downloads");
            // 下载主库文件
            if (downloads.has("artifact")) {
                JsonObject artifact = downloads.getAsJsonObject("artifact");
                downloadLibrary(artifact, "");
            }

            // 下载原生库
            if (lib.has("natives")) {
                String classifier = getNativeClassifier(lib.getAsJsonObject("natives"));
                if (classifier != null && downloads.has("classifiers")) {
                    JsonObject classifiers = downloads.getAsJsonObject("classifiers");
                    if (classifiers.has(classifier)) {
                        downloadLibrary(classifiers.getAsJsonObject(classifier), classifier);
                    }
                }
            }
        }
    }

    private boolean isLibraryAllowed(JsonObject library) {
        if (!library.has("rules")) return true;
        JsonArray rules = library.getAsJsonArray("rules");
        boolean allow = false;
        for (JsonElement rule : rules) {
            JsonObject ruleObj = rule.getAsJsonObject();
            String action = ruleObj.get("action").getAsString();
            if (ruleObj.has("os")) {
                JsonObject os = ruleObj.getAsJsonObject("os");
                String osName = os.get("name").getAsString();
                boolean matches = OS_NAME.contains(osName);
                if (action.equals("allow")) {
                    allow = matches;
                } else if (action.equals("disallow")) {
                    if (matches) return false;
                }
            } else {
                allow = action.equals("allow");
            }
        }
        return allow;
    }

    private String getNativeClassifier(JsonObject natives) {
        if (OS_NAME.contains("win")) return "natives-windows";
        if (OS_NAME.contains("mac")) return "natives-macos";
        if (OS_NAME.contains("linux")) return "natives-linux";
        return null;
    }

    private void downloadLibrary(JsonObject artifact, String classifier) throws IOException, InterruptedException {
        String path = artifact.get("path").getAsString();
        Path target = Paths.get(DOWNLOAD_DIR, "libraries", path);
        if (classifier.isEmpty()) {
            downloadFile(artifact.get("url").getAsString(), target, artifact.get("sha1").getAsString());
        } else {
            // 处理原生库解压
            Path tempFile = Files.createTempFile("natives", ".jar");
            downloadFile(artifact.get("url").getAsString(), tempFile, artifact.get("sha1").getAsString());
            unzipNatives(tempFile, Paths.get(DOWNLOAD_DIR, "versions", versionId, versionId + "-natives"));
        }
    }

    private void downloadFile(String url, Path target, String expectedSha1) throws IOException, InterruptedException {
        if (Files.exists(target)) {
            if (checkFileHash(target, expectedSha1)) {
                if (mainController != null) {
                    mainController.logUpdate("File exists: " + target);
                }
                return;
            }
            Files.delete(target);
        }

        if (mainController != null) {
            mainController.logUpdate("Downloading: " + url);
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            throw new IOException("Download failed: " + response.statusCode());
        }

        Files.createDirectories(target.getParent());
        try (InputStream is = response.body()) {
            Files.copy(is, target);
        }

        if (!checkFileHash(target, expectedSha1)) {
            Files.delete(target);
            throw new IOException("SHA1 mismatch for " + target.getFileName());
        }
    }

    private boolean checkFileHash(Path file, String expectedSha1) throws IOException {
        try (InputStream is = Files.newInputStream(file)) {
            String actualSha1 = DigestUtils.sha1Hex(is);
            return actualSha1.equalsIgnoreCase(expectedSha1);
        }
    }

    private void unzipNatives(Path zipFile, Path targetDir) throws IOException {
        if (mainController != null) {
            mainController.logUpdate("Extracting natives: " + zipFile + " to " + targetDir);
        }
        Files.createDirectories(targetDir);

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path entryPath = targetDir.resolve(entry.getName());
                Files.createDirectories(entryPath.getParent());
                if (!entry.isDirectory()) {
                    try (OutputStream os = Files.newOutputStream(entryPath)) {
                        byte[] buffer = new byte[4096];
                        int length;
                        while ((length = zis.read(buffer)) >= 0) {
                            os.write(buffer, 0, length);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }
}