-- Add CHECK constraints to ai_generation_log to enforce enum values at the DB level.
-- agent  maps to AgentType enum; outcome maps to GenerationOutcome enum.

ALTER TABLE ai_generation_log
    ADD CONSTRAINT chk_ai_generation_log_agent
        CHECK (agent IN ('CONTENT_ARCHITECT', 'SOCRATIC_MENTOR', 'MOCK_INTERVIEWER', 'JUDGE')),
    ADD CONSTRAINT chk_ai_generation_log_outcome
        CHECK (outcome IN ('SUCCEEDED', 'EXHAUSTED', 'PARSE_FAILED'));
