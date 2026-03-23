package com.java.file_storage_system.service.impl;

import com.java.file_storage_system.entity.UserEntity;
import com.java.file_storage_system.repository.UserRepository;
import com.java.file_storage_system.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends BaseServiceImpl<UserEntity, UserRepository> implements UserService {

    public UserServiceImpl(UserRepository repository) {
        super(repository);
    }
}
