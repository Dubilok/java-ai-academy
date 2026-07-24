-- Align user_progress.status default with the ProgressStatus enum (was 'NOT_STARTED', unused)
ALTER TABLE user_progress ALTER COLUMN status SET DEFAULT 'IN_PROGRESS';

-- Enforce valid enum values at the database level so a bug in application code cannot persist
-- an unknown status string that would later cause a deserialization error.
ALTER TABLE user_progress
    ADD CONSTRAINT chk_user_progress_status CHECK (status IN ('IN_PROGRESS', 'PASSED'));

ALTER TABLE submissions
    ADD CONSTRAINT chk_submissions_status CHECK (status IN ('PENDING', 'PASSED', 'FAILED'));
