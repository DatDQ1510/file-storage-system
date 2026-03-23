package com.java.file_storage_system.service.impl;

import com.java.file_storage_system.entity.FileVersionEntity;
import com.java.file_storage_system.repository.FileVersionRepository;
import com.java.file_storage_system.service.FileVersionService;
import org.springframework.stereotype.Service;

@Service
public class FileVersionServiceImpl extends BaseServiceImpl<FileVersionEntity, FileVersionRepository> implements FileVersionService {

    public FileVersionServiceImpl(FileVersionRepository repository) {
        super(repository);
    }
}
