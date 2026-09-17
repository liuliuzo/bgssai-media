package com.bgssai.media.user.download.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;

import com.bgssai.media.user.download.dto.BotDownloadItem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListBotDownloadsServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void emptyDirectoryReturnsEmptyList() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("bgssai.bot.download.directory", tempDir.toString());
        ListBotDownloadsService service = new ListBotDownloadsService(env);
        assertTrue(service.list().isEmpty());
    }

    @Test
    void missingDirectoryReturnsEmptyList() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("bgssai.bot.download.directory", tempDir.resolve("missing").toString());
        ListBotDownloadsService service = new ListBotDownloadsService(env);
        assertTrue(service.list().isEmpty());
    }

    @Test
    void listsInstallerAndSkipsReadme() throws Exception {
        Files.writeString(tempDir.resolve("BGSSAI-Bot-Setup.exe"), "fake-bytes");
        Files.writeString(tempDir.resolve("README.txt"), "not an installer");
        MockEnvironment env = new MockEnvironment();
        env.setProperty("bgssai.bot.download.directory", tempDir.toString());
        ListBotDownloadsService service = new ListBotDownloadsService(env);
        List<BotDownloadItem> items = service.list();
        assertEquals(1, items.size());
        assertEquals("BGSSAI-Bot-Setup.exe", items.get(0).getFileName());
        assertEquals("Windows", items.get(0).getPlatform());
        assertEquals("/downloads/bot/BGSSAI-Bot-Setup.exe", items.get(0).getUrl());
    }
    @Test
    void listsMobileInstallersAndExcludesStoreBundles() throws Exception {
        Files.writeString(tempDir.resolve("BGSSAI-Bot.APK"), "test-only");
        Files.writeString(tempDir.resolve("BGSSAI-Bot.ipa"), "test-only");
        Files.writeString(tempDir.resolve("BGSSAI-Bot.aab"), "store bundle");
        Files.writeString(tempDir.resolve("README.txt"), "not an installer");
        MockEnvironment env = new MockEnvironment();
        env.setProperty("bgssai.bot.download.directory", tempDir.toString());
        List<BotDownloadItem> items = new ListBotDownloadsService(env).list();
        assertEquals(2, items.size());
        assertTrue(items.stream().anyMatch(item -> "Android".equals(item.getPlatform())
                && "/downloads/bot/BGSSAI-Bot.APK".equals(item.getUrl())));
        assertTrue(items.stream().anyMatch(item -> "iOS".equals(item.getPlatform())
                && "/downloads/bot/BGSSAI-Bot.ipa".equals(item.getUrl())));
    }
}
