-- Additive migration for canonical, snapshot-based quiz attempts.
-- Legacy quiz rows in Submissions are intentionally retained for history.

ALTER TABLE Questions
    ADD COLUMN topic_code VARCHAR(100) NOT NULL DEFAULT 'GENERAL',
    ADD COLUMN difficulty VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    ADD COLUMN review_status VARCHAR(20) NOT NULL DEFAULT 'APPROVED',
    ADD COLUMN version INT NOT NULL DEFAULT 1,
    ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN generation_source VARCHAR(20) NOT NULL DEFAULT 'MANUAL';

UPDATE Questions
SET topic_code = COALESCE(NULLIF(TRIM(topic_code), ''), 'GENERAL'),
    difficulty = CASE
        WHEN version IS NULL OR version <= 0 THEN 'MEDIUM'
        ELSE COALESCE(NULLIF(TRIM(difficulty), ''), 'MEDIUM')
    END,
    review_status = COALESCE(NULLIF(TRIM(review_status), ''), 'APPROVED'),
    active = CASE
        WHEN (version IS NULL OR version <= 0)
             AND (topic_code IS NULL OR TRIM(topic_code) = '') THEN TRUE
        ELSE COALESCE(active, TRUE)
    END,
    generation_source = CASE
        WHEN version IS NULL OR version <= 0 THEN 'MANUAL'
        ELSE COALESCE(NULLIF(TRIM(generation_source), ''), 'MANUAL')
    END,
    version = CASE WHEN version IS NULL OR version <= 0 THEN 1 ELSE version END;

UPDATE Questions
SET question_text = REGEXP_REPLACE(
        question_text,
        '^[[:space:]]*Checkpoint[[:space:]]+[0-9]+[[:space:]]*:[[:space:]]*',
        '')
WHERE question_text REGEXP
      '^[[:space:]]*Checkpoint[[:space:]]+[0-9]+[[:space:]]*:';

CREATE INDEX idx_questions_quiz_bank
    ON Questions (quiz_id, review_status, active, topic_code, difficulty);

CREATE TABLE Quiz_Blueprint_Items (
    id INT NOT NULL AUTO_INCREMENT,
    quiz_id INT NOT NULL,
    topic_code VARCHAR(100) NOT NULL,
    difficulty VARCHAR(20) NOT NULL,
    question_count INT NOT NULL,
    display_order INT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_quiz_blueprint_quiz
        FOREIGN KEY (quiz_id) REFERENCES Quizzes(id),
    CONSTRAINT uq_quiz_blueprint_bucket
        UNIQUE (quiz_id, topic_code, difficulty),
    CONSTRAINT chk_quiz_blueprint_count
        CHECK (question_count > 0)
);

CREATE INDEX idx_quiz_blueprint_order
    ON Quiz_Blueprint_Items (quiz_id, display_order, id);

-- Compatibility default: preserve existing quizzes without manufacturing
-- questions. The runtime caps an attempt at ten questions.
INSERT INTO Quiz_Blueprint_Items
    (quiz_id, topic_code, difficulty, question_count, display_order)
SELECT q.id, 'GENERAL', 'MEDIUM', LEAST(COUNT(qu.id), 10), 1
FROM Quizzes q
JOIN Questions qu ON qu.quiz_id = q.id
WHERE qu.review_status = 'APPROVED' AND qu.active = TRUE
GROUP BY q.id
HAVING COUNT(qu.id) > 0;

CREATE TABLE Quiz_Attempt_Questions (
    id INT NOT NULL AUTO_INCREMENT,
    attempt_id INT NOT NULL,
    question_id INT NOT NULL,
    display_order INT NOT NULL,
    points_snapshot DECIMAL(10,2) NOT NULL,
    question_version INT NOT NULL,
    question_text_snapshot TEXT NOT NULL,
    options_json_snapshot LONGTEXT NULL,
    correct_answer_snapshot TEXT NULL,
    question_type_snapshot VARCHAR(50) NULL,
    topic_snapshot VARCHAR(100) NULL,
    difficulty_snapshot VARCHAR(20) NULL,
    assigned_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_attempt_question_attempt
        FOREIGN KEY (attempt_id) REFERENCES Quiz_Attempts(id),
    CONSTRAINT fk_attempt_question_question
        FOREIGN KEY (question_id) REFERENCES Questions(id),
    CONSTRAINT uq_attempt_question
        UNIQUE (attempt_id, question_id),
    CONSTRAINT uq_attempt_display_order
        UNIQUE (attempt_id, display_order)
);

CREATE INDEX idx_attempt_question_order
    ON Quiz_Attempt_Questions (attempt_id, display_order);

CREATE INDEX idx_attempt_question_question
    ON Quiz_Attempt_Questions (question_id);

ALTER TABLE Quiz_Answers
    ADD CONSTRAINT uq_quiz_answer_attempt_question
    UNIQUE (attempt_id, question_id);

CREATE TABLE Question_Generation_Jobs (
    id INT NOT NULL AUTO_INCREMENT,
    quiz_id INT NOT NULL,
    requested_by INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    requested_count INT NOT NULL,
    generated_count INT NOT NULL DEFAULT 0,
    topic_code VARCHAR(100) NOT NULL,
    difficulty VARCHAR(20) NOT NULL,
    lesson_content_snapshot LONGTEXT NOT NULL,
    provider_name VARCHAR(50) NULL,
    model_name VARCHAR(100) NULL,
    error_message TEXT NULL,
    started_at DATETIME(6) NULL,
    completed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_question_generation_quiz
        FOREIGN KEY (quiz_id) REFERENCES Quizzes(id),
    CONSTRAINT fk_question_generation_requester
        FOREIGN KEY (requested_by) REFERENCES Users(id)
);

CREATE INDEX idx_question_generation_quiz_status
    ON Question_Generation_Jobs (quiz_id, status, created_at);
