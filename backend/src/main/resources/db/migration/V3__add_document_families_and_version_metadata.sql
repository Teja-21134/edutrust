CREATE TABLE document_families (
    id UUID PRIMARY KEY,
    institution_key VARCHAR(100) NOT NULL,
    family_key VARCHAR(150) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_document_families_institution_family
        UNIQUE (institution_key, family_key)
);

ALTER TABLE documents
    ADD COLUMN family_id UUID NULL REFERENCES document_families(id),
    ADD COLUMN effective_date DATE NULL,
    ADD COLUMN document_hash VARCHAR(64) NULL;

CREATE INDEX idx_documents_family_id ON documents(family_id);
CREATE INDEX idx_documents_family_doc_date ON documents(family_id, doc_date);
CREATE INDEX idx_documents_family_effective_date ON documents(family_id, effective_date);
