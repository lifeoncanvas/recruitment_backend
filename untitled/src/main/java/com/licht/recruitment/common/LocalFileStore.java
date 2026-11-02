package com.licht.recruitment.common;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Stores files on the server's own disk. Fine for a single server with a persistent disk;
 * on hosts that wipe the disk on redeploy use {@link S3FileStore} instead.
 */
public class LocalFileStore implements FileStore {

    private final Path rootDir;

    public LocalFileStore(String uploadDir) {
        this.rootDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @Override
    public void save(String key, InputStream content, long size, String contentType) throws IOException {
        Path target = resolve(key);
        Files.createDirectories(target.getParent());
        Files.copy(content, target);
    }

    @Override
    public StoredFile open(String key) throws IOException {
        Path path = resolve(key);
        if (!Files.isRegularFile(path)) {
            return null;
        }
        return new StoredFile(Files.newInputStream(path), Files.size(path));
    }

    private Path resolve(String key) throws IOException {
        Path path = rootDir.resolve(key).normalize();
        if (!path.startsWith(rootDir)) {
            throw new IOException("Invalid storage key");
        }
        return path;
    }
}
