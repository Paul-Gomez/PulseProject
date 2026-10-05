-- Trigram index: lets "ILIKE '%docker%'" use an index, so partial words ("dock") find "Docker".
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_messages_content_trgm ON messages USING gin (content gin_trgm_ops);
CREATE INDEX idx_messages_author_id ON messages (author_id);
