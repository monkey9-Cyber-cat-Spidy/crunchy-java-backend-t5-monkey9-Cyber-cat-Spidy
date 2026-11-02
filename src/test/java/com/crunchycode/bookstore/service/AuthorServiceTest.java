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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthorServiceTest {

    @Mock
    private AuthorRepository authorRepository;

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private AuthorService authorService;

    private Author sampleAuthor;

    @BeforeEach
    void setUp() {
        sampleAuthor = new Author(1L, "George Orwell", "orwell@example.com", "English novelist");
    }

    @Test
    void testGetAllAuthors() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Author> page = new PageImpl<>(List.of(sampleAuthor), pageable, 1);
        when(authorRepository.findAll(pageable)).thenReturn(page);
        when(bookRepository.countByAuthorId(1L)).thenReturn(2);

        PageResponse<AuthorResponse> response = authorService.getAllAuthors(pageable);

        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertEquals("George Orwell", response.content().get(0).name());
        assertEquals(2, response.content().get(0).bookCount());
    }

    @Test
    void testGetAuthorByIdSuccess() {
        when(authorRepository.findById(1L)).thenReturn(Optional.of(sampleAuthor));
        when(bookRepository.countByAuthorId(1L)).thenReturn(0);

        AuthorResponse response = authorService.getAuthorById(1L);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("George Orwell", response.name());
    }

    @Test
    void testGetAuthorByIdNotFound() {
        when(authorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authorService.getAuthorById(99L));
    }

    @Test
    void testCreateAuthorSuccess() {
        AuthorCreateRequest req = new AuthorCreateRequest("Jane Austen", "austen@example.com", "English novelist");
        when(authorRepository.existsByEmail("austen@example.com")).thenReturn(false);
        when(authorRepository.save(any(Author.class))).thenAnswer(inv -> {
            Author a = inv.getArgument(0);
            a.setId(2L);
            return a;
        });

        AuthorResponse response = authorService.createAuthor(req);

        assertNotNull(response);
        assertEquals("Jane Austen", response.name());
        assertEquals("austen@example.com", response.email());
        verify(authorRepository, times(1)).save(any(Author.class));
    }

    @Test
    void testCreateAuthorDuplicateEmailThrowsException() {
        AuthorCreateRequest req = new AuthorCreateRequest("Jane Austen", "orwell@example.com", "Bio");
        when(authorRepository.existsByEmail("orwell@example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authorService.createAuthor(req));
        verify(authorRepository, never()).save(any());
    }

    @Test
    void testUpdateAuthorSuccess() {
        AuthorUpdateRequest req = new AuthorUpdateRequest("George Orwell Updated", "orwell@example.com", "Updated Bio");
        when(authorRepository.findById(1L)).thenReturn(Optional.of(sampleAuthor));
        when(authorRepository.save(any(Author.class))).thenReturn(sampleAuthor);

        AuthorResponse response = authorService.updateAuthor(1L, req);

        assertNotNull(response);
        assertEquals("George Orwell Updated", response.name());
    }

    @Test
    void testDeleteAuthorSuccess() {
        when(authorRepository.existsById(1L)).thenReturn(true);
        when(bookRepository.countByAuthorId(1L)).thenReturn(0);

        assertDoesNotThrow(() -> authorService.deleteAuthor(1L));
        verify(authorRepository, times(1)).deleteById(1L);
    }

    @Test
    void testDeleteAuthorWithActiveBooksThrowsBusinessRuleException() {
        when(authorRepository.existsById(1L)).thenReturn(true);
        when(bookRepository.countByAuthorId(1L)).thenReturn(3);

        assertThrows(BusinessRuleException.class, () -> authorService.deleteAuthor(1L));
        verify(authorRepository, never()).deleteById(1L);
    }
}
