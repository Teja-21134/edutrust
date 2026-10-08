ALTER TABLE chunks
    ADD COLUMN search_vector tsvector
    GENERATED ALWAYS AS (to_tsvector('english'::regconfig, coalesce(text, ''))) STORED;

CREATE INDEX idx_chunks_search_vector
    ON chunks USING GIN (search_vector);
