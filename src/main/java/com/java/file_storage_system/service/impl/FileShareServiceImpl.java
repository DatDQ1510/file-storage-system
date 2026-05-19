package com.java.file_storage_system.service.impl;

import com.java.file_storage_system.context.UserContext;
import com.java.file_storage_system.dto.file.FileShareResponse;
import com.java.file_storage_system.dto.file.ShareFileRequest;
import com.java.file_storage_system.entity.FileEntity;
import com.java.file_storage_system.entity.FileShareEntity;
import com.java.file_storage_system.entity.UserEntity;
import com.java.file_storage_system.exception.ConflictException;
import com.java.file_storage_system.exception.ForbiddenException;
import com.java.file_storage_system.exception.ResourceNotFoundException;
import com.java.file_storage_system.repository.FileRepository;
import com.java.file_storage_system.repository.FileShareRepository;
import com.java.file_storage_system.repository.UserRepository;
import com.java.file_storage_system.service.FileService;
import com.java.file_storage_system.service.FileShareService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementation của FileShareService
 *
 * Permission logic:
 *  - Chỉ file owner hoặc project owner mới được share file
 *  - Không được share cho chính file owner
 *  - File, user, và actor phải cùng tenant
 */
@Service
@RequiredArgsConstructor
public class FileShareServiceImpl implements FileShareService {

    private final FileRepository fileRepository;
    private final FileShareRepository fileShareRepository;
    private final UserRepository userRepository;
    private final FileService fileService;
    private final UserContext userContext;

