package com.bgssai.media.user.download.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.bgssai.media.user.download.dto.BotDownloadItem;

/**
 * List bgssai-bot installers on disk. Empty directory returns an empty list.
 */
@Service
public class ListBotDownloadsService {

    private static final Logger log = LoggerFactory.getLogger(ListBotDownloadsService.class);

    private static final List<String> INSTALLER_SUFFIXES =
            List.of(".exe", ".msi", ".dmg", ".pkg", ".appimage", ".deb", ".rpm", ".tar.gz", ".zip", ".apk", ".ipa");

    private final Path directory;

    public ListBotDownloadsService(Environment environment) {
        this.directory = Path.of(environment.getProperty(
                "bgssai.bot.download.directory", "downloads/bot")).toAbsolutePath().normalize();
    }

    public List<BotDownloadItem> list() {
        List<BotDownloadItem> items = new ArrayList<>();
        if (!Files.isDirectory(this.directory)) {
            return items;
        }
        try (Stream<Path> files = Files.list(this.directory)) {
            files.filter(Files::isRegularFile).forEach((file) -> {
                String name = file.getFileName().toString();
                if (!isInstaller(name)) {
                    return;
                }
                try {
                    BotDownloadItem item = new BotDownloadItem();
                    item.setFileName(name);
                    item.setPlatform(platformOf(name));
                    item.setSizeBytes(Files.size(file));
                    item.setUpdatedAt(new Date(Files.getLastModifiedTime(file).toMillis()));
                    item.setUrl("/downloads/bot/" + name);
                    items.add(item);
                } catch (IOException ex) {
                    log.warn("读取 Bot 安装包失败 file={}", name);
                }
            });
        } catch (IOException ex) {
            log.warn("扫描 Bot 下载目录失败 directory={}", this.directory);
        }
        items.sort(Comparator.comparing(BotDownloadItem::getUpdatedAt).reversed());
        return items;
    }

    static boolean isInstaller(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return INSTALLER_SUFFIXES.stream().anyMatch(lower::endsWith);
    }

    static String platformOf(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".apk")) {
            return "Android";
        }
        if (lower.endsWith(".ipa")) {
            return "iOS";
        }
        if (lower.endsWith(".exe") || lower.endsWith(".msi")) {
            return "Windows";
        }
        if (lower.endsWith(".dmg") || lower.endsWith(".pkg")) {
            return "macOS";
        }
        if (lower.endsWith(".appimage") || lower.endsWith(".deb") || lower.endsWith(".rpm")) {
            return "Linux";
        }
        if (lower.contains("windows") || lower.contains("win")) {
            return "Windows";
        }
        if (lower.contains("darwin") || lower.contains("mac")) {
            return "macOS";
        }
        if (lower.contains("linux")) {
            return "Linux";
        }
        return "";
    }
}
