/* TODO: All VARCHAR fields should be TEXT */
CREATE TABLE audit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    command VARCHAR(50) NOT NULL,
    candidate VARCHAR(100) NOT NULL,
    version VARCHAR(100) NOT NULL,
    host VARCHAR(45) NOT NULL,
    agent VARCHAR(200) NOT NULL,
    platform VARCHAR(50) NOT NULL,
    dist VARCHAR(50) NOT NULL,
    timestamp BIGINT NOT NULL
);
