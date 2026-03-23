package com.java.file_storage_system.service.impl;

import com.java.file_storage_system.entity.FolderEntity;
import com.java.file_storage_system.repository.FolderRepository;
import com.java.file_storage_system.service.FolderService;
import org.springframework.stereotype.Service;

@Service
public class FolderServiceImpl extends BaseServiceImpl<FolderEntity, FolderRepository> implements FolderService {

    public FolderServiceImpl(FolderRepository repository) {
        super(repository);
    }
}
