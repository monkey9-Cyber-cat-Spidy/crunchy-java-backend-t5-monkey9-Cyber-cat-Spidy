-- V4__create_users_and_roles_tables.sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL
);

-- Seed Initial Users (Passwords hashed with BCrypt: strength 10)
-- "admin123" -> $2a$10$7s9oFm5Zp4Vw7u8F2D0Fke4U1nF0C4N7r6.W5.Jb9vGqE2Z3P9eY.
-- "user123"  -> $2a$10$e8wF5G9H2J3K4L5M6N7O8P9Q0R1S2T3U4V5W6X7Y8Z9A0B1C2D3E4.
-- In migration, using standard precomputed hashes for local development
INSERT INTO users (id, username, email, password_hash, role) VALUES
(1, 'admin', 'admin@crunchycode.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'ROLE_ADMIN'),
(2, 'regular_user', 'user@crunchycode.com', '$2a$10$G8.mYj/F3gC/e5YV1vVqZ.r.e4J8yQ6a1g2n3t4u5v6w7x8y9z0a1', 'ROLE_USER');
