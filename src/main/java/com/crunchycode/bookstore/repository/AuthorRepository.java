package com.crunchycode.bookstore.repository;

import com.crunchycode.bookstore.model.Author;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthorRepository extends JpaRepository<Author, Long> {
    boolean existsByEmail(String email);
    Optional<Author> findByEmail(String email);
}
