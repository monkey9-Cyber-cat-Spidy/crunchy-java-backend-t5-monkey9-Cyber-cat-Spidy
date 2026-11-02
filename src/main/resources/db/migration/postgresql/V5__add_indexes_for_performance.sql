-- V5__add_indexes_for_performance.sql (PostgreSQL)
CREATE INDEX idx_books_title ON books(title);
CREATE INDEX idx_books_publication_year ON books(publication_year);
CREATE INDEX idx_books_price ON books(price);
CREATE INDEX idx_authors_name ON authors(name);
