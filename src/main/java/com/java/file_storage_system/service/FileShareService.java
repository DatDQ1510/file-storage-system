package com.java.file_storage_system.service;

import com.java.file_storage_system.dto.file.FileShareResponse;
import com.java.file_storage_system.dto.file.ShareFileRequest;
import com.java.file_storage_system.exception.ConflictException;
import com.java.file_storage_system.exception.ForbiddenException;
import com.java.file_storage_system.exception.ResourceNotFoundException;

import java.util.List;

/**
 * Service interface cho file sharing functionality
 */
public interface FileShareService {

    /**
     * Share file với user khác
     *
     * @param fileId File cần share
     * @param request Thông tin share (sharedWithUserId, permission, expiresAt)
     * @return FileShareResponse
     * @throws ResourceNotFoundException nếu file/user không tìm thấy
     * @throws ForbiddenException nếu user không có quyền share file
     * @throws ConflictException nếu file đã share cho user này
     */
    FileShareResponse shareFile(String fileId, ShareFileRequest request);

    /**
     * Revoke file share (xoá share)
     * 
     * @param fileId File ID
     * @param sharedWithUserId User ID muốn xoá share
     * @throws ResourceNotFoundException nếu file/share không tìm thấy
     * @throws ForbiddenException nếu user không có quyền revoke
     */
    void unshareFile(String fileId, String sharedWithUserId);

    /**
     * Update share permission
     * 
     * @param fileId File ID
     * @param sharedWithUserId User ID
     * @param request Thông tin update (permission, expiresAt)
     * @return FileShareResponse updated
     * @throws ResourceNotFoundException nếu share không tìm thấy
     * @throws ForbiddenException nếu user không có quyền update
     */
    FileShareResponse updateFileShare(String fileId, String sharedWithUserId, ShareFileRequest request);

    /**
     * Lấy tất cả file shares của file (ai được share file này)
     * 
     * @param fileId File ID
     * @return List FileShareResponse
     * @throws ResourceNotFoundException nếu file không tìm thấy
     * @throws ForbiddenException nếu user không có quyền xem
     */
    List<FileShareResponse> getFileShares(String fileId);

    /**
     * Lấy file share details
     * 
     * @param fileId File ID
     * @param sharedWithUserId User ID
     * @return FileShareResponse
     * @throws ResourceNotFoundException nếu share không tìm thấy
     */
    FileShareResponse getFileShare(String fileId, String sharedWithUserId);

    /**
     * Xoá các file shares đã expire (scheduled task)
     */
    void deleteExpiredShares();
}
