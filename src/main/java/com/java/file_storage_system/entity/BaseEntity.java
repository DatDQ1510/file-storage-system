package com.java.file_storage_system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity {

	@Id
	@Column(name = "id", nullable = false, updatable = false)
	private String id;

	@Column(name= "createdAt" ,updatable = false, nullable = false)
	private LocalDateTime createdAt;

	@Column(name= "updatedAt" ,nullable = false)
	private LocalDateTime updatedAt;

	@PrePersist
	protected void onCreate() {
		if (this.id == null || this.id.isBlank()) {
			this.id = UUID.randomUUID().toString();
		}
		LocalDateTime now = LocalDateTime.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate
	protected void onUpdate() {
		this.updatedAt = LocalDateTime.now();
	}

}