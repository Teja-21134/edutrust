CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE documents (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    department VARCHAR(255),
    doc_type VARCHAR(255),
    academic_year VARCHAR(50),
    version VARCHAR(50),
    doc_date DATE,
    authority VARCHAR(255),
    file_name VARCHAR(255),
    uploaded_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE chunks (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    page_number INT NOT NULL,
    chunk_index INT NOT NULL,
    text TEXT NOT NULL,
    embedding vector(384),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_chunks_document_id ON chunks(document_id);
