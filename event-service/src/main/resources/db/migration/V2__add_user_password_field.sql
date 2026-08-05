ALTER TABLE users (
    DROP COLUMN cohort
    ADD password_hash varchar NOT NULL
);