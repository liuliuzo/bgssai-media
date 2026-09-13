package com.bgssai.media.user.download.dto;

import java.util.Date;

/**
 * One bgssai-bot installer on this site.
 * Java properties stay camelCase; Jackson SNAKE_CASE emits file_name / size_bytes / updated_at.
 */
public class BotDownloadItem {

    private String fileName;

    private String platform;

    private Long sizeBytes;

    private Date updatedAt;

    private String url;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
