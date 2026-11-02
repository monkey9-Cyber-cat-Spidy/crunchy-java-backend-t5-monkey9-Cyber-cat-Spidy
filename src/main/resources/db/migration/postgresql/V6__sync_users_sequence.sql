-- V6__sync_users_sequence.sql (PostgreSQL)
-- Synchronizes the identity sequence for the users table with the max ID
SELECT setval(pg_get_serial_sequence('users', 'id'), COALESCE(MAX(id), 1)) FROM users;
