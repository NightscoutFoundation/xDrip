DELETE FROM BgReadings WHERE rowid NOT IN (SELECT MAX(rowid) FROM BgReadings GROUP BY uuid);
DROP INDEX index_BgReadings_uuid;
CREATE UNIQUE INDEX index_BgReadings_uuid on BgReadings(uuid);
