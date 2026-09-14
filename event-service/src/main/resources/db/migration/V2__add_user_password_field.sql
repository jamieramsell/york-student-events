ALTER TABLE users DROP COLUMN cohort;

ALTER TABLE users ADD COLUMN password_hash varchar;

/* Adds a default, illegal value, just in case anyone stores a user before the
 * migration is executed. Services should not accept this illegal password, as 
 * it contains a space, meaning the user will be forced to reset their password.
 */
UPDATE users SET password_hash = 'migrated stub' WHERE password_hash IS NULL;
ALTER TABLE users ALTER COLUMN password_hash SET NOT NULL;