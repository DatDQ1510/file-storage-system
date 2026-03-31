package com.java.file_storage_system.externalService;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class StorageService {

    private static final long MIN_PART_SIZE = 10 * 1024 * 1024L;

    private final MinioClient minioClient;

    public void uploadFile(String bucketName, String objectName, InputStream inputStream, String contentType) {
        try {
            if (bucketName == null || bucketName.isBlank()) {
                throw new IllegalArgumentException("bucketName is required");
            }
            if (objectName == null || objectName.isBlank()) {
                throw new IllegalArgumentException("objectName is required");
            }
            if (inputStream == null) {
                throw new IllegalArgumentException("inputStream is required");
            }

            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }

            String normalizedContentType =
                    (contentType == null || contentType.isBlank()) ? "application/octet-stream" : contentType;

            minioClient.putObject(
                    PutObjectArgs.builder().bucket(bucketName).object(objectName).stream(
                            inputStream, -1, MIN_PART_SIZE)
                            .contentType(normalizedContentType)
                            .build());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to upload object to storage", e);
        }
    }
}