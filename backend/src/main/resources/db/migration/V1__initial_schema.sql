-- Full initial schema: users, course catalog hierarchy, progress tracking,
-- submissions, interview system, and AI telemetry tables.
-- Flyway CE has no rollback; compensating migrations will be added as needed.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ============================================================
-- users
-- ============================================================
CREATE TABLE users (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(50)  NOT NULL DEFAULT 'ROLE_STUDENT',
    xp_points     BIGINT       NOT NULL DEFAULT 0,
    crystals      BIGINT       NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_users_email UNIQUE (email)
);

-- ============================================================
-- courses
-- ============================================================
CREATE TABLE courses (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    title        VARCHAR(255) NOT NULL,
    description  TEXT,
    technology   VARCHAR(100) NOT NULL,
    is_published BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_courses_technology ON courses (technology);

-- ============================================================
-- modules
-- ============================================================
CREATE TABLE modules (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id   UUID         NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    title       VARCHAR(255) NOT NULL,
    order_index INTEGER      NOT NULL,
    CONSTRAINT uq_modules_course_order UNIQUE (course_id, order_index)
);
CREATE INDEX idx_modules_course_id ON modules (course_id);

-- ============================================================
-- lectures
-- ============================================================
CREATE TABLE lectures (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    module_id        UUID         NOT NULL REFERENCES modules (id) ON DELETE CASCADE,
    title            VARCHAR(255) NOT NULL,
    content_markdown TEXT,
    order_index      INTEGER      NOT NULL,
    CONSTRAINT uq_lectures_module_order UNIQUE (module_id, order_index)
);
CREATE INDEX idx_lectures_module_id ON lectures (module_id);

-- ============================================================
-- tasks  (test_code and solution_code are never sent to student clients)
-- ============================================================
CREATE TABLE tasks (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    lecture_id    UUID         NOT NULL REFERENCES lectures (id) ON DELETE CASCADE,
    title         VARCHAR(255) NOT NULL,
    description   TEXT,
    difficulty    VARCHAR(50)  NOT NULL DEFAULT 'BEGINNER',
    template_code TEXT,
    test_code     TEXT,
    solution_code TEXT,
    xp_reward     BIGINT       NOT NULL DEFAULT 0
);
CREATE INDEX idx_tasks_lecture_id ON tasks (lecture_id);

-- ============================================================
-- user_progress  (UNIQUE (user_id, task_id) enforces idempotent XP award)
-- ============================================================
CREATE TABLE user_progress (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    task_id        UUID        NOT NULL REFERENCES tasks (id) ON DELETE CASCADE,
    status         VARCHAR(50) NOT NULL DEFAULT 'NOT_STARTED',
    attempts       INTEGER     NOT NULL DEFAULT 0,
    submitted_code TEXT,
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_progress_user_task UNIQUE (user_id, task_id)
);
CREATE INDEX idx_user_progress_user_id ON user_progress (user_id);
CREATE INDEX idx_user_progress_task_id ON user_progress (task_id);

-- ============================================================
-- submissions  (append-only audit trail; task_id nullable so rows survive task deletion)
-- ============================================================
CREATE TABLE submissions (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    task_id     UUID        REFERENCES tasks (id) ON DELETE SET NULL,
    source      TEXT        NOT NULL,
    status      VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    logs        TEXT,
    duration_ms BIGINT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_submissions_user_id ON submissions (user_id);
CREATE INDEX idx_submissions_task_id ON submissions (task_id);
CREATE INDEX idx_submissions_status  ON submissions (status);

-- ============================================================
-- interview_questions
-- ============================================================
CREATE TABLE interview_questions (
    id                   UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    technology           VARCHAR(100) NOT NULL,
    category             VARCHAR(100) NOT NULL,
    question             TEXT         NOT NULL,
    short_answer         TEXT,
    detailed_explanation TEXT,
    difficulty           VARCHAR(50)  NOT NULL DEFAULT 'INTERMEDIATE'
);
CREATE INDEX idx_interview_questions_technology ON interview_questions (technology);
CREATE INDEX idx_interview_questions_category   ON interview_questions (category);
CREATE INDEX idx_interview_questions_difficulty ON interview_questions (difficulty);

-- ============================================================
-- interview_sessions
-- ============================================================
CREATE TABLE interview_sessions (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    technology  VARCHAR(100) NOT NULL,
    score       INTEGER,
    report_json TEXT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_interview_sessions_user_id ON interview_sessions (user_id);

-- ============================================================
-- interview_answers
-- ============================================================
CREATE TABLE interview_answers (
    id          UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id  UUID    NOT NULL REFERENCES interview_sessions (id) ON DELETE CASCADE,
    question_id UUID    NOT NULL REFERENCES interview_questions (id) ON DELETE RESTRICT,
    answer_text TEXT,
    score       INTEGER
);
CREATE INDEX idx_interview_answers_session_id  ON interview_answers (session_id);
CREATE INDEX idx_interview_answers_question_id ON interview_answers (question_id);

-- ============================================================
-- ai_generation_log  (FinOps + Judge dashboard source)
-- ============================================================
CREATE TABLE ai_generation_log (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    agent             VARCHAR(100) NOT NULL,
    model             VARCHAR(100) NOT NULL,
    prompt_tokens     INTEGER,
    completion_tokens INTEGER,
    cost_usd          NUMERIC(12, 6),
    latency_ms        BIGINT,
    outcome           VARCHAR(50)  NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ============================================================
-- ai_evaluation  (LLM-as-a-Judge output; never in the request path)
-- ============================================================
CREATE TABLE ai_evaluation (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    target_type       VARCHAR(100) NOT NULL,
    target_id         UUID,
    judge_scores_json TEXT,
    flags             TEXT,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
