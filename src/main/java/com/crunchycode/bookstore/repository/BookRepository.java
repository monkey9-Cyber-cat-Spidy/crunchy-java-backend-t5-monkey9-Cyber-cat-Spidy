package com.crunchycode.bookstore.repository;

import com.crunchycode.bookstore.model.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    boolean existsByIsbn(String isbn);
    int countByAuthorId(Long authorId);
    Page<Book> findByAuthorId(Long authorId, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "categories"})
    @Query("SELECT b FROM Book b")
    Page<Book> findAllWithAssociations(Pageable pageable);

    @EntityGraph(attributePaths = {"author", "categories"})
    Optional<Book> findWithAssociationsById(Long id);
}
