package com.crunchycode.bookstore.service;

import com.crunchycode.bookstore.dto.BookCreateRequest;
import com.crunchycode.bookstore.dto.BookResponse;
import com.crunchycode.bookstore.dto.BookUpdateRequest;
import com.crunchycode.bookstore.dto.PageResponse;
import com.crunchycode.bookstore.exception.DuplicateResourceException;
import com.crunchycode.bookstore.exception.ResourceNotFoundException;
import com.crunchycode.bookstore.model.Author;
import com.crunchycode.bookstore.model.Book;
import com.crunchycode.bookstore.model.Category;
import com.crunchycode.bookstore.repository.AuthorRepository;
import com.crunchycode.bookstore.repository.BookRepository;
import com.crunchycode.bookstore.repository.CategoryRepository;
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

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private AuthorRepository authorRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private BookService bookService;

    private Author sampleAuthor;
    private Book sampleBook;

    @BeforeEach
    void setUp() {
        sampleAuthor = new Author(1L, "George Orwell", "orwell@example.com", "Bio");
        sampleBook = new Book(10L, "1984", "9780451524935", new BigDecimal("14.99"), 1949, sampleAuthor);
    }

    @Test
    void testGetAllBooks() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Book> page = new PageImpl<>(List.of(sampleBook), pageable, 1);
        when(bookRepository.findAllWithAssociations(pageable)).thenReturn(page);

        PageResponse<BookResponse> response = bookService.getAllBooks(pageable);

        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertEquals("1984", response.content().get(0).title());
        assertEquals("George Orwell", response.content().get(0).authorName());
    }

    @Test
    void testGetBookByIdSuccess() {
        when(bookRepository.findWithAssociationsById(10L)).thenReturn(Optional.of(sampleBook));

        BookResponse response = bookService.getBookById(10L);

        assertNotNull(response);
        assertEquals(10L, response.id());
        assertEquals("1984", response.title());
    }

    @Test
    void testGetBookByIdNotFound() {
        when(bookRepository.findWithAssociationsById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> bookService.getBookById(99L));
    }

    @Test
    void testCreateBookSuccess() {
        BookCreateRequest req = new BookCreateRequest("Animal Farm", "9780451526342", new BigDecimal("12.99"), 1945, 1L, Set.of(1L));
        Category cat = new Category(1L, "Classics", "Literature");

        when(authorRepository.findById(1L)).thenReturn(Optional.of(sampleAuthor));
        when(bookRepository.existsByIsbn("9780451526342")).thenReturn(false);
        when(categoryRepository.findAllById(Set.of(1L))).thenReturn(List.of(cat));
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> {
            Book b = inv.getArgument(0);
            b.setId(20L);
            return b;
        });

        BookResponse response = bookService.createBook(req);

        assertNotNull(response);
        assertEquals("Animal Farm", response.title());
        verify(bookRepository, times(1)).save(any(Book.class));
    }

    @Test
    void testCreateBookAuthorNotFoundThrowsException() {
        BookCreateRequest req = new BookCreateRequest("Title", "9780451526342", new BigDecimal("12.99"), 1945, 999L, null);
        when(authorRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> bookService.createBook(req));
        verify(bookRepository, never()).save(any());
    }

    @Test
    void testCreateBookDuplicateIsbnThrowsException() {
        BookCreateRequest req = new BookCreateRequest("Title", "9780451524935", new BigDecimal("12.99"), 1945, 1L, null);
        when(authorRepository.findById(1L)).thenReturn(Optional.of(sampleAuthor));
        when(bookRepository.existsByIsbn("9780451524935")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> bookService.createBook(req));
    }

    @Test
    void testDeleteBookSuccess() {
        when(bookRepository.existsById(10L)).thenReturn(true);

        assertDoesNotThrow(() -> bookService.deleteBook(10L));
        verify(bookRepository, times(1)).deleteById(10L);
    }
}
