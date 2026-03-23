package com.java.file_storage_system.service.impl;

import com.java.file_storage_system.entity.ChunkEntity;
import com.java.file_storage_system.repository.ChunkRepository;
import com.java.file_storage_system.service.ChunkService;
import org.springframework.stereotype.Service;

@Service
public class ChunkServiceImpl extends BaseServiceImpl<ChunkEntity, ChunkRepository> implements ChunkService {

    public ChunkServiceImpl(ChunkRepository repository) {
        super(repository);
    }
}
