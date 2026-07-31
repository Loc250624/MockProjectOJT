CREATE TABLE Ai_Chat_Conversations (
    id VARCHAR(36) NOT NULL,
    user_id INT NOT NULL,
    title VARCHAR(160) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    last_message_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_ai_chat_conversation_user
        FOREIGN KEY (user_id) REFERENCES Users(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_ai_chat_conversation_user_last
    ON Ai_Chat_Conversations (user_id, last_message_at);

CREATE TABLE Ai_Chat_Messages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    conversation_id VARCHAR(36) NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    refused BOOLEAN NOT NULL DEFAULT FALSE,
    reason_code VARCHAR(64) NULL,
    request_id VARCHAR(128) NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_ai_chat_message_conversation
        FOREIGN KEY (conversation_id) REFERENCES Ai_Chat_Conversations(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_ai_chat_message_conversation_order
    ON Ai_Chat_Messages (conversation_id, created_at, id);

CREATE TABLE Ai_Chat_Suggestions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    conversation_id VARCHAR(36) NOT NULL,
    assistant_message_id BIGINT NOT NULL,
    normalized_text VARCHAR(500) NOT NULL,
    display_text VARCHAR(500) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_ai_chat_suggestion_conversation
        FOREIGN KEY (conversation_id) REFERENCES Ai_Chat_Conversations(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_ai_chat_suggestion_message
        FOREIGN KEY (assistant_message_id) REFERENCES Ai_Chat_Messages(id)
        ON DELETE CASCADE,
    CONSTRAINT uk_ai_chat_suggestion_fingerprint
        UNIQUE (conversation_id, normalized_text)
);

CREATE INDEX idx_ai_chat_suggestion_message
    ON Ai_Chat_Suggestions (assistant_message_id, id);
