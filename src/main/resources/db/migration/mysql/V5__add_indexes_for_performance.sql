-- V5__add_indexes_for_performance.sql
-- High-Performance Indexes for Sub-500ms Query Latency under High Record Volumes

-- Fast lookup and sorting by book title
CREATE INDEX idx_books_title ON books(title);

-- Range filtering and sorting by publication year
CREATE INDEX idx_books_publication_year ON books(publication_year);

-- Range filtering and sorting by book price
CREATE INDEX idx_books_price ON books(price);

-- Fast lookup and alphabetical sorting by author name
CREATE INDEX idx_authors_name ON authors(name);
