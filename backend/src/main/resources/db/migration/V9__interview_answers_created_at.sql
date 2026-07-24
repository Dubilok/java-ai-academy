-- Add created_at to interview_answers so scores can be fetched in chronological order
-- for the adaptive difficulty rolling-window calculation.
ALTER TABLE interview_answers
    ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT now();

CREATE INDEX idx_interview_answers_created_at ON interview_answers (session_id, created_at);
