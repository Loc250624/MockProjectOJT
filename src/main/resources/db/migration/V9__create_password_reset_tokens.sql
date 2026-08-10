CREATE TABLE IF NOT EXISTS Password_Reset_Tokens (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    token_hash VARCHAR(128) NOT NULL,
    expires_at DATETIME NOT NULL,
    used_at DATETIME NULL,
    created_at DATETIME NULL,
    CONSTRAINT uk_password_reset_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_password_reset_tokens_user
        FOREIGN KEY (user_id) REFERENCES Users(id)
);

CREATE INDEX idx_password_reset_tokens_user_used
    ON Password_Reset_Tokens (user_id, used_at);
