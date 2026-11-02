package com.crunchycode.bookstore.service;

import com.crunchycode.bookstore.dto.BenchmarkResponse;
import com.crunchycode.bookstore.model.Author;
import com.crunchycode.bookstore.model.Book;
import com.crunchycode.bookstore.model.Category;
import com.crunchycode.bookstore.repository.AuthorRepository;
import com.crunchycode.bookstore.repository.BookRepository;
import com.crunchycode.bookstore.repository.CategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class BenchmarkService {

    private static final Logger log = LoggerFactory.getLogger(BenchmarkService.class);
    private static final double TARGET_THRESHOLD_MS = 500.0;

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final CategoryRepository categoryRepository;

    public BenchmarkService(BookRepository bookRepository,
                            AuthorRepository authorRepository,
                            CategoryRepository categoryRepository) {
        this.bookRepository = Objects.requireNonNull(bookRepository);
        this.authorRepository = Objects.requireNonNull(authorRepository);
        this.categoryRepository = Objects.requireNonNull(categoryRepository);
    }

    @Transactional
    public long seedDataIfNecessary(int targetCount) {
        long currentCount = bookRepository.count();
        if (currentCount >= targetCount) {
            log.info("Database already contains {} books (target: {}). Skipping seed.", currentCount, targetCount);
            return currentCount;
        }

        log.info("Seeding database to {} records (current: {})...", targetCount, currentCount);

        // Ensure categories exist
        List<Category> categories = categoryRepository.findAll();
        if (categories.isEmpty()) {
            categories = categoryRepository.saveAll(List.of(
                    new Category(null, "Technology", "Software and Computer Systems"),
                    new Category(null, "Science", "Natural and Physical Sciences"),
                    new Category(null, "Fiction", "Literary and Speculative Works"),
                    new Category(null, "History", "Historical Studies and Biographies"),
                    new Category(null, "Philosophy", "Epistemology and Ethics")
            ));
        }

        // Ensure at least 25 authors exist
        List<Author> authors = authorRepository.findAll();
        if (authors.size() < 25) {
            List<Author> newAuthors = new ArrayList<>();
            for (int i = authors.size() + 1; i <= 25; i++) {
                newAuthors.add(new Author(null, "Benchmark Author " + i, "author" + i + "@crunchybenchmark.internal", "Bio for author " + i));
            }
            authors = new ArrayList<>(authors);
            authors.addAll(authorRepository.saveAll(newAuthors));
        }

        // Generate books in batches of 200
        long remaining = targetCount - currentCount;
        List<Book> batch = new ArrayList<>(200);
        Random random = new Random(42); // deterministic seed for reproducibility

        for (int i = 1; i <= remaining; i++) {
            long sequence = currentCount + i;
            Author author = authors.get(random.nextInt(authors.size()));
            Category cat = categories.get(random.nextInt(categories.size()));

            String isbn = String.format("978-%010d", sequence);
            BigDecimal price = BigDecimal.valueOf(10.0 + (random.nextDouble() * 90.0)).setScale(2, RoundingMode.HALF_UP);
            int pubYear = 1990 + random.nextInt(35);

            Book book = new Book(
                    null,
                    "Benchmark Performance Book Vol. " + sequence,
                    isbn,
                    price,
                    pubYear,
                    author
            );
            book.addCategory(cat);
            batch.add(book);

            if (batch.size() >= 200 || i == remaining) {
                bookRepository.saveAll(batch);
                batch.clear();
            }
        }

        long finalCount = bookRepository.count();
        log.info("Seeding completed successfully. Total book records now: {}", finalCount);
        return finalCount;
    }

    @Transactional(readOnly = true)
    public BenchmarkResponse runBenchmark(int iterations, int pageSize) {
        long totalRecords = bookRepository.count();
        if (totalRecords < 1000) {
            totalRecords = seedDataIfNecessary(1000);
        }

        int maxPage = (int) Math.max(0, (totalRecords / pageSize) - 1);
        Random random = new Random();

        // Warm up JVM and Hibernate query plan cache (5 executions)
        for (int w = 0; w < 5; w++) {
            Pageable warmupPage = PageRequest.of(w % (maxPage + 1), pageSize, Sort.by(Sort.Direction.ASC, "title"));
            bookRepository.findAllWithAssociations(warmupPage);
        }

        List<Double> latencies = new ArrayList<>(iterations);

        for (int i = 0; i < iterations; i++) {
            int pageIndex = random.nextInt(maxPage + 1);
            String sortProperty = (i % 3 == 0) ? "title" : (i % 3 == 1) ? "publicationYear" : "price";
            Sort.Direction direction = (i % 2 == 0) ? Sort.Direction.ASC : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(direction, sortProperty));

            long startTime = System.nanoTime();
            bookRepository.findAllWithAssociations(pageable);
            long elapsedNanos = System.nanoTime() - startTime;

            double latencyMs = elapsedNanos / 1_000_000.0;
            latencies.add(latencyMs);
        }

        Collections.sort(latencies);

        double sum = 0;
        for (double lat : latencies) {
            sum += lat;
        }
        double mean = round(sum / latencies.size());
        double min = round(latencies.get(0));
        double max = round(latencies.get(latencies.size() - 1));
        double p50 = round(percentile(latencies, 50));
        double p95 = round(percentile(latencies, 95));
        double p99 = round(percentile(latencies, 99));

        boolean meetsRequirement = p95 <= TARGET_THRESHOLD_MS && mean <= TARGET_THRESHOLD_MS;
        String summary = meetsRequirement
                ? String.format("PASS: At %d records, mean latency is %.2f ms and p95 is %.2f ms, strictly beating the 500 ms threshold.",
                totalRecords, mean, p95)
                : String.format("FAIL: Response latency exceeded the 500 ms threshold (p95: %.2f ms).", p95);

        return new BenchmarkResponse(
                totalRecords,
                iterations,
                pageSize,
                true,
                TARGET_THRESHOLD_MS,
                mean,
                p50,
                p95,
                p99,
                min,
                max,
                meetsRequirement,
                summary
        );
    }

    private double percentile(List<Double> sortedList, double percentile) {
        int index = (int) Math.ceil((percentile / 100.0) * sortedList.size()) - 1;
        index = Math.max(0, Math.min(index, sortedList.size() - 1));
        return sortedList.get(index);
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
