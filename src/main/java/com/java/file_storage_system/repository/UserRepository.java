package com.java.file_storage_system.repository;

import com.java.file_storage_system.entity.UserEntity;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends BaseRepository<UserEntity> {
}
