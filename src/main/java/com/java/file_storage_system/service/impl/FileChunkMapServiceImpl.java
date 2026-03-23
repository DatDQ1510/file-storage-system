package com.java.file_storage_system.service.impl;

import com.java.file_storage_system.entity.FileChunkMapEntity;
import com.java.file_storage_system.repository.FileChunkMapRepository;
import com.java.file_storage_system.service.FileChunkMapService;
import org.springframework.stereotype.Service;

@Service
public class FileChunkMapServiceImpl extends BaseServiceImpl<FileChunkMapEntity, FileChunkMapRepository> implements FileChunkMapService {

    public FileChunkMapServiceImpl(FileChunkMapRepository repository) {
        super(repository);
    }
}
