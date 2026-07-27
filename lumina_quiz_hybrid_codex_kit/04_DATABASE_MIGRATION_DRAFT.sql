-- DRAFT: Codex must verify actual table names, column names and existing constraints.
-- Create a NEW Flyway migration, for example:
-- V2__hybrid_quiz_question_bank.sql
-- Do not edit an already applied migration.

-- ============================================================
-- 1. Question bank metadata
-- ============================================================

ALTER TABLE Questions
    ADD COLUMN topic_code VARCHAR(100) NULL,
    ADD COLUMN difficulty VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    ADD COLUMN review_status VARCHAR(20) NOT NULL DEFAULT 'APPROVED',
    ADD COLUMN question_version INT NOT NULL DEFAULT 1,
    ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN generation_source VARCHAR(20) NOT NULL DEFAULT 'MANUAL';

UPDATE Questions
SET topic_code = COALESCE(NULLIF(TRIM(topic_code), ''), 'GENERAL'),
    difficulty = COALESCE(NULLIF(TRIM(difficulty), ''), 'MEDIUM'),
    review_status = COALESCE(NULLIF(TRIM(review_status), ''), 'APPROVED'),
    question_version = COALESCE(question_version, 1),
    is_active = COALESCE(is_active, TRUE),
    generation_source = COALESCE(NULLIF(TRIM(generation_source), ''), 'MANUAL');

CREATE INDEX idx_questions_quiz_bank
    ON Questions (quiz_id, review_status, is_active, topic_code, difficulty);

-- ============================================================
-- 2. Quiz sampling settings
-- ============================================================

ALTER TABLE Quizzes
    ADD COLUMN questions_per_attempt INT NULL,
    ADD COLUMN max_overlap_percent INT NOT NULL DEFAULT 30,
    ADD COLUMN blueprint_version INT NOT NULL DEFAULT 1;

UPDATE Quizzes q
SET q.questions_per_attempt = (
    SELECT COUNT(*)
    FROM Questions qu
    WHERE qu.quiz_id = q.id
)
WHERE q.questions_per_attempt IS NULL;

-- ============================================================
-- 3. Blueprint
-- ============================================================

CREATE TABLE Quiz_Blueprint_Items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    quiz_id INT NOT NULL,
    topic_code VARCHAR(100) NOT NULL,
    difficulty VARCHAR(20) NOT NULL,
    question_count INT NOT NULL,
    display_order INT NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_quiz_blueprint_quiz
        FOREIGN KEY (quiz_id) REFERENCES Quizzes(id),

    CONSTRAINT uq_quiz_blueprint_bucket
        UNIQUE (quiz_id, topic_code, difficulty),

    CONSTRAINT chk_quiz_blueprint_count
        CHECK (question_count > 0)
);

CREATE INDEX idx_quiz_blueprint_order
    ON Quiz_Blueprint_Items (quiz_id, display_order, id);

-- Compatibility blueprint: existing quizzes initially expose their current
-- question count as GENERAL + MEDIUM. Teacher can refine it later.
INSERT INTO Quiz_Blueprint_Items
    (quiz_id, topic_code, difficulty, question_count, display_order)
SELECT q.id, 'GENERAL', 'MEDIUM', COUNT(qu.id), 1
FROM Quizzes q
JOIN Questions qu ON qu.quiz_id = q.id
GROUP BY q.id
HAVING COUNT(qu.id) > 0;

-- ============================================================
-- 4. Fixed question set per attempt
-- ============================================================

CREATE TABLE Quiz_Attempt_Questions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    attempt_id INT NOT NULL,
    question_id INT NOT NULL,
    display_order INT NOT NULL,
    points_snapshot DECIMAL(10,2) NOT NULL DEFAULT 0,
    question_version INT NOT NULL DEFAULT 1,
    question_text_snapshot TEXT NOT NULL,
    options_json_snapshot LONGTEXT NULL,
    correct_answer_snapshot TEXT NULL,
    question_type_snapshot VARCHAR(50) NULL,
    topic_snapshot VARCHAR(100) NULL,
    difficulty_snapshot VARCHAR(20) NULL,
    assigned_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_attempt_question_attempt
        FOREIGN KEY (attempt_id) REFERENCES Quiz_Attempts(id),

    CONSTRAINT fk_attempt_question_question
        FOREIGN KEY (question_id) REFERENCES Questions(id),

    CONSTRAINT uq_attempt_question
        UNIQUE (attempt_id, question_id),

    CONSTRAINT uq_attempt_display_order
        UNIQUE (attempt_id, display_order)
);

CREATE INDEX idx_attempt_questions_attempt_order
    ON Quiz_Attempt_Questions (attempt_id, display_order);

CREATE INDEX idx_attempt_questions_question
    ON Quiz_Attempt_Questions (question_id);

-- ============================================================
-- 5. Answer integrity
-- ============================================================

-- Inspect duplicates before adding this constraint:
--
-- SELECT attempt_id, question_id, COUNT(*)
-- FROM Quiz_Answers
-- WHERE attempt_id IS NOT NULL
-- GROUP BY attempt_id, question_id
-- HAVING COUNT(*) > 1;

ALTER TABLE Quiz_Answers
    ADD CONSTRAINT uq_quiz_answer_attempt_question
    UNIQUE (attempt_id, question_id);

-- ============================================================
-- 6. Important compatibility rule
-- ============================================================

-- Do NOT delete old quiz rows from Submissions in this migration.
-- Do NOT drop legacy columns/tables in the same release.
