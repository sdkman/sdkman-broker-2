DROP TABLE IF EXISTS version_tags;

CREATE TABLE version_tags (
    id              SERIAL    PRIMARY KEY,
    candidate       TEXT      NOT NULL,
    tag             TEXT      NOT NULL,
    distribution    TEXT          NULL,
    platform        TEXT      NOT NULL,
    version_id      INTEGER   NOT NULL REFERENCES versions(id) ON DELETE RESTRICT,
    created_at      TIMESTAMP     NULL DEFAULT now(),
    last_updated_at TIMESTAMP     NULL DEFAULT now(),
    UNIQUE NULLS NOT DISTINCT (candidate, tag, distribution, platform)
);

CREATE INDEX idx_version_tags_version_id ON version_tags(version_id);
CREATE INDEX idx_version_tags_candidate ON version_tags(candidate);
CREATE INDEX idx_version_tags_lookup ON version_tags(candidate, tag, platform);
