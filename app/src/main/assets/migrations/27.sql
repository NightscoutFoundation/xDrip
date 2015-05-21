ALTER TABLE TransmitterData ADD COLUMN filtered_data DOUBLE;
ALTER TABLE BgReadings ADD COLUMN filtered_data DOUBLE;
ALTER TABLE BgReadings ADD COLUMN selected_filtered_data BOOLEAN;
