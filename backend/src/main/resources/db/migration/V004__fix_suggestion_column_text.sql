-- Repair: ensure suggestion column is TEXT (not VARCHAR(255)).
-- V003 declared TEXT, but Hibernate ddl-auto=update can silently downgrade
-- a String field to CHARACTER VARYING(255) when columnDefinition is absent.
-- This migration is idempotent: ALTER TYPE on a column already typed TEXT is a no-op in PostgreSQL.
ALTER TABLE session_ai_suggestions
    ALTER COLUMN suggestion TYPE TEXT;

