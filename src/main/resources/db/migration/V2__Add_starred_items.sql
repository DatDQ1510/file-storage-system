-- Add user-specific starred tables for files and folders.

CREATE TABLE IF NOT EXISTS file_stars (
    id varchar(255) PRIMARY KEY,
    createdAt timestamp NOT NULL,
    updatedAt timestamp NOT NULL,
    fileId varchar(255) NOT NULL,
    userId varchar(255) NOT NULL,
    tenantId varchar(255) NOT NULL,
    CONSTRAINT uk_file_star_file_user UNIQUE (fileId, userId),
    CONSTRAINT fk_file_star_file FOREIGN KEY (fileId) REFERENCES files (id) ON DELETE CASCADE,
    CONSTRAINT fk_file_star_user FOREIGN KEY (userId) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_file_star_tenant FOREIGN KEY (tenantId) REFERENCES tenants (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_file_star_file_id ON file_stars (fileId);
CREATE INDEX IF NOT EXISTS idx_file_star_user_id ON file_stars (userId);
CREATE INDEX IF NOT EXISTS idx_file_star_tenant_id ON file_stars (tenantId);

CREATE TABLE IF NOT EXISTS folder_stars (
    id varchar(255) PRIMARY KEY,
    createdAt timestamp NOT NULL,
    updatedAt timestamp NOT NULL,
    folderId varchar(255) NOT NULL,
    userId varchar(255) NOT NULL,
    tenantId varchar(255) NOT NULL,
    CONSTRAINT uk_folder_star_folder_user UNIQUE (folderId, userId),
    CONSTRAINT fk_folder_star_folder FOREIGN KEY (folderId) REFERENCES folders (id) ON DELETE CASCADE,
    CONSTRAINT fk_folder_star_user FOREIGN KEY (userId) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_folder_star_tenant FOREIGN KEY (tenantId) REFERENCES tenants (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_folder_star_folder_id ON folder_stars (folderId);
CREATE INDEX IF NOT EXISTS idx_folder_star_user_id ON folder_stars (userId);
CREATE INDEX IF NOT EXISTS idx_folder_star_tenant_id ON folder_stars (tenantId);