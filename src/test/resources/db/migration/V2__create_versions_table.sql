DROP TABLE IF EXISTS versions;

CREATE TABLE versions (
    id              SERIAL    PRIMARY KEY,
    candidate       TEXT      NOT NULL,
    version         TEXT      NOT NULL,
    distribution    TEXT          NULL,
    platform        TEXT      NOT NULL,
    visible         BOOLEAN   NOT NULL,
    url             TEXT      NOT NULL,
    md5_sum         TEXT          NULL,
    sha_256_sum     TEXT          NULL,
    sha_512_sum     TEXT          NULL,
    created_at      TIMESTAMP     NULL DEFAULT now(),
    last_updated_at TIMESTAMP     NULL DEFAULT now(),
    UNIQUE NULLS NOT DISTINCT (candidate, version, distribution, platform)
);
