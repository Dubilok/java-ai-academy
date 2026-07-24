ALTER TABLE interview_questions
    ALTER COLUMN difficulty SET DEFAULT 'INTERMEDIATE',
    ADD CONSTRAINT chk_interview_questions_difficulty
        CHECK (difficulty IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'EXPERT'));
