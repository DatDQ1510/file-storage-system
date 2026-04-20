-- Migration: Add status column to projects table and set default for existing rows
-- This migration handles the addition of the ProjectStatus field to existing projects

-- Update existing projects with NULL status to ACTIVE
UPDATE projects 
SET status = 'ACTIVE' 
WHERE status IS NULL;

-- Add NOT NULL constraint if not already present (Hibernate will handle this via columnDefinition)
-- ALTER TABLE projects ALTER COLUMN status SET NOT NULL;
