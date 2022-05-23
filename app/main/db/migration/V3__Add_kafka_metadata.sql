ALTER TABLE søker ADD COLUMN kafka_partition numeric;
ALTER TABLE søker ADD COLUMN kafka_offset bigint;
ALTER TABLE søker ADD COLUMN kafka_topic VARCHAR(50);
ALTER TABLE søker ADD COLUMN kafka_timestamp_ms bigint;
ALTER TABLE søker ADD COLUMN kafka_stream_time_ms bigint;
ALTER TABLE søker ADD COLUMN kafka_system_time_ms bigint;
