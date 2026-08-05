ALTER TABLE users DROP COLUMN cohort;
ALTER TABLE users ADD COLUMN password_hash varchar NOT NULL;
