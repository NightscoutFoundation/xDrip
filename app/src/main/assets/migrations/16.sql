ALTER TABLE CalibrationSendQueue ADD COLUMN mongo_success BOOLEAN;
ALTER TABLE BgSendQueue ADD COLUMN mongo_success BOOLEAN;
