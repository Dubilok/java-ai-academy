ALTER TABLE interview_sessions
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN current_question_id UUID REFERENCES interview_questions (id) ON DELETE SET NULL,
    ADD CONSTRAINT chk_interview_sessions_status
        CHECK (status IN ('ACTIVE', 'FINISHED'));

CREATE INDEX idx_interview_sessions_current_question ON interview_sessions (current_question_id);
