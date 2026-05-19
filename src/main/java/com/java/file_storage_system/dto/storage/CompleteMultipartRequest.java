package com.java.file_storage_system.dto.storage;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CompleteMultipartRequest(
    @NotBlank(message = "uploadId is required")
    String uploadId,
    @NotBlank(message = "objectName is required")
    String objectName,
    @NotEmpty(message = "parts is required")
    List<@Valid PartDTO> parts
) {}
