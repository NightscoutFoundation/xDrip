DROP INDEX index_BgReadings_uuid;
CREATE UNIQUE INDEX index_BgReadings_uuid on BgReadings(uuid);
