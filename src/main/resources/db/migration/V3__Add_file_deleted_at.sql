ALTER TABLE files
ADD COLUMN IF NOT EXISTS deletedAt TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_files_deleted_at
ON files(deletedAt);
