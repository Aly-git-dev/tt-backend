package com.upiiz.platform_api.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class ForumFileStorage {

    private final Path baseDir;
    private final long maxBytes;

    public ForumFileStorage(
            @Value("${forum.storage.baseDir:${chat.storage.baseDir:/app/data/forum-uploads}}") String baseDir,
            @Value("${forum.storage.maxBytes:${chat.storage.maxBytes:104857600}}") long maxBytes
    ) {
        this.baseDir = Paths.get(baseDir).toAbsolutePath().normalize();
        this.maxBytes = maxBytes;
    }

    public StoredForumFile storeForThread(Long threadId, MultipartFile file) throws IOException {
        return store("thread-" + threadId, file);
    }

    public StoredForumFile storeForPost(Long postId, MultipartFile file) throws IOException {
        return store("post-" + postId, file);
    }

    public Path resolve(String storedPath) {
        Path path = Paths.get(storedPath).toAbsolutePath().normalize();
        if (!path.startsWith(baseDir)) {
            throw new IllegalArgumentException("Ruta de archivo invalida");
        }
        return path;
    }

    private StoredForumFile store(String ownerDir, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo esta vacio");
        }
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException("El archivo excede el tamano permitido");
        }

        String safeName = sanitize(file.getOriginalFilename());
        String extension = extensionOf(safeName);

        Path dir = baseDir.resolve(ownerDir);
        Files.createDirectories(dir);

        Path out = dir.resolve(UUID.randomUUID() + extension).normalize();
        if (!out.startsWith(dir)) {
            throw new IllegalArgumentException("Nombre de archivo invalido");
        }

        Files.copy(file.getInputStream(), out, StandardCopyOption.REPLACE_EXISTING);

        return new StoredForumFile(
                out.toString(),
                safeName,
                file.getContentType() == null ? "application/octet-stream" : file.getContentType(),
                file.getSize()
        );
    }

    private String sanitize(String originalName) {
        String value = originalName == null || originalName.isBlank() ? "archivo" : originalName;
        return value
                .replaceAll("[\\\\/]", "_")
                .replace("\r", "")
                .replace("\n", "")
                .trim();
    }

    private String extensionOf(String safeName) {
        int dot = safeName.lastIndexOf('.');
        if (dot < 0 || dot == safeName.length() - 1) {
            return "";
        }
        return safeName.substring(dot);
    }

    public record StoredForumFile(String path, String originalName, String mimeType, long sizeBytes) {}
}
