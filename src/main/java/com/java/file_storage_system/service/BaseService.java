package com.java.file_storage_system.service;

import java.util.List;
import java.util.Optional;

public interface BaseService<T> {
    List<T> findAll();

    Optional<T> findById(String id);

    T save(T item);

    void deleteById(String id);
}
