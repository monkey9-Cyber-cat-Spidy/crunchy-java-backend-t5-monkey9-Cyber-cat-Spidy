package com.crunchycode.bookstore.service;

import com.crunchycode.bookstore.dto.*;
import com.crunchycode.bookstore.exception.DuplicateResourceException;
import com.crunchycode.bookstore.exception.ResourceNotFoundException;
import com.crunchycode.bookstore.model.Author;
import com.crunchycode.bookstore.model.Book;
import com.crunchycode.bookstore.model.Category;
import com.crunchycode.bookstore.repository.AuthorRepository;
import com.crunchycode.bookstore.repository.BookRepository;
import com.crunchycode.bookstore.repository.CategoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final CategoryRepository categoryRepository;

    public BookService(BookRepository bookRepository,
                       AuthorRepository authorRepository,
                       CategoryRepository categoryRepository) {
        this.bookRepository = Objects.requireNonNull(bookRepository);
        this.authorRepository = Objects.requireNonNull(authorRepository);
        this.categoryRepository = Objects.requireNonNull(categoryRepository);
    }

    @Transactional(readOnly = true)
    public PageResponse<BookResponse> getAllBooks(Pageable pageable) {
        Page<Book> page = bookRepository.findAllWithAssociations(pageable);
        List<BookResponse> dtos = page.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return toPageResponse(page, dtos);
    }

    @Transactional(readOnly = true)
    public PageResponse<BookResponse> getBooksByAuthor(Long authorId, Pageable pageable) {
        if (!authorRepository.existsById(authorId)) {
            throw new ResourceNotFoundException("Author not found with id: " + authorId);
        }
        Page<Book> page = bookRepository.findByAuthorId(authorId, pageable);
        List<BookResponse> dtos = page.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return toPageResponse(page, dtos);
    }

    @Transactional(readOnly = true)
    public BookResponse getBookById(Long id) {
        Book book = bookRepository.findWithAssociationsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));
        return mapToResponse(book);
    }

    @Transactional
    public BookResponse createBook(BookCreateRequest request) {
        Author author = authorRepository.findById(request.authorId())
                .orElseThrow(() -> new ResourceNotFoundException("Author not found with id: " + request.authorId()));

        if (bookRepository.existsByIsbn(request.isbn())) {
            throw new DuplicateResourceException("Book already exists with ISBN: " + request.isbn());
        }

        Book book = new Book();
        book.setTitle(request.title().trim());
        book.setIsbn(request.isbn().trim().toUpperCase());
        book.setPrice(request.price());
        book.setPublicationYear(request.publicationYear());
        book.setAuthor(author);

        if (request.categoryIds() != null && !request.categoryIds().isEmpty()) {
            Set<Category> categories = new HashSet<>(categoryRepository.findAllById(request.categoryIds()));
            book.setCategories(categories);
        }

        Book saved = bookRepository.save(book);
        return mapToResponse(saved);
    }

    @Transactional
    public BookResponse updateBook(Long id, BookUpdateRequest request) {
        Book existing = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));

        Author author = authorRepository.findById(request.authorId())
                .orElseThrow(() -> new ResourceNotFoundException("Author not found with id: " + request.authorId()));

        if (!existing.getIsbn().equalsIgnoreCase(request.isbn())
                && bookRepository.existsByIsbn(request.isbn())) {
            throw new DuplicateResourceException("Book already exists with ISBN: " + request.isbn());
        }

        existing.setTitle(request.title().trim());
        existing.setIsbn(request.isbn().trim().toUpperCase());
        existing.setPrice(request.price());
        existing.setPublicationYear(request.publicationYear());
        existing.setAuthor(author);

        if (request.categoryIds() != null) {
            Set<Category> categories = new HashSet<>(categoryRepository.findAllById(request.categoryIds()));
            existing.setCategories(categories);
        }

        Book saved = bookRepository.save(existing);
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteBook(Long id) {
        if (!bookRepository.existsById(id)) {
            throw new ResourceNotFoundException("Book not found with id: " + id);
        }
        bookRepository.deleteById(id);
    }

    private BookResponse mapToResponse(Book book) {
        List<String> catNames = book.getCategories().stream()
                .map(Category::getName)
                .sorted()
                .collect(Collectors.toList());

        return new BookResponse(
                book.getId(),
                book.getTitle(),
                book.getIsbn(),
                book.getPrice(),
                book.getPublicationYear(),
                book.getAuthor().getId(),
                book.getAuthor().getName(),
                catNames
        );
    }

    private <T> PageResponse<T> toPageResponse(Page<?> page, List<T> dtos) {
        return new PageResponse<>(
                dtos,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
