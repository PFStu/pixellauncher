package com.pfstu.pixellauncher.Modules.Download;

import com.google.gson.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.UUID;

import javafx.application.Platform;
import javafx.scene.control.TextArea;

public class GameLauncher {
    private static final String OS = System.getProperty("os.name").toLowerCase();
    private static final String JAVA_PATH = detectJavaPath();
    private static final String OS_NAME = DownloadManager.OS_NAME;
    private static String gameDir = DownloadManager.DOWNLOAD_DIR;
    static String versionId = "";
    private static String username = "OfflinePlayer";
    private static TextArea logArea; // 添加静态 logArea 字段
    private static String classpath; // 添加静态 classpath 字段

    public GameLauncher(String gameDir, String versionId, String username, TextArea logArea) {
        GameLauncher.gameDir = gameDir;
        GameLauncher.versionId = versionId;
        GameLauncher.username = username;
        GameLauncher.logArea = logArea; // 初始化静态 logArea
        try {
            GameLauncher.classpath = buildClasspath(loadVersionConfig());
        } catch (IOException e) {
            log("构建类路径失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void launch() throws Exception {
        try {
            System.out.println("启动游戏");
            log("启动游戏");

            // 读取版本配置文件
            JsonObject versionConfig = loadVersionConfig();

            // 构建启动命令
            List<String> command = buildLaunchCommand(versionConfig);

            // 切换中文
            ToChinese.toChinese();

            // 启动游戏进程
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new File(GameLauncher.gameDir));
            pb.redirectErrorStream(true); // 将错误流重定向到输出流
            Process process = pb.start();

            // 重定向游戏进程的输出到指定的 TextArea
            StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream(), GameLauncher::log);
            new Thread(outputGobbler).start();

            int exitCode = process.waitFor();
            log("Minecraft 已退出: " + exitCode);
        } catch (IOException | InterruptedException e) {
            log("启动失败: " + e.getMessage());
            log("启动失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static JsonObject loadVersionConfig() throws IOException {
        Path configPath = Paths.get(GameLauncher.gameDir, "versions", GameLauncher.versionId, GameLauncher.versionId + ".json");
        log("加载版本配置文件: " + configPath);
        try (Reader reader = Files.newBufferedReader(configPath)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static List<String> buildLaunchCommand(JsonObject versionConfig) throws IOException {
        List<String> command = new ArrayList<>();
        command.add(JAVA_PATH);

        // ==================== 1. 添加 JVM 参数 ====================
        // 公共 JVM 参数
        command.add("-Djava.library.path=" + getNativesDir());
        command.add("-Dminecraft.launcher.brand=PixelLauncher");
        command.add("-Dminecraft.launcher.version=1.0.0");
        command.add("-cp");
        command.add(GameLauncher.classpath); // 使用已经构建好的类路径

        // 从版本配置文件中读取 JVM 参数（如果有）
        JsonElement argumentsElement = versionConfig.get("arguments");
        if (argumentsElement != null && argumentsElement.isJsonObject()) {
            JsonObject arguments = argumentsElement.getAsJsonObject();
            JsonArray jvmArgs = arguments.getAsJsonArray("jvm");
            if (jvmArgs != null) {
                for (JsonElement arg : jvmArgs) {
                    if (arg.isJsonPrimitive()) {
                        String resolvedArg = resolvePlaceholders(arg.getAsString());
                        command.add(resolvedArg);
                    }
                }
            }
        }

        // ==================== 2. 添加主类名称 ====================
        JsonElement mainClassElement = versionConfig.get("mainClass");
        if (mainClassElement != null && mainClassElement.isJsonPrimitive()) {
            String mainClass = mainClassElement.getAsString();
            command.add(mainClass);
        } else {
            throw new IllegalArgumentException("版本配置文件中缺少 mainClass 键");
        }

        // ==================== 3. 添加游戏参数 ====================
        // 从版本配置文件中读取游戏参数（如果有）
        if (argumentsElement != null && argumentsElement.isJsonObject()) {
            JsonObject arguments = argumentsElement.getAsJsonObject();
            JsonArray gameArgs = arguments.getAsJsonArray("game");
            if (gameArgs != null) {
                for (JsonElement arg : gameArgs) {
                    if (arg.isJsonPrimitive()) {
                        String resolvedArg = resolvePlaceholders(arg.getAsString());
                        command.add(resolvedArg);
                    }
                }
            }
        } else {
            // 兼容旧版本（1.13 之前）
            Collections.addAll(command,
                    "--username", GameLauncher.username,
                    "--version", GameLauncher.versionId,
                    "--gameDir", GameLauncher.gameDir,
                    "--assetsDir", Paths.get(GameLauncher.gameDir, "assets").toString(),
                    "--assetIndex", versionConfig.getAsJsonObject("assetIndex").get("id").getAsString(),
                    "--uuid", generateOfflineUUID(GameLauncher.username),
                    "--userType", "legacy",
                    "--versionType", "release",
                    "--accessToken", "defaultAccessToken"
            );
        }

        // 记录构建的启动命令
        log("构建的启动命令: " + String.join(" ", command));
        return command;
    }

    private static String buildClasspath(JsonObject versionConfig) throws IOException {
        List<String> libraries = new ArrayList<>();

        // 添加客户端JAR
        String clientJarPath = Paths.get(GameLauncher.gameDir, "versions", GameLauncher.versionId, GameLauncher.versionId + ".jar").toString();
        if (!Files.exists(Paths.get(clientJarPath))) {
            log("客户端JAR文件不存在: " + clientJarPath);
            throw new FileNotFoundException("客户端JAR文件不存在: " + clientJarPath);
        }
        libraries.add(clientJarPath);

        // 添加依赖库
        JsonArray libs = versionConfig.getAsJsonArray("libraries");
        if (libs == null) {
            log("版本配置文件中缺少 libraries 数组");
            throw new IllegalArgumentException("版本配置文件中缺少 libraries 数组");
        }
        for (JsonElement lib : libs) {
            JsonObject library = lib.getAsJsonObject();
            if (isLibraryAllowed(library)) {
                JsonObject downloads = library.getAsJsonObject("downloads");
                if (downloads != null) {
                    // 检查 artifact 是否存在
                    JsonObject artifact = downloads.getAsJsonObject("artifact");
                    if (artifact != null) {
                        String path = artifact.get("path").getAsString();
                        String libraryPath = Paths.get(GameLauncher.gameDir, "libraries", path).toString();
                        if (!Files.exists(Paths.get(libraryPath))) {
                            log("库文件不存在: " + libraryPath);
                            throw new FileNotFoundException("库文件不存在: " + libraryPath);
                        }
                        libraries.add(libraryPath);
                    } else {
                        // 处理 classifiers
                        JsonObject classifiers = downloads.getAsJsonObject("classifiers");
                        if (classifiers != null) {
                            String classifierKey = OS.contains("win") ? "natives-windows" : OS.contains("mac") ? "natives-osx" : "natives-linux";
                            JsonObject classifier = classifiers.getAsJsonObject(classifierKey);
                            if (classifier != null) {
                                String path = classifier.get("path").getAsString();
                                String libraryPath = Paths.get(GameLauncher.gameDir, "libraries", path).toString();
                                if (!Files.exists(Paths.get(libraryPath))) {
                                    log("库文件不存在: " + libraryPath);
                                    throw new FileNotFoundException("库文件不存在: " + libraryPath);
                                }
                                libraries.add(libraryPath);
                            } else {
                                log("classifier " + classifierKey + " 为 null 在依赖库: " + library);
                            }
                        } else {
                            log("classifiers 为 null 在依赖库: " + library);
                        }
                    }
                } else {
                    log("downloads 为 null 在依赖库: " + library);
                }
            }
        }

        String cp = String.join(File.pathSeparator, libraries);
        log("构建的类路径: " + cp);
        return cp;
    }

    private static boolean isLibraryAllowed(JsonObject library) {
        if (!library.has("rules")) return true;
        JsonArray rules = library.getAsJsonArray("rules");
        boolean allow = false;
        for (JsonElement rule : rules) {
            JsonObject ruleObj = rule.getAsJsonObject();
            String action = ruleObj.get("action").getAsString();
            if (ruleObj.has("os")) {
                JsonObject os = ruleObj.getAsJsonObject("os");
                String osName = os.get("name").getAsString();
                boolean matches = GameLauncher.OS_NAME.contains(osName);
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

    private static String resolvePlaceholders(String arg) {
        String resolvedArg = arg.replace("${version_name}", GameLauncher.versionId)
                .replace("${game_directory}", GameLauncher.gameDir)
                .replace("${assets_root}", Paths.get(GameLauncher.gameDir, "assets").toString())
                .replace("${auth_player_name}", GameLauncher.username)
                .replace("${auth_uuid}", generateOfflineUUID(GameLauncher.username))
                .replace("${user_type}", "legacy")
                .replace("${version_type}", "release") // 修改为 "release"
                .replace("${natives_directory}", getNativesDir())
                .replace("${launcher_name}", "PixelLauncher")
                .replace("${launcher_version}", "1.0.0")
                .replace("${classpath}", GameLauncher.classpath); // 使用已经构建好的类路径
        log("原始参数: " + arg + " -> 替换后的参数: " + resolvedArg);
        return resolvedArg;
    }

    private static String getNativesDir() {
        String nativesDir = Paths.get(GameLauncher.gameDir, "versions", GameLauncher.versionId, GameLauncher.versionId + "-natives").toString();
        log("natives 目录: " + nativesDir);
        return nativesDir;
    }

    private static String generateOfflineUUID(String username) {
        // 生成离线模式UUID (v3 UUID)
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes()).toString();
    }

    private static String detectJavaPath() {
        // 优先查找 Java 17
        String[] possibleJavaHomes = {
                System.getenv("JAVA_HOME_17"),
                "C:\\Program Files\\Java\\jdk-17", // 示例路径
                "C:\\Program Files\\Java\\jre-17"
        };

        for (String javaHome : possibleJavaHomes) {
            if (javaHome != null) {
                Path javaPath = Paths.get(javaHome, "bin", "java" + (OS.contains("win") ? ".exe" : ""));
                if (Files.exists(javaPath)) {
                    return javaPath.toString();
                }
            }
        }

        // 回退到环境变量中的 JAVA_HOME
        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome != null) {
            Path javaPath = Paths.get(javaHome, "bin", "java" + (OS.contains("win") ? ".exe" : ""));
            if (Files.exists(javaPath)) {
                return javaPath.toString();
            }
        }

        // 最后使用系统默认
        return "java";
    }

    public static void log(String message) {
        if (GameLauncher.logArea != null) {
            Platform.runLater(() -> GameLauncher.logArea.appendText(message + "\n"));
        } else {
            System.out.println(message);
        }
    }

    private static void checkLatestLog() {
        Path logPath = Paths.get(GameLauncher.gameDir, "logs", "latest.log");
        if (Files.exists(logPath)) {
            try (BufferedReader reader = Files.newBufferedReader(logPath)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log(line);
                }
            } catch (IOException e) {
                System.err.println("读取日志文件失败: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            log("日志文件不存在: " + logPath);
        }
    }

    public static void main(String[] args) {
        String gameDir = DownloadManager.DOWNLOAD_DIR;
        TextArea logArea = new TextArea(); // 创建一个 TextArea 实例
        GameLauncher launcher = new GameLauncher(
                gameDir,
                "1.19.2", // 修改为 1.19.2
                "OfflinePlayer",
                logArea // 传入 logArea 实例
        );

        try {
            launcher.launch();
            checkLatestLog(); // 读取并记录日志文件内容
        } catch (Exception e) {
            System.err.println("启动失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
