package com.java.file_storage_system.controller;

import com.java.file_storage_system.dto.storage.CompleteMultipartRequest;
import com.java.file_storage_system.dto.storage.InitiateMultipartResponse;
import com.java.file_storage_system.externalService.StorageService;
import com.java.file_storage_system.payload.ApiResponse;
import io.minio.messages.Part;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/storage")
@RequiredArgsConstructor
@Validated
public class StorageController {

    private final StorageService storageService;

    @GetMapping("/presigned-url")
    public ResponseEntity<ApiResponse<String>> getPresignedUrl(
            @RequestParam(value = "bucketName", required = false) String bucketName,
            @RequestParam("objectName") @NotBlank(message = "objectName is required") String objectName,
            @RequestParam(value = "expiry", required = false, defaultValue = "60")
            @Min(value = 1, message = "expiry must be at least 1 minute")
            @Max(value = 10080, message = "expiry must be less than or equal to 10080 minutes")
            Integer expiry,
            HttpServletRequest request
    ) {
        String url = storageService.getPreSignedUrl(bucketName, objectName, expiry);
        return ResponseEntity.ok(ApiResponse.success("Pre-signed URL generated successfully", url, request.getRequestURI()));
    }

    @PostMapping("/multipart/initiate")
    public ResponseEntity<ApiResponse<InitiateMultipartResponse>> initiateMultipart(
            @RequestParam(value = "bucketName", required = false) String bucketName,
            @RequestParam("objectName") @NotBlank(message = "objectName is required") String objectName,
            HttpServletRequest request
    ) {
        String uploadId = storageService.initiateMultipartUpload(bucketName, objectName);
        return ResponseEntity.ok(ApiResponse.success(
                "Multipart upload initiated successfully",
                new InitiateMultipartResponse(uploadId, objectName),
                request.getRequestURI()
        ));
    }

    @GetMapping("/multipart/presigned-url")
    public ResponseEntity<ApiResponse<String>> getPresignedUrlForPart(
            @RequestParam(value = "bucketName", required = false) String bucketName,
            @RequestParam("objectName") @NotBlank(message = "objectName is required") String objectName,
            @RequestParam("uploadId") @NotBlank(message = "uploadId is required") String uploadId,
            @RequestParam("partNumber") @Min(value = 1, message = "partNumber must be greater than 0") int partNumber,
            @RequestParam(value = "expiry", required = false, defaultValue = "60")
            @Min(value = 1, message = "expiry must be at least 1 minute")
            @Max(value = 10080, message = "expiry must be less than or equal to 10080 minutes")
            Integer expiry,
            HttpServletRequest request
    ) {
        String url = storageService.getPreSignedUrlForPart(bucketName, objectName, uploadId, partNumber, expiry);
        return ResponseEntity.ok(ApiResponse.success("Pre-signed URL for part generated successfully", url, request.getRequestURI()));
    }

    @PostMapping("/multipart/complete")
    public ResponseEntity<ApiResponse<String>> completeMultipart(
            @RequestParam(value = "bucketName", required = false) String bucketName,
            @RequestBody @Valid CompleteMultipartRequest completeRequest,
            HttpServletRequest request
    ) {
        Part[] parts = completeRequest.parts().stream()
                .map(p -> new Part(p.partNumber(), p.etag()))
                .toArray(Part[]::new);

        storageService.completeMultipartUpload(bucketName, completeRequest.objectName(), completeRequest.uploadId(), parts);
        return ResponseEntity.ok(ApiResponse.success("Multipart upload completed successfully", "Success", request.getRequestURI()));
    }
}
