package com.java.file_storage_system.service;

import com.java.file_storage_system.dto.starred.StarredFileResponse;
import com.java.file_storage_system.dto.starred.StarredFolderResponse;
import com.java.file_storage_system.dto.starred.StarredPageResponse;

public interface StarService {

    StarredPageResponse getStarredPage(String userId, String tenantId);

    StarredFileResponse starFile(String fileId, String userId, String tenantId);

    void unstarFile(String fileId, String userId);

    StarredFolderResponse starFolder(String folderId, String userId, String tenantId);

    void unstarFolder(String folderId, String userId);
}