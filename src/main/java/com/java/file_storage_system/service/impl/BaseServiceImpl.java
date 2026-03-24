package com.java.file_storage_system.service.impl;

import com.java.file_storage_system.repository.BaseRepository;
import com.java.file_storage_system.service.BaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Transactional
public abstract class BaseServiceImpl<T, R extends BaseRepository<T>> implements BaseService<T> {

    protected R repository;

    protected BaseServiceImpl() {
    }

    protected BaseServiceImpl(R repository) {
        this.repository = repository;
    }

    @Autowired
    protected void setRepository(R repository) {
        if (this.repository == null) {
            this.repository = repository;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> findAll() {
        return repository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<T> findById(String id) {
        return repository.findById(id);
    }

    @Override
    public T save(T item) {
        return repository.save(item);
    }

    @Override
    public void deleteById(String id) {
        repository.deleteById(id);
    }
}
