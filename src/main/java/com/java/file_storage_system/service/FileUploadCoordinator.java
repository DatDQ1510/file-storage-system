package com.java.file_storage_system.service;

import com.java.file_storage_system.dto.chunk.ChunkPreSignBatchRequest;
import com.java.file_storage_system.dto.chunk.ChunkPreSignBatchResponse;
import com.java.file_storage_system.dto.chunk.FileUploadFinalizeRequest;
import com.java.file_storage_system.dto.chunk.FileUploadFinalizeResponse;

public interface FileUploadCoordinator {

    ChunkPreSignBatchResponse preSignBatch(ChunkPreSignBatchRequest request);

    FileUploadFinalizeResponse finalizeFileUpload(FileUploadFinalizeRequest request);
}
