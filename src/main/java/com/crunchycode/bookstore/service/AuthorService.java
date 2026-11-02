package com.crunchycode.bookstore.service;

import com.crunchycode.bookstore.dto.AuthorCreateRequest;
import com.crunchycode.bookstore.dto.AuthorResponse;
import com.crunchycode.bookstore.dto.AuthorUpdateRequest;
import com.crunchycode.bookstore.dto.PageResponse;
import com.crunchycode.bookstore.exception.BusinessRuleException;
import com.crunchycode.bookstore.exception.DuplicateResourceException;
import com.crunchycode.bookstore.exception.ResourceNotFoundException;
import com.crunchycode.bookstore.model.Author;
import com.crunchycode.bookstore.repository.AuthorRepository;
import com.crunchycode.bookstore.repository.BookRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AuthorService {

    private final AuthorRepository authorRepository;
    private final BookRepository bookRepository;

    public AuthorService(AuthorRepository authorRepository, BookRepository bookRepository) {
        this.authorRepository = Objects.requireNonNull(authorRepository);
        this.bookRepository = Objects.requireNonNull(bookRepository);
    }

    @Transactional(readOnly = true)
    public PageResponse<AuthorResponse> getAllAuthors(Pageable pageable) {
        Page<Author> page = authorRepository.findAll(pageable);
        List<AuthorResponse> dtos = page.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return new PageResponse<>(
                dtos,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    @Transactional(readOnly = true)
    public AuthorResponse getAuthorById(Long id) {
        Author author = authorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Author not found with id: " + id));
        return mapToResponse(author);
    }

    @Transactional
    public AuthorResponse createAuthor(AuthorCreateRequest request) {
        if (authorRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Author already exists with email: " + request.email());
        }

        Author author = new Author();
        author.setName(request.name().trim());
        author.setEmail(request.email().trim().toLowerCase());
        author.setBio(request.bio() != null ? request.bio().trim() : null);

        Author saved = authorRepository.save(author);
        return mapToResponse(saved);
    }

    @Transactional
    public AuthorResponse updateAuthor(Long id, AuthorUpdateRequest request) {
        Author existing = authorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Author not found with id: " + id));

        if (!existing.getEmail().equalsIgnoreCase(request.email())
                && authorRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Author already exists with email: " + request.email());
        }

        existing.setName(request.name().trim());
        existing.setEmail(request.email().trim().toLowerCase());
        existing.setBio(request.bio() != null ? request.bio().trim() : null);

        Author saved = authorRepository.save(existing);
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteAuthor(Long id) {
        if (!authorRepository.existsById(id)) {
            throw new ResourceNotFoundException("Author not found with id: " + id);
        }

        int bookCount = bookRepository.countByAuthorId(id);
        if (bookCount > 0) {
            throw new BusinessRuleException("Cannot delete author with id " + id + " because they still have " + bookCount + " registered book(s)");
        }

        authorRepository.deleteById(id);
    }

    private AuthorResponse mapToResponse(Author author) {
        int count = bookRepository.countByAuthorId(author.getId());
        return new AuthorResponse(
                author.getId(),
                author.getName(),
                author.getEmail(),
                author.getBio(),
                count
        );
    }
}
