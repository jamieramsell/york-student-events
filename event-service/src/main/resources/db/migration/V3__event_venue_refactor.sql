ALTER TABLE event DROP COLUMN location;
ALTER TABLE event ADD COLUMN venue_id uuid REFERENCES venue (id);