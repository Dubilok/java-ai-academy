-- Add optional user and course tracking to ai_generation_log for FinOps cost breakdowns.
-- Both columns are nullable because not all agent calls are tied to a specific user or course.
ALTER TABLE ai_generation_log
    ADD COLUMN user_id   UUID REFERENCES users(id)   ON DELETE SET NULL,
    ADD COLUMN course_id UUID REFERENCES courses(id) ON DELETE SET NULL;

CREATE INDEX idx_ai_log_user_id   ON ai_generation_log (user_id)   WHERE user_id   IS NOT NULL;
CREATE INDEX idx_ai_log_course_id ON ai_generation_log (course_id) WHERE course_id IS NOT NULL;
CREATE INDEX idx_ai_log_created_at ON ai_generation_log (created_at);
