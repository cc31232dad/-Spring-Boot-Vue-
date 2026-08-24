package com.agromall.product.application;

import com.agromall.common.exception.BusinessException;
import com.agromall.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;
import java.util.Map;

@Service
public class LocalProductImageStorage implements ProductImageStorage {

    private static final long MAX_BYTES = 5 * 1024 * 1024;
    private final Path root;

    public LocalProductImageStorage(@Value("${agromall.upload.product-dir:uploads/products}") String directory) {
        this.root = Path.of(directory).toAbsolutePath().normalize();
    }

    @Override
    public String store(MultipartFile file) {
        String extension = validExtension(file);
        if (!contentTypeMatches(file.getContentType(), extension)) throw invalidImage();
        String detected = detectType(file);
        if (detected == null || !detected.equals(extension)) {
            throw invalidImage();
        }
        try {
            Files.createDirectories(root);
            String filename = UUID.randomUUID() + "." + extension;
            Path target = root.resolve(filename).normalize();
            if (!target.startsWith(root)) throw invalidImage();
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return "/uploads/products/" + filename;
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    private boolean contentTypeMatches(String contentType, String extension) {
        if (contentType == null) return false;
        return Map.of("jpg", "image/jpeg", "png", "image/png", "webp", "image/webp")
                .get(extension).equalsIgnoreCase(contentType);
    }

    private String validExtension(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_BYTES) throw invalidImage();
        String name = StringUtils.getFilename(file.getOriginalFilename());
        if (name == null || !name.contains(".")) throw invalidImage();
        String extension = name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (extension.equals("jpeg")) extension = "jpg";
        if (!extension.equals("jpg") && !extension.equals("png") && !extension.equals("webp")) throw invalidImage();
        return extension;
    }

    private String detectType(MultipartFile file) {
        try (InputStream input = file.getInputStream()) {
            byte[] header = input.readNBytes(12);
            if (header.length >= 8 && (header[0] & 0xff) == 0x89 && header[1] == 0x50 && header[2] == 0x4e
                    && header[3] == 0x47 && header[4] == 0x0d && header[5] == 0x0a && header[6] == 0x1a && header[7] == 0x0a) return "png";
            if (header.length >= 3 && (header[0] & 0xff) == 0xff && (header[1] & 0xff) == 0xd8 && (header[2] & 0xff) == 0xff) return "jpg";
            if (header.length >= 12 && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                    && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P') return "webp";
            return null;
        } catch (IOException exception) {
            throw invalidImage();
        }
    }

    private BusinessException invalidImage() {
        return new BusinessException(ErrorCode.VALIDATION_ERROR);
    }
}
