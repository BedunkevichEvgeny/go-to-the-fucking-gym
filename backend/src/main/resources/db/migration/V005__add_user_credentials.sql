-- V005: Add authentication credentials to users table

-- Step 1: Add columns as nullable to allow existing rows
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS username VARCHAR(120),
    ADD COLUMN IF NOT EXISTS password VARCHAR(255);

-- Step 2: Assign placeholder credentials to any rows that have no username yet
--         (only relevant if the table had pre-existing rows from a dev environment)
UPDATE users SET username = 'user_' || id::text, password = 'changeme'
WHERE username IS NULL;

-- Step 3: Enforce NOT NULL now that all rows have values
ALTER TABLE users
    ALTER COLUMN username SET NOT NULL,
    ALTER COLUMN password SET NOT NULL;

-- Step 4: Unique index on username
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_username ON users (username);

-- Step 5: Seed admin user (admin / admin — plain text, NoOpPasswordEncoder, MVP only)
INSERT INTO users (id, preferred_weight_unit, username, password)
VALUES (gen_random_uuid(), 'KG', 'admin', 'admin')
ON CONFLICT (username) DO NOTHING;
