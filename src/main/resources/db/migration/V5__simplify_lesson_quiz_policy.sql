-- Back up Quizzes before applying this migration in production.
-- Rollback: restore duration_minutes/max_attempts from that backup; no attempt
-- or attempt-question history is deleted by this migration.

UPDATE Quizzes
SET duration_minutes = 20
WHERE duration_minutes IS NULL OR duration_minutes <> 20;

ALTER TABLE Quizzes
    MODIFY COLUMN duration_minutes INT NOT NULL DEFAULT 20,
    MODIFY COLUMN max_attempts INT NULL;
