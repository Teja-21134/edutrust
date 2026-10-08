CREATE TABLE conversations (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_conversations_user_updated
    ON conversations(user_id, updated_at DESC);

CREATE TABLE messages (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL CHECK (role IN ('USER', 'ASSISTANT')),
    content TEXT NOT NULL,
    sources JSONB,
    faithfulness_status VARCHAR(30),
    faithfulness_score DOUBLE PRECISION,
    conflict_detected BOOLEAN,
    conflict_resolution VARCHAR(100),
    selected_document VARCHAR(255),
    selected_version VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_messages_conversation_created
    ON messages(conversation_id, created_at);
