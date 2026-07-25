-- Add CHECK constraint and indexes for ai_evaluation quality dashboard
-- target_type matches EvaluationTargetType enum values

ALTER TABLE ai_evaluation
    ADD CONSTRAINT chk_ai_evaluation_target_type
        CHECK (target_type IN ('HINT', 'INTERVIEW_EVALUATION', 'CONTENT_GENERATION'));

CREATE INDEX idx_ai_evaluation_created_at  ON ai_evaluation (created_at DESC);
CREATE INDEX idx_ai_evaluation_target_type ON ai_evaluation (target_type);
