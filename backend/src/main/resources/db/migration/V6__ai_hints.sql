-- AI Hint history: one row per Socratic hint generated for a user+task pair.
-- task_id is SET NULL on task delete to retain hint history for audit.
CREATE TABLE ai_hints (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users(id)  ON DELETE CASCADE,
    task_id     UUID                 REFERENCES tasks(id) ON DELETE SET NULL,
    hint_text   TEXT        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_ai_hints_user_id ON ai_hints (user_id);
CREATE INDEX idx_ai_hints_task_id ON ai_hints (task_id);
