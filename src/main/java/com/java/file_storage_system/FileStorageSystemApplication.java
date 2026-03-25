package com.java.file_storage_system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FileStorageSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(FileStorageSystemApplication.class, args);
	}

}
