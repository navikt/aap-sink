CREATE TABLE mottaker
(
    id                   BIGSERIAL PRIMARY KEY,
    personident          VARCHAR(11) NOT NULL,
    record               TEXT        NOT NULL,
    kafka_partition      numeric,
    kafka_offset         bigint,
    kafka_topic          VARCHAR(50),
    kafka_timestamp_ms   bigint,
    kafka_stream_time_ms bigint,
    kafka_system_time_ms bigint,
    dto_version          numeric
);

CREATE INDEX mottaker_personident ON mottaker (personident);
