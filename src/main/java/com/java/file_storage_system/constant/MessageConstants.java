package com.java.file_storage_system.constant;

/**
 * Centralized error and informational messages for the application.
 * Supports consistent messaging across services and enables future i18n support.
 */
public class MessageConstants {

    // Project Messages
    public static final String PROJECT_NOT_FOUND = "Project not found";
    public static final String PROJECT_ALREADY_EXISTS = "Project name already exists in this tenant";
    public static final String PROJECT_OWNER_NOT_MEMBER = "Owner is not a member of the tenant";
    public static final String PROJECT_USER_ALREADY_MEMBER = "User is already a member of the project";

    // Folder Messages
    public static final String FOLDER_NOT_FOUND = "Folder not found";
    public static final String FOLDER_OWNER_NOT_IN_TENANT = "Owner does not belong to the same tenant";
    public static final String FOLDER_CANNOT_BE_OWN_PARENT = "Folder cannot be its own parent";
    public static final String FOLDER_PROJECT_MISMATCH = "Parent folder does not belong to the same project";
    public static final String FOLDER_TENANT_MISMATCH = "Parent folder does not belong to the same tenant";

    // Tenant Messages
    public static final String TENANT_NOT_FOUND = "Tenant not found";
    public static final String TENANT_PROJECT_MISMATCH = "Project does not belong to the selected tenant";
    public static final String TENANT_OWNER_MISMATCH = "Owner does not belong to the selected tenant";

    // User Messages
    public static final String USER_NOT_FOUND = "User not found";
    public static final String USER_CAPACITY_EXCEEDED = "Tenant capacity exceeded";

    // File Messages
    public static final String FILE_NOT_FOUND = "File not found";

    // Version Messages
    public static final String VERSION_NOT_FOUND = "Version not found";

    // Chunk Messages
    public static final String CHUNK_NOT_FOUND = "Chunk not found";

    // Generic Messages
    public static final String RESOURCE_NOT_FOUND = "Resource not found";
    public static final String FIELD_REQUIRED = "%s is required";
    public static final String INVALID_PAGINATION = "Invalid pagination parameters";

    private MessageConstants() {
        // Private constructor to prevent instantiation
    }
}
