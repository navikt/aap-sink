CREATE TABLE søker
(
    id          BIGSERIAL PRIMARY KEY,
    personident VARCHAR(11) NOT NULL,
    record      TEXT        NOT NULL
);

CREATE INDEX søker_personident ON søker (personident);

-- give access to IAM users (GCP)
-- GRANT ALL ON ALL TABLES IN SCHEMA PUBLIC TO cloudsqliamuser;
