CREATE TABLE IF NOT EXISTS Student_Feedback (
    id INT NOT NULL AUTO_INCREMENT,
    student_id INT NOT NULL,
    category VARCHAR(40) NOT NULL,
    subject VARCHAR(150) NOT NULL,
    content TEXT NOT NULL,
    course_content_rating TINYINT NOT NULL,
    instructor_support_rating TINYINT NOT NULL,
    learning_experience_rating TINYINT NOT NULL,
    platform_usability_rating TINYINT NOT NULL,
    assessment_experience_rating TINYINT NOT NULL,
    overall_satisfaction_rating TINYINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_student_feedback_student
        FOREIGN KEY (student_id)
        REFERENCES Users (id),
    CONSTRAINT chk_student_feedback_course_content_rating CHECK (course_content_rating BETWEEN 1 AND 5),
    CONSTRAINT chk_student_feedback_instructor_support_rating CHECK (instructor_support_rating BETWEEN 1 AND 5),
    CONSTRAINT chk_student_feedback_learning_experience_rating CHECK (learning_experience_rating BETWEEN 1 AND 5),
    CONSTRAINT chk_student_feedback_platform_usability_rating CHECK (platform_usability_rating BETWEEN 1 AND 5),
    CONSTRAINT chk_student_feedback_assessment_experience_rating CHECK (assessment_experience_rating BETWEEN 1 AND 5),
    CONSTRAINT chk_student_feedback_overall_satisfaction_rating CHECK (overall_satisfaction_rating BETWEEN 1 AND 5),
    INDEX idx_student_feedback_student_created (student_id, created_at),
    INDEX idx_student_feedback_created (created_at),
    INDEX idx_student_feedback_category (category)
);
