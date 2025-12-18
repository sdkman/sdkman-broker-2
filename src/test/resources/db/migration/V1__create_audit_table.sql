DROP TABLE IF EXISTS audit;

CREATE TABLE audit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    command TEXT NOT NULL,
    candidate TEXT NOT NULL,
    version TEXT NOT NULL,
    client_platform TEXT NOT NULL,
    candidate_platform TEXT NOT NULL,
    distribution TEXT NULL,
    host TEXT NULL,
    agent TEXT NULL,
    timestamp TIMESTAMP NOT NULL
);
