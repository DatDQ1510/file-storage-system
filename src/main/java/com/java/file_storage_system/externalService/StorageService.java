package com.java.file_storage_system.externalService;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.PutObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.http.Method;
import io.minio.messages.Part;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.java.file_storage_system.config.MinioProperties;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class StorageService {

    private static final int DEFAULT_EXPIRY_MINUTES = 60;
    private static final int MAX_EXPIRY_MINUTES = 7 * 24 * 60;
    private static final long MIN_PART_SIZE = 10 * 1024 * 1024L;

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    private final Set<String> ensuredBuckets = ConcurrentHashMap.newKeySet();

    public void uploadFile(String bucketName, String objectName, InputStream inputStream, String contentType) {
        try {
            String finalBucketName = resolveBucketName(bucketName);
            String finalObjectName = requireNonBlank(objectName, "objectName is required");
            if (inputStream == null) {
                throw new IllegalArgumentException("inputStream is required");
            }

            ensureBucketExists(finalBucketName);

            String normalizedContentType =
                    (contentType == null || contentType.isBlank()) ? "application/octet-stream" : contentType;

            minioClient.putObject(
                    PutObjectArgs.builder().bucket(finalBucketName).object(finalObjectName).stream(
                            inputStream, -1, MIN_PART_SIZE)
                            .contentType(normalizedContentType)
                            .build());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to upload object to storage", e);
        }
    }

    public String getPreSignedUrl(String bucketName, String objectName, Integer expiryInMinutes) {
        try {
            String finalBucketName = resolveBucketName(bucketName);
            String finalObjectName = requireNonBlank(objectName, "objectName is required");
            int finalExpiry = normalizeExpiry(expiryInMinutes);

            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(finalBucketName)
                            .object(finalObjectName)
                            .expiry(finalExpiry, TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate pre-signed URL", e);
        }
    }

    public String getPreSignedPutUrl(String bucketName, String objectName, Integer expiryInMinutes) {
        try {
            String finalBucketName = resolveBucketName(bucketName);
            String finalObjectName = requireNonBlank(objectName, "objectName is required");
            int finalExpiry = normalizeExpiry(expiryInMinutes);
            ensureBucketExists(finalBucketName);

            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(finalBucketName)
                            .object(finalObjectName)
                            .expiry(finalExpiry, TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate pre-signed PUT URL", e);
        }
    }

    public InputStream downloadObject(String bucketName, String objectName) {
        try {
            String finalBucketName = resolveBucketName(bucketName);
            String finalObjectName = requireNonBlank(objectName, "objectName is required");

            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(finalBucketName)
                            .object(finalObjectName)
                            .build()
            );
        } catch (Exception e) {
            throw new IllegalStateException("Failed to download object from storage", e);
        }
    }

    public ObjectStatResult statObject(String bucketName, String objectName) {
        try {
            String finalBucketName = resolveBucketName(bucketName);
            String finalObjectName = requireNonBlank(objectName, "objectName is required");

            StatObjectResponse response = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(finalBucketName)
                            .object(finalObjectName)
                            .build()
            );

            return new ObjectStatResult(true, response.etag(), response.size());
        } catch (Exception e) {
            return new ObjectStatResult(false, null, null);
        }
    }

    public String initiateMultipartUpload(String bucketName, String objectName) {
        try {
            String finalBucketName = resolveBucketName(bucketName);
            String finalObjectName = requireNonBlank(objectName, "objectName is required");
            ensureBucketExists(finalBucketName);

            return invokeCreateMultipartUpload(finalBucketName, finalObjectName);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initiate multipart upload", e);
        }
    }

    public String getPreSignedUrlForPart(String bucketName, String objectName, String uploadId, int partNumber, Integer expiryInMinutes) {
        try {
            String finalBucketName = resolveBucketName(bucketName);
            String finalObjectName = requireNonBlank(objectName, "objectName is required");
            String finalUploadId = requireNonBlank(uploadId, "uploadId is required");
            if (partNumber <= 0) {
                throw new IllegalArgumentException("partNumber must be greater than 0");
            }
            int finalExpiry = normalizeExpiry(expiryInMinutes);

            Map<String, String> extraParams = new HashMap<>();
            extraParams.put("uploadId", finalUploadId);
            extraParams.put("partNumber", String.valueOf(partNumber));

            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(finalBucketName)
                            .object(finalObjectName)
                            .expiry(finalExpiry, TimeUnit.MINUTES)
                            .extraQueryParams(extraParams)
                            .build()
            );
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate pre-signed URL for part", e);
        }
    }

    public void completeMultipartUpload(String bucketName, String objectName, String uploadId, Part[] parts) {
        try {
            String finalBucketName = resolveBucketName(bucketName);
            String finalObjectName = requireNonBlank(objectName, "objectName is required");
            String finalUploadId = requireNonBlank(uploadId, "uploadId is required");
            if (parts == null || parts.length == 0) {
                throw new IllegalArgumentException("parts is required");
            }

            Part[] sortedParts = Arrays.stream(parts)
                    .filter(part -> part != null && part.partNumber() > 0 && part.etag() != null && !part.etag().isBlank())
                    .sorted(Comparator.comparingInt(Part::partNumber))
                    .toArray(Part[]::new);
            if (sortedParts.length == 0) {
                throw new IllegalArgumentException("parts must contain at least one valid part");
            }

            invokeCompleteMultipartUpload(finalBucketName, finalObjectName, finalUploadId, sortedParts);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to complete multipart upload", e);
        }
    }

    private String invokeCreateMultipartUpload(String bucketName, String objectName) throws Exception {
        Optional<Class<?>> argsClassOpt = resolveClass(
                "io.minio.CreateMultipartUploadArgs",
                "io.minio.messages.CreateMultipartUploadArgs"
        );
        if (argsClassOpt.isEmpty()) {
            throw new UnsupportedOperationException("Current MinIO SDK does not expose CreateMultipartUploadArgs");
        }

        Class<?> argsClass = argsClassOpt.get();
        Object builder = argsClass.getMethod("builder").invoke(null);
        builder.getClass().getMethod("bucket", String.class).invoke(builder, bucketName);
        builder.getClass().getMethod("object", String.class).invoke(builder, objectName);
        Object args = builder.getClass().getMethod("build").invoke(builder);

        Object response = minioClient.getClass().getMethod("createMultipartUpload", argsClass).invoke(minioClient, args);
        Object result = response.getClass().getMethod("result").invoke(response);
        return (String) result.getClass().getMethod("uploadId").invoke(result);
    }

    private void invokeCompleteMultipartUpload(String bucketName, String objectName, String uploadId, Part[] parts)
            throws Exception {
        Optional<Class<?>> argsClassOpt = resolveClass(
                "io.minio.CompleteMultipartUploadArgs",
                "io.minio.messages.CompleteMultipartUploadArgs"
        );
        if (argsClassOpt.isEmpty()) {
            throw new UnsupportedOperationException("Current MinIO SDK does not expose CompleteMultipartUploadArgs");
        }

        Class<?> argsClass = argsClassOpt.get();
        Object builder = argsClass.getMethod("builder").invoke(null);
        builder.getClass().getMethod("bucket", String.class).invoke(builder, bucketName);
        builder.getClass().getMethod("object", String.class).invoke(builder, objectName);
        builder.getClass().getMethod("uploadId", String.class).invoke(builder, uploadId);
        builder.getClass().getMethod("parts", Part[].class).invoke(builder, (Object) parts);
        Object args = builder.getClass().getMethod("build").invoke(builder);

        minioClient.getClass().getMethod("completeMultipartUpload", argsClass).invoke(minioClient, args);
    }

    private Optional<Class<?>> resolveClass(String... classNames) {
        for (String className : classNames) {
            try {
                return Optional.of(Class.forName(className));
            } catch (ClassNotFoundException ignored) {
                // Try next candidate class name.
            }
        }
        return Optional.empty();
    }

    private String resolveBucketName(String bucketName) {
        String finalBucketName = (bucketName == null || bucketName.isBlank()) ? minioProperties.getBucket() : bucketName;
        return requireNonBlank(finalBucketName, "bucketName is required");
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private int normalizeExpiry(Integer expiryInMinutes) {
        if (expiryInMinutes == null || expiryInMinutes <= 0) {
            return DEFAULT_EXPIRY_MINUTES;
        }
        return Math.min(expiryInMinutes, MAX_EXPIRY_MINUTES);
    }

    private void ensureBucketExists(String bucketName) throws Exception {
        if (ensuredBuckets.contains(bucketName)) {
            return;
        }

        synchronized (ensuredBuckets) {
            if (ensuredBuckets.contains(bucketName)) {
                return;
            }

            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
            ensuredBuckets.add(bucketName);
        }
    }

    public record ObjectStatResult(boolean exists, String etag, Long size) {
    }
}
