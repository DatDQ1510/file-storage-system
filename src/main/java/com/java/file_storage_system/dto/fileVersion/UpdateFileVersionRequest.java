package com.java.file_storage_system.dto.fileVersion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdateFileVersionRequest(
        @NotNull(message = "fileVersion is required")
        @Positive(message = "fileVersion must be greater than 0")
        Integer fileVersion,

        @NotBlank(message = "fileHash is required")
        String fileHash,

        @NotNull(message = "sizeFile is required")
        @Positive(message = "sizeFile must be greater than 0")
        Double sizeFile,

        @NotBlank(message = "fileId is required")
        String fileId
) {
}