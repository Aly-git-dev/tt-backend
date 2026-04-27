package com.upiiz.platform_api.storage;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class ChatFileStorage {

    private final ChatStorageProperties props;

    public ChatFileStorage(ChatStorageProperties props) {
        this.props = props;
    }

    public StoredFile store(Long conversationId, Long messageId, MultipartFile file) throws IOException {
        if (file.isEmpty()) throw new IllegalArgumentException("Empty file");
        if (file.getSize() > props.getMaxBytes()) throw new IllegalArgumentException("File too large");

        String safeName = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename().replaceAll("[\\\\/]", "_");
        String ext = "";
        int dot = safeName.lastIndexOf('.');
        if (dot >= 0 && dot < safeName.length()-1) ext = safeName.substring(dot);

        String uuid = UUID.randomUUID().toString();
        Path dir = Paths.get(props.getBaseDir(), "conv-" + conversationId, "msg-" + messageId);
        Files.createDirectories(dir);

        Path out = dir.resolve(uuid + ext);
        Files.copy(file.getInputStream(), out, StandardCopyOption.REPLACE_EXISTING);

        return new StoredFile(out.toString(), safeName, file.getContentType(), file.getSize());
    }

    public Path resolve(String storedPath) {
        return Paths.get(storedPath).toAbsolutePath().normalize();
    }

    public record StoredFile(String path, String originalName, String mimeType, long sizeBytes) {}
}
