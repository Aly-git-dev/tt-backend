package com.upiiz.platform_api.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;


@ConfigurationProperties(prefix = "chat.storage")
public class ChatStorageProperties {
    private String baseDir = "./data/chat-uploads";
    private long maxBytes = 15 * 1024 * 1024; // 15MB default

    public String getBaseDir() { return baseDir; }
    public void setBaseDir(String baseDir) { this.baseDir = baseDir; }
    public long getMaxBytes() { return maxBytes; }
    public void setMaxBytes(long maxBytes) { this.maxBytes = maxBytes; }
}

