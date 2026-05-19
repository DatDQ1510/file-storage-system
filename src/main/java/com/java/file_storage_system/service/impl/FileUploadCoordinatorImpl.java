package com.java.file_storage_system.service.impl;

import com.java.file_storage_system.config.MinioProperties;
import com.java.file_storage_system.dto.chunk.ChunkPreSignBatchRequest;
import com.java.file_storage_system.dto.chunk.ChunkPreSignBatchResponse;
import com.java.file_storage_system.dto.chunk.ChunkPreSignItemRequest;
import com.java.file_storage_system.dto.chunk.ChunkPreSignItemResponse;
import com.java.file_storage_system.dto.chunk.FileAssemblyMessage;
import com.java.file_storage_system.dto.chunk.FileUploadFinalizeRequest;
import com.java.file_storage_system.dto.chunk.FileUploadFinalizeResponse;
import com.java.file_storage_system.entity.ChunkEntity;
import com.java.file_storage_system.exception.BadRequestException;
import com.java.file_storage_system.externalService.StorageService;
import com.java.file_storage_system.repository.ChunkRepository;
import com.java.file_storage_system.repository.TenantRepository;
import com.java.file_storage_system.service.FileUploadCoordinator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileUploadCoordinatorImpl implements FileUploadCoordinator {

    public static final String STATUS_DONE = "DONE";
    public static final String STATUS_UPLOADING = "UPLOADING";
    public static final String STATUS_ALREADY_EXISTS = "ALREADY_EXISTS";
    public static final String STATUS_NEED_UPLOAD = "NEED_UPLOAD";
    public static final String FILE_ASSEMBLY_EXCHANGE = "file-assembly-exchange";
    public static final String FILE_ASSEMBLY_ROUTING_KEY = "merge-key";

    private final ChunkRepository chunkRepository;
    private final TenantRepository tenantRepository;
    private final StorageService storageService;
    private final MinioProperties minioProperties;
    private final RabbitTemplate rabbitTemplate;

    @Override
    @Transactional
    public ChunkPreSignBatchResponse preSignBatch(ChunkPreSignBatchRequest request) {
        ensureTenantExists(request.tenantId());

        log.info(
            "upload.pre-sign.start tenantId={}, chunkCount={}, expiryMinutes={}",
            request.tenantId(),
            request.chunks() == null ? 0 : request.chunks().size(),
            request.expiryMinutes() == null ? 5 : request.expiryMinutes()
        );

        int expiryMinutes = request.expiryMinutes() == null ? 5 : request.expiryMinutes();
        String expiresAt = Instant.now().plus(expiryMinutes, ChronoUnit.MINUTES).toString();

        List<ChunkPreSignItemResponse> items = request.chunks()
                .stream()
                .map(chunk -> processChunkPreSign(request.tenantId(), chunk, expiryMinutes, expiresAt))
                .toList();

            log.info(
                "upload.pre-sign.done tenantId={}, chunkCount={}, expiresAt={}",
                request.tenantId(),
                items.size(),
                expiresAt
            );

        return new ChunkPreSignBatchResponse(items);
    }

    @Override
    @Transactional
    public FileUploadFinalizeResponse finalizeFileUpload(FileUploadFinalizeRequest request) {
        ensureTenantExists(request.tenantId());

        log.info(
                "upload.finalize.start tenantId={}, folderId={}, ownerId={}, fileName={}, totalSize={}, chunkCount={}",
                request.tenantId(),
                request.folderId(),
                request.ownerId(),
                request.fileName(),
                request.totalSize(),
                request.chunkHashes() == null ? 0 : request.chunkHashes().size()
        );

        Set<String> normalizedHashes = normalizeHashes(request.chunkHashes());
        if (normalizedHashes.isEmpty()) {
            throw new BadRequestException("chunkHashes is required");
        }

        List<String> missingHashes = new ArrayList<>();

        for (String hash : normalizedHashes) {
            ChunkEntity chunk = chunkRepository.findTopByTenant_IdAndChunkHashOrderByCreatedAtDesc(request.tenantId(), hash)
                    .orElse(null);

            if (chunk == null) {
                missingHashes.add(hash);
                continue;
            }

            if (STATUS_DONE.equals(chunk.getStatus())) {
                continue;
            }

            StorageService.ObjectStatResult objectStat = storageService.statObject(chunk.getBucket(), chunk.getObjectName());
            if (!objectStat.exists()) {
                missingHashes.add(hash);
                continue;
            }

            chunk.setStatus(STATUS_DONE);
            chunk.setEtag(objectStat.etag());
            if (chunk.getSize() == null && objectStat.size() != null) {
                chunk.setSize(objectStat.size());
            }
            chunkRepository.save(chunk);
        }

        if (!missingHashes.isEmpty()) {
            log.warn(
                    "upload.finalize.missing-chunks tenantId={}, fileName={}, missingCount={}, missingHashes={}",
                    request.tenantId(),
                    request.fileName(),
                    missingHashes.size(),
                    missingHashes
            );
            throw new BadRequestException("Some chunks are not uploaded successfully: " + String.join(",", missingHashes));
        }

        String messageId = UUID.randomUUID().toString();
        FileAssemblyMessage message = new FileAssemblyMessage(
                messageId,
                request.tenantId(),
            request.folderId(),
            request.ownerId(),
                request.fileName(),
                request.contentType(),
                request.totalSize(),
                List.copyOf(normalizedHashes),
                Instant.now().toString()
        );

            log.info(
                "upload.finalize.queue tenantId={}, folderId={}, ownerId={}, fileName={}, messageId={}, chunkCount={}",
                request.tenantId(),
                request.folderId(),
                request.ownerId(),
                request.fileName(),
                messageId,
                normalizedHashes.size()
            );

        rabbitTemplate.convertAndSend(
                FILE_ASSEMBLY_EXCHANGE,
                FILE_ASSEMBLY_ROUTING_KEY,
                message,
                amqpMessage -> {
                    amqpMessage.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    amqpMessage.getMessageProperties().setMessageId(messageId);
                    return amqpMessage;
                }
        );

        return new FileUploadFinalizeResponse(
                "QUEUED",
                messageId,
                FILE_ASSEMBLY_EXCHANGE,
                FILE_ASSEMBLY_ROUTING_KEY,
                normalizedHashes.size()
        );
    }

    private ChunkPreSignItemResponse processChunkPreSign(
            String tenantId,
            ChunkPreSignItemRequest item,
            int expiryMinutes,
            String expiresAt
    ) {
        String normalizedHash = normalizeHash(item.hash());

        log.debug(
            "upload.chunk.pre-sign tenantId={}, chunkHash={}, chunkSize={}",
            tenantId,
            normalizedHash,
            item.size()
        );

        ChunkEntity existingDoneChunk = chunkRepository.findFirstByTenant_IdAndChunkHashAndSizeAndStatus(
                tenantId,
                normalizedHash,
                item.size(),
                STATUS_DONE
        ).orElse(null);

        if (existingDoneChunk != null) {
            String existingObjectUrl = storageService.getPreSignedUrl(
                    existingDoneChunk.getBucket(),
                    existingDoneChunk.getObjectName(),
                    expiryMinutes
            );
            return new ChunkPreSignItemResponse(
                    normalizedHash,
                    item.size(),
                    STATUS_ALREADY_EXISTS,
                    existingDoneChunk.getBucket(),
                    existingDoneChunk.getObjectName(),
                    null,
                    existingObjectUrl,
                    expiresAt
            );
        }

        String bucket = minioProperties.getBucket();
        String objectName = buildChunkObjectName(tenantId, normalizedHash, item.size());

        ChunkEntity pending = chunkRepository.findFirstByTenant_IdAndChunkHashAndSizeAndStatus(
                tenantId,
                normalizedHash,
                item.size(),
                STATUS_UPLOADING
        ).orElseGet(() -> createPendingChunk(tenantId, normalizedHash, item.size(), bucket, objectName));

        String uploadUrl = storageService.getPreSignedPutUrl(pending.getBucket(), pending.getObjectName(), expiryMinutes);
        log.debug(
            "upload.chunk.upload-url tenantId={}, chunkHash={}, objectName={}, status={}",
            tenantId,
            normalizedHash,
            pending.getObjectName(),
            STATUS_NEED_UPLOAD
        );
        return new ChunkPreSignItemResponse(
                normalizedHash,
                item.size(),
                STATUS_NEED_UPLOAD,
                pending.getBucket(),
                pending.getObjectName(),
                uploadUrl,
                null,
                expiresAt
        );
    }

    private ChunkEntity createPendingChunk(String tenantId, String hash, Long size, String bucket, String objectName) {
        try {
            log.debug(
                "upload.chunk.create-pending tenantId={}, chunkHash={}, size={}, bucket={}, objectName={}",
                tenantId,
                hash,
                size,
                bucket,
                objectName
            );
            ChunkEntity chunk = new ChunkEntity();
            String chunkId = UUID.randomUUID().toString();
            chunk.setId(chunkId);
            chunk.setChunkId(chunkId);
            chunk.setChunkHash(hash);
            chunk.setSize(size);
            chunk.setBucket(bucket);
            chunk.setObjectName(objectName);
            chunk.setMinIOUrl(bucket + "/" + objectName);
            chunk.setStatus(STATUS_UPLOADING);
            chunk.setTenant(tenantRepository.getReferenceById(tenantId));
            return chunkRepository.save(chunk);
        } catch (DataIntegrityViolationException ex) {
            return chunkRepository.findTopByTenant_IdAndChunkHashOrderByCreatedAtDesc(tenantId, hash)
                    .orElseThrow(() -> ex);
        }
    }

    private void ensureTenantExists(String tenantId) {
        if (!tenantRepository.existsById(tenantId)) {
            log.warn("upload.tenant.not-found tenantId={}", tenantId);
            throw new BadRequestException("Tenant not found with id: " + tenantId);
        }
    }

    private String buildChunkObjectName(String tenantId, String hash, Long size) {
        return "chunks/" + tenantId + "/" + hash + "_" + size + ".bin";
    }

    private Set<String> normalizeHashes(List<String> hashes) {
        Set<String> normalized = new LinkedHashSet<>();
        for (String hash : hashes) {
            normalized.add(normalizeHash(hash));
        }
        return normalized;
    }

    private String normalizeHash(String hash) {
        if (hash == null || hash.isBlank()) {
            throw new BadRequestException("hash is required");
        }
        String normalized = hash.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("^[a-f0-9]{64}$")) {
            throw new BadRequestException("hash must be a valid sha256 hex string");
        }
        return normalized;
    }
}
