package com.java.file_storage_system.repository;

import com.java.file_storage_system.entity.UserProjectEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserProjectRepository extends BaseRepository<UserProjectEntity> {

    @Query("select (count(up) > 0) from UserProjectEntity up where up.user.id = :userId and up.project.id = :projectId")
    boolean existsByUserIdAndProjectId(@Param("userId") String userId, @Param("projectId") String projectId);

    @Query("select up from UserProjectEntity up where up.user.id = :userId and up.project.id = :projectId")
    Optional<UserProjectEntity> findByUserIdAndProjectId(@Param("userId") String userId, @Param("projectId") String projectId);

    @Query("select up from UserProjectEntity up where up.project.id = :projectId")
    List<UserProjectEntity> findAllByProjectId(@Param("projectId") String projectId);
}
