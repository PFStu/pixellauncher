package com.pfstu.pixellauncher.Modules.Download;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;

public class ToChinese {
    private static final String VERSION_ID = "1.8.8";
    static String gameDir = DownloadManager.DOWNLOAD_DIR;
    public static void toChinese() throws IOException {
        Path versionDir = Paths.get(gameDir, VERSION_ID);
        Path optionsFile = Paths.get(String.valueOf(versionDir), "options.txt");
        String langEntry = "lang:zh_cn";

        // 确保版本目录存在
        if (!Files.exists(versionDir)) {
            Files.createDirectories(versionDir);
        }

        if (!Files.exists(optionsFile)) {
            Files.write(optionsFile, Collections.singletonList(langEntry), StandardOpenOption.CREATE);
            return;
        }

        List<String> lines = Files.readAllLines(optionsFile);
        boolean hasLang = lines.stream().anyMatch(line -> line.startsWith("lang:"));

        if (!hasLang) {
            lines.add(langEntry);
            Files.write(optionsFile, lines);
        } else {
            // 如果已有语言设置，强制改为中文
            lines.replaceAll(line -> line.startsWith("lang:") ? langEntry : line);
            Files.write(optionsFile, lines);
        }
    }
}
