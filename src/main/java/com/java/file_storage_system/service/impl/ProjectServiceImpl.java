package com.java.file_storage_system.service.impl;

import com.java.file_storage_system.entity.ProjectEntity;
import com.java.file_storage_system.repository.ProjectRepository;
import com.java.file_storage_system.service.ProjectService;
import org.springframework.stereotype.Service;

@Service
public class ProjectServiceImpl extends BaseServiceImpl<ProjectEntity, ProjectRepository> implements ProjectService {

    public ProjectServiceImpl(ProjectRepository repository) {
        super(repository);
    }
}
