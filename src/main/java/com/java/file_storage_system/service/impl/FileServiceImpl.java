package com.java.file_storage_system.service.impl;

import com.java.file_storage_system.entity.FileEntity;
import com.java.file_storage_system.repository.FileRepository;
import com.java.file_storage_system.service.FileService;
import org.springframework.stereotype.Service;

@Service
public class FileServiceImpl extends BaseServiceImpl<FileEntity, FileRepository> implements FileService {

    public FileServiceImpl(FileRepository repository) {
        super(repository);
    }
}
