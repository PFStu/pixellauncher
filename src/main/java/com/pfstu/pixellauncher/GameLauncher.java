package com.pfstu.pixellauncher;

import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class GameLauncher {
    private static final String OS = System.getProperty("os.name").toLowerCase();
    private static final String JAVA_PATH = detectJavaPath();
    private static final String OS_NAME = DownloadManager.OS_NAME;
    private String gameDir = DownloadManager.DOWNLOAD_DIR;
    private String versionId = "1.8.8";
    private String username = "OfflinePlayer";

    public GameLauncher(String gameDir, String versionId, String username) {
        this.gameDir = gameDir;
        this.versionId = versionId;
        this.username = username;
    }

    public void launch() throws Exception {

        // 读取版本配置文件
        JsonObject versionConfig = loadVersionConfig();

        // 构建启动命令
        List<String> command = buildLaunchCommand(versionConfig);

        //切换中文
        ToChinese.toChinese();

        // 启动游戏进程
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(new File(gameDir));
        pb.inheritIO(); // 继承控制台输入输出
        Process process = pb.start();

        int exitCode = process.waitFor();
        System.out.println("Minecraft exited with code: " + exitCode);
    }

    private JsonObject loadVersionConfig() throws IOException {
        Path configPath = Paths.get(gameDir, "versions", versionId, versionId + ".json");
        try (Reader reader = Files.newBufferedReader(configPath)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private List<String> buildLaunchCommand(JsonObject versionConfig) {
        List<String> command = new ArrayList<>();
        command.add(JAVA_PATH);

        // 添加JVM参数
        JsonElement argumentsElement = versionConfig.get("arguments");
        if (argumentsElement != null && argumentsElement.isJsonObject()) {
            JsonObject arguments = argumentsElement.getAsJsonObject();
            JsonArray jvmArgs = arguments.getAsJsonArray("jvm");
            if (jvmArgs != null) {
                for (JsonElement arg : jvmArgs) {
                    if (arg.isJsonPrimitive()) {
                        command.add(resolvePlaceholders(arg.getAsString()));
                    }
                }
            }
        } else {
            // 兼容旧版本（1.13 之前）
            command.add("-Djava.library.path=" + getNativesDir());
            command.add("-cp");
            command.add(buildClasspath(versionConfig));
            // 添加主类
            String mainClass = versionConfig.get("mainClass").getAsString();
            command.add(mainClass);

            // 添加游戏参数
            command.add("--username");
            command.add(username);
            command.add("--version");
            command.add(versionId);
            command.add("--gameDir");
            command.add(gameDir);
            command.add("--assetsDir");
            command.add(Paths.get(gameDir, "assets").toString());
            command.add("--assetIndex");
            JsonObject assetIndex = versionConfig.getAsJsonObject("assetIndex");
            if (assetIndex == null) {
                System.out.println("assetIndex 为 null");
            } else {
                JsonElement idElement = assetIndex.get("id");
                if (idElement == null) {
                    System.out.println("assetIndex 中没有 id 键");
                } else {
                    command.add(assetIndex.get("id").getAsString());
                }
            }
            command.add("--uuid");
            command.add(generateOfflineUUID(username));
            command.add("--userType");
            command.add("legacy");
            command.add("--versionType");
            command.add("release");
            // 添加 accessToken 参数（假设一个默认值，实际情况可能需要从其他地方获取）
            command.add("--accessToken");
            command.add("defaultAccessToken");
        }

        return command;
    }


    private String buildClasspath(JsonObject versionConfig) {
        List<String> libraries = new ArrayList<>();

        // 添加客户端JAR
        libraries.add(Paths.get(gameDir, "versions", versionId, versionId + ".jar").toString());

        // 添加依赖库
        JsonArray libs = versionConfig.getAsJsonArray("libraries");
        if (libs == null) {
            System.out.println("版本配置文件中缺少libraries数组");
            throw new IllegalArgumentException("版本配置文件中缺少libraries数组");
        }
        System.out.println("依赖库列表: " + libs);
        for (JsonElement lib : libs) {
            JsonObject library = lib.getAsJsonObject();
            System.out.println("处理依赖库: " + library);
            if (isLibraryAllowed(library)) {
                JsonObject downloads = library.getAsJsonObject("downloads");
                if (downloads == null) {
                    System.out.println("downloads 为 null 在依赖库: " + library);
                } else {
                    // 检查 artifact 是否存在
                    JsonObject artifact = downloads.getAsJsonObject("artifact");
                    if (artifact != null) {
                        String path = artifact.get("path").getAsString();
                        libraries.add(Paths.get(gameDir, "libraries", path).toString());
                    } else {
                        // 处理 classifiers
                        JsonObject classifiers = downloads.getAsJsonObject("classifiers");
                        if (classifiers != null) {
                            String classifierKey = OS.contains("win") ? "natives-windows" : OS.contains("mac") ? "natives-osx" : "natives-linux";
                            JsonObject classifier = classifiers.getAsJsonObject(classifierKey);
                            if (classifier != null) {
                                String path = classifier.get("path").getAsString();
                                libraries.add(Paths.get(gameDir, "libraries", path).toString());
                            } else {
                                System.out.println("classifier " + classifierKey + " 为 null 在依赖库: " + library);
                            }
                        } else {
                            System.out.println("classifiers 为 null 在依赖库: " + library);
                        }
                    }
                }
            }
        }

        return String.join(File.pathSeparator, libraries);
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

    private String resolvePlaceholders(String arg) {
        return arg.replace("${version_name}", versionId)
                .replace("${game_directory}", gameDir)
                .replace("${assets_root}", Paths.get(gameDir, "assets").toString())
                .replace("${auth_player_name}", username)
                .replace("${auth_uuid}", generateOfflineUUID(username))
                .replace("${user_type}", "legacy")
                .replace("${version_type}", "Pixel Launcher")
                .replace("${natives_directory}", getNativesDir());
    }

    private String getNativesDir() {
        return Paths.get(gameDir, "versions", versionId, versionId + "-natives").toString();
    }

    private static String generateOfflineUUID(String username) {
        // 生成离线模式UUID (v3 UUID)
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes()).toString();
    }

    private static String detectJavaPath() {
        // 优先使用JAVA_HOME环境变量
        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome != null) {
            Path javaPath = Paths.get(javaHome, "bin", "java" + (OS.contains("win") ? ".exe" : ""));
            if (Files.exists(javaPath)) {
                return javaPath.toString();
            }
        }
        // 回退到系统默认Java
        return "java";
    }

    public static void main(String[] args) {
        String gameDir = DownloadManager.DOWNLOAD_DIR;
        GameLauncher launcher = new GameLauncher(
                gameDir,
                "1.8.8",
                "OfflinePlayer"
        );

        try {
            launcher.launch();
        } catch (Exception e) {
            System.err.println("启动失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
