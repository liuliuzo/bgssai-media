package com.bgssai.media.user.config;

import java.nio.file.Path;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Serve bgssai-bot installers from disk and fall back /download SPA routes to index.html.
 */
@Configuration
public class BotDownloadResourceConfig implements WebMvcConfigurer {

    private final Environment environment;

    public BotDownloadResourceConfig(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/download").setViewName("forward:/index.html");
        registry.addViewController("/download/**").setViewName("forward:/index.html");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path downloadDirectory = Path.of(environment.getProperty(
                "bgssai.bot.download.directory", "downloads/bot")).toAbsolutePath().normalize();
        registry.addResourceHandler("/downloads/bot/**")
                .addResourceLocations(downloadDirectory.toUri().toString());
    }
}
