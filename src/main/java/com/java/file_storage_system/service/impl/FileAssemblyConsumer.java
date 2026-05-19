package com.java.file_storage_system.service.impl;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.java.file_storage_system.config.MinioProperties;
import com.java.file_storage_system.constant.FileStatus;
import com.java.file_storage_system.dto.chunk.FileAssemblyMessage;
import com.java.file_storage_system.dto.file.CreateFileRequest;
import com.java.file_storage_system.entity.ChunkEntity;
import com.java.file_storage_system.externalService.StorageService;
import com.java.file_storage_system.repository.ChunkRepository;
import com.java.file_storage_system.repository.FileRepository;
import com.java.file_storage_system.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.java.file_storage_system.config.RabbitMQConfig.FILE_ASSEMBLY_QUEUE;

@Component
@Slf4j
@RequiredArgsConstructor
public class FileAssemblyConsumer {

    private final ChunkRepository chunkRepository;
    private final FileRepository fileRepository;
    private final FileService fileService;
    private final StorageService storageService;
    private final MinioProperties minioProperties;

    @RabbitListener(queues = FILE_ASSEMBLY_QUEUE)
    public void handleFileAssembly(FileAssemblyMessage message) {
        if (message == null) {
            return;
        }

        log.info(
                "upload.consumer.received messageId={}, tenantId={}, folderId={}, ownerId={}, fileName={}, chunkCount={}",
                message.messageId(),
                message.tenantId(),
                message.folderId(),
                message.ownerId(),
                message.fileName(),
                message.chunkHashes() == null ? 0 : message.chunkHashes().size()
        );

        String finalObjectName = buildFinalObjectName(message.tenantId(), message.messageId(), message.fileName());
        assembleAndPersist(message, finalObjectName);
    }

    @Transactional
    protected void assembleAndPersist(FileAssemblyMessage message, String finalObjectName) {
        if (fileRepository.findFirstByTenant_IdAndFolder_IdAndNameFile(
                message.tenantId(),
                message.folderId(),
                message.fileName()
        ).isPresent()) {
            log.info(
                "upload.consumer.skip-existing messageId={}, tenantId={}, folderId={}, fileName={}",
                message.messageId(),
                message.tenantId(),
                message.folderId(),
                message.fileName()
            );
            return;
        }

        List<ChunkEntity> chunks = chunkRepository.findByTenant_IdAndChunkHashInAndStatus(
                message.tenantId(),
                message.chunkHashes(),
                FileUploadCoordinatorImpl.STATUS_DONE
        );

        log.info(
            "upload.consumer.assemble-start messageId={}, tenantId={}, fileName={}, chunkFoundCount={}",
            message.messageId(),
            message.tenantId(),
            message.fileName(),
            chunks.size()
        );

        Map<String, ChunkEntity> chunkByHash = chunks.stream()
                .collect(Collectors.toMap(ChunkEntity::getChunkHash, chunk -> chunk));

        List<InputStream> orderedStreams = new ArrayList<>();
        try {
            for (String hash : message.chunkHashes()) {
                ChunkEntity chunk = chunkByHash.get(hash);
                if (chunk == null) {
                log.warn(
                    "upload.consumer.missing-chunk messageId={}, tenantId={}, chunkHash={}",
                    message.messageId(),
                    message.tenantId(),
                    hash
                );
                    throw new IllegalStateException("Missing chunk for hash: " + hash);
                }

                orderedStreams.add(storageService.downloadObject(chunk.getBucket(), chunk.getObjectName()));
            }

            InputStream assembledStream = new java.io.SequenceInputStream(Collections.enumeration(orderedStreams));
            log.info(
                "upload.consumer.upload-assembled-object messageId={}, tenantId={}, objectName={}",
                message.messageId(),
                message.tenantId(),
                finalObjectName
            );
            storageService.uploadFile(minioProperties.getBucket(), finalObjectName, assembledStream, normalizeContentType(message.contentType()));

            java.util.Map<String, Object> extraInfo = new java.util.HashMap<>();
            extraInfo.put("bucket", minioProperties.getBucket());
            extraInfo.put("objectName", finalObjectName);
            extraInfo.put("messageId", message.messageId());
            extraInfo.put("assemblyStatus", "COMPLETED");
            extraInfo.put("chunkHashes", message.chunkHashes());

            CreateFileRequest createFileRequest = new CreateFileRequest(
                    message.fileName(),
                    FileStatus.APPROVED,
                    message.totalSize() / (1024d * 1024d),
                    extraInfo,
                    message.tenantId(),
                    message.folderId(),
                    message.ownerId(),
                    null
            );

            var createdFile = fileService.createFile(createFileRequest);
            log.info(
                    "upload.consumer.file-created messageId={}, tenantId={}, folderId={}, fileId={}, fileName={}, sizeFile={}, objectName={}",
                    message.messageId(),
                    message.tenantId(),
                    message.folderId(),
                    createdFile.id(),
                    createdFile.nameFile(),
                    createdFile.sizeFile(),
                    finalObjectName
            );
        } catch (Exception exception) {
            log.error(
                    "upload.consumer.failed messageId={}, tenantId={}, folderId={}, fileName={}, error={}",
                    message.messageId(),
                    message.tenantId(),
                    message.folderId(),
                    message.fileName(),
                    exception.getMessage(),
                    exception
            );
            throw new IllegalStateException("Failed to assemble uploaded file", exception);
        } finally {
            for (InputStream inputStream : orderedStreams) {
                try {
                    inputStream.close();
                } catch (Exception ignored) {
                    // ignore close failure
                }
            }
        }
    }

    private String buildFinalObjectName(String tenantId, String messageId, String fileName) {
        String normalizedFileName = fileName == null ? "uploaded-file" : fileName.trim().replaceAll("\\s+", "_");
        return "files/" + tenantId + "/" + messageId + "/" + normalizedFileName;
    }

    private String normalizeContentType(String contentType) {
        return (contentType == null || contentType.isBlank()) ? "application/octet-stream" : contentType;
    }
}