    @Override
    @Transactional
    public FileShareResponse shareFile(String fileId, ShareFileRequest request) {
        FileEntity file = findFile(fileId);
        UserEntity sharedWithUser = findUser(request.sharedWithUserId());

        String actorId = userContext.getId();
        String actorTenantId = userContext.getTenantId();

        // ✅ Kiểm tra tenant consistency
        if (!file.getTenant().getId().equals(actorTenantId)) {
            throw new ForbiddenException("Actor không cùng tenant với file");
        }

        if (!file.getTenant().getId().equals(sharedWithUser.getTenant().getId())) {
            throw new ForbiddenException("Shared user không cùng tenant với file");
        }

        // ✅ Chỉ file owner hoặc project owner mới được share
        boolean isFileOwner = file.getOwner().getId().equals(actorId);
        boolean isProjectOwner = file.getFolder().getProject().getOwner().getId().equals(actorId);

        if (!isFileOwner && !isProjectOwner) {
            throw new ForbiddenException("Chỉ file owner hoặc project owner mới được share file");
        }

        // ✅ Không được share cho chính file owner
        if (file.getOwner().getId().equals(sharedWithUser.getId())) {
            throw ConflictException.withMessage("Không được share file cho chính file owner");
        }

        // ✅ Kiểm tra xem đã share trước đó chưa
        if (fileShareRepository.findByFileIdAndSharedWithUserId(fileId, request.sharedWithUserId()).isPresent()) {
            throw ConflictException.withMessage("File đã share cho user này rồi");
        }

        // ✅ Tạo new FileShare
        FileShareEntity fileShare = new FileShareEntity();
        fileShare.setFileId(fileId);
        fileShare.setSharedWithUserId(request.sharedWithUserId());
        fileShare.setSharedByUserId(actorId);
        fileShare.setPermission(request.permission());
        fileShare.setExpiresAt(request.expiresAt());

        FileShareEntity saved = fileShareRepository.save(fileShare);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void unshareFile(String fileId, String sharedWithUserId) {
        FileEntity file = findFile(fileId);

        String actorId = userContext.getId();
        String actorTenantId = userContext.getTenantId();

        // ✅ Kiểm tra tenant consistency
        if (!file.getTenant().getId().equals(actorTenantId)) {
            throw new ForbiddenException("Actor không cùng tenant với file");
        }

        // ✅ Chỉ file owner, project owner, hoặc người share mới được revoke
        boolean isFileOwner = file.getOwner().getId().equals(actorId);
        boolean isProjectOwner = file.getFolder().getProject().getOwner().getId().equals(actorId);

        if (!isFileOwner && !isProjectOwner) {
            // Check if actor is the one who shared
            var fileShareOpt = fileShareRepository.findByFileIdAndSharedWithUserId(
                    fileId,
                    sharedWithUserId
            );

            if (fileShareOpt.isEmpty()) {
                throw ResourceNotFoundException.byField("FileShare", "fileId-sharedWithUserId", fileId + "-" + sharedWithUserId);
            }

            if (!fileShareOpt.get().getSharedByUserId().toString().equals(actorId)) {
                throw new ForbiddenException("Chỉ file owner, project owner, hoặc người share mới được revoke");
            }
        }

        fileShareRepository.deleteByFileIdAndSharedWithUserId(fileId, sharedWithUserId);
    }

    @Override
    @Transactional
    public FileShareResponse updateFileShare(String fileId, String sharedWithUserId, ShareFileRequest request) {
        FileEntity file = findFile(fileId);
        FileShareEntity fileShare = fileShareRepository.findByFileIdAndSharedWithUserId(
            fileId,
            sharedWithUserId
        ).orElseThrow(() -> ResourceNotFoundException.byField("FileShare", "fileId-sharedWithUserId", fileId + "-" + sharedWithUserId));

        String actorId = userContext.getId();
        String actorTenantId = userContext.getTenantId();

        // ✅ Kiểm tra tenant consistency
        if (!file.getTenant().getId().equals(actorTenantId)) {
            throw new ForbiddenException("Actor không cùng tenant với file");
        }

        // ✅ Chỉ file owner, project owner, hoặc người share mới được update
        boolean isFileOwner = file.getOwner().getId().equals(actorId);
        boolean isProjectOwner = file.getFolder().getProject().getOwner().getId().equals(actorId);
        boolean isSharedBy = fileShare.getSharedByUserId().toString().equals(actorId);

        if (!isFileOwner && !isProjectOwner && !isSharedBy) {
            throw new ForbiddenException("Chỉ file owner, project owner, hoặc người share mới được update");
        }

        // ✅ Update permission và expiresAt
        fileShare.setPermission(request.permission());
        fileShare.setExpiresAt(request.expiresAt());

        FileShareEntity updated = fileShareRepository.save(fileShare);
        return mapToResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileShareResponse> getFileShares(String fileId) {
        FileEntity file = findFile(fileId);

        String actorId = userContext.getId();
        String actorTenantId = userContext.getTenantId();

        // ✅ Kiểm tra tenant consistency
        if (!file.getTenant().getId().equals(actorTenantId)) {
            throw new ForbiddenException("Actor không cùng tenant với file");
        }

        // ✅ Chỉ file owner hoặc project owner mới được xem
        boolean isFileOwner = file.getOwner().getId().equals(actorId);
        boolean isProjectOwner = file.getFolder().getProject().getOwner().getId().equals(actorId);

        if (!isFileOwner && !isProjectOwner) {
            throw new ForbiddenException("Chỉ file owner hoặc project owner mới được xem file shares");
        }

        return fileShareRepository.findAllByFileId(fileId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FileShareResponse getFileShare(String fileId, String sharedWithUserId) {
        FileEntity file = findFile(fileId);
        FileShareEntity fileShare = fileShareRepository.findByFileIdAndSharedWithUserId(
            fileId,
            sharedWithUserId
        ).orElseThrow(() -> ResourceNotFoundException.byField("FileShare", "fileId-sharedWithUserId", fileId + "-" + sharedWithUserId));

        String actorId = userContext.getId();
        String actorTenantId = userContext.getTenantId();

        // ✅ Kiểm tra tenant consistency
        if (!file.getTenant().getId().equals(actorTenantId)) {
            throw new ForbiddenException("Actor không cùng tenant với file");
        }

        // ✅ Chỉ file owner, project owner, hoặc người được share mới được xem
        boolean isFileOwner = file.getOwner().getId().equals(actorId);
        boolean isProjectOwner = file.getFolder().getProject().getOwner().getId().equals(actorId);
        boolean isSharedWith = fileShare.getSharedWithUserId().toString().equals(actorId);

        if (!isFileOwner && !isProjectOwner && !isSharedWith) {
            throw new ForbiddenException("Bạn không có quyền xem file share này");
        }

        return mapToResponse(fileShare);
    }

    @Override
    @Scheduled(fixedDelay = 3600000)  // Mỗi 1 giờ
    @Transactional
    public void deleteExpiredShares() {
        List<FileShareEntity> expiredShares = fileShareRepository.findExpiredShares(LocalDateTime.now());
        fileShareRepository.deleteAll(expiredShares);
    }

    // ========== Private Helpers ==========

    private FileEntity findFile(String fileId) {
        return fileRepository.findById(fileId)
                .orElseThrow(() -> ResourceNotFoundException.byField("File", "id", fileId));
    }

    private UserEntity findUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.byField("User", "id", userId));
    }

    private FileShareResponse mapToResponse(FileShareEntity entity) {
        return new FileShareResponse(
                entity.getId(),
                entity.getFileId(),
                entity.getSharedWithUserId(),
                entity.getSharedByUserId(),
                entity.getPermission(),
                entity.getExpiresAt(),
                entity.getCreatedAt()
        );
    }
}
