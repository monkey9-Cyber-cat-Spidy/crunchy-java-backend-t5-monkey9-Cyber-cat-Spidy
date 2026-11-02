package com.crunchycode.bookstore.service;

import com.crunchycode.bookstore.dto.BenchmarkResponse;
import com.crunchycode.bookstore.model.Author;
import com.crunchycode.bookstore.model.Book;
import com.crunchycode.bookstore.model.Category;
import com.crunchycode.bookstore.repository.AuthorRepository;
import com.crunchycode.bookstore.repository.BookRepository;
import com.crunchycode.bookstore.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BenchmarkServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private AuthorRepository authorRepository;

    @Mock
    private CategoryRepository categoryRepository;

    private BenchmarkService benchmarkService;

    @BeforeEach
    void setUp() {
        benchmarkService = new BenchmarkService(bookRepository, authorRepository, categoryRepository);
    }

    @Test
    void testSeedDataIfNecessaryWhenAlreadyPopulated() {
        when(bookRepository.count()).thenReturn(1000L);

        long count = benchmarkService.seedDataIfNecessary(1000);

        assertThat(count).isEqualTo(1000L);
        verify(bookRepository, never()).saveAll(any());
    }

    @Test
    void testSeedDataIfNecessaryWhenUnderTarget() {
        when(bookRepository.count()).thenReturn(0L, 100L);

        Category techCat = new Category(1L, "Technology", "Tech books");
        when(categoryRepository.findAll()).thenReturn(List.of(techCat));

        Author author = new Author(1L, "Author 1", "author1@test.com", "Bio");
        when(authorRepository.findAll()).thenReturn(List.of(author));

        long count = benchmarkService.seedDataIfNecessary(100);

        assertThat(count).isEqualTo(100L);
        verify(bookRepository, atLeastOnce()).saveAll(any());
    }

    @Test
    void testRunBenchmarkCalculatesCorrectMetrics() {
        when(bookRepository.count()).thenReturn(1000L);

        Author author = new Author(1L, "Test Author", "test@benchmark.com", "Bio");
        Book sampleBook = new Book(1L, "Sample Book", "9781234567890", new BigDecimal("19.99"), 2021, author);
        PageImpl<Book> mockPage = new PageImpl<>(List.of(sampleBook));

        when(bookRepository.findAllWithAssociations(any(Pageable.class))).thenReturn(mockPage);

        BenchmarkResponse response = benchmarkService.runBenchmark(10, 20);

        assertThat(response).isNotNull();
        assertThat(response.totalRecords()).isEqualTo(1000L);
        assertThat(response.iterations()).isEqualTo(10);
        assertThat(response.pageSize()).isEqualTo(20);
        assertThat(response.targetThresholdMs()).isEqualTo(500.0);
        assertThat(response.meetsRequirement()).isTrue();
        assertThat(response.meanLatencyMs()).isLessThanOrEqualTo(500.0);
        assertThat(response.summary()).contains("PASS");
    }
}
