package com.snovasys.trustvault.document;

import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileValidationService {

    private static final long MAX_FILE_SIZE =
            10 * 1024 * 1024;

    private static final Set<String> ALLOWED_TYPES =
            Set.of(
                    "application/pdf",
                    "text/plain",
                    "image/png",
                    "image/jpeg"
            );

    public void validate(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "File cannot be empty"
            );
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "File size cannot exceed 10 MB"
            );
        }

        String filename = file.getOriginalFilename();

        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException(
                    "Invalid filename"
            );
        }

        if (filename.contains("..")
                || filename.contains("/")
                || filename.contains("\\")) {

            throw new IllegalArgumentException(
                    "Unsafe filename"
            );
        }

        String contentType = file.getContentType();

        if (contentType == null ||
                !ALLOWED_TYPES.contains(
                        contentType.toLowerCase()
                )) {

            throw new IllegalArgumentException(
                    "File type not allowed"
            );
        }
    }
}