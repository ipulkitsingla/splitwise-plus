package com.splitwiseplusplus.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Objects;
import java.util.UUID;

/**
 * File storage service — saves uploaded files to a local directory.
 * Can be extended to use AWS S3, GCS, or Azure Blob Storage.
 */
@Service
@Slf4j
public class FileStorageService {

    @Value("${app.upload.dir}")
    private String uploadDir;

    public String storeFile(MultipartFile file, String subDir) {
        String originalName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String ext = originalName.contains(".") ?
                originalName.substring(originalName.lastIndexOf('.')) : "";
        String fileName = UUID.randomUUID() + ext;

        try {
            Path targetDir = Paths.get(uploadDir, subDir).toAbsolutePath().normalize();
            Files.createDirectories(targetDir);
            Path targetPath = targetDir.resolve(fileName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.debug("File stored: {}", targetPath);
            return "/uploads/" + subDir + "/" + fileName;
        } catch (IOException e) {
            throw new RuntimeException("Could not store file " + fileName, e);
        }
    }

    public void deleteFile(String fileUrl) {
        try {
            Path filePath = Paths.get(uploadDir + fileUrl.replace("/uploads", ""));
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("Could not delete file: {}", fileUrl);
        }
    }
}
