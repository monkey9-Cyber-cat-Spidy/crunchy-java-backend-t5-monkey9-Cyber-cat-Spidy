package com.crunchycode.bookstore.controller;

import com.crunchycode.bookstore.dto.BenchmarkResponse;
import com.crunchycode.bookstore.service.BenchmarkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/benchmark")
@Tag(name = "Performance Benchmark", description = "Endpoints for demonstrating sub-500ms paginated query response times across 1,000+ records")
public class BenchmarkController {

    private final BenchmarkService benchmarkService;

    public BenchmarkController(BenchmarkService benchmarkService) {
        this.benchmarkService = Objects.requireNonNull(benchmarkService);
    }

    @GetMapping("/run")
    @Operation(
            summary = "Execute 1,000-record query performance benchmark",
            description = "Seeds 1,000 records if not present, runs warm-up iterations, and executes a battery of paginated, indexed queries with associations to measure latency percentiles against the 500ms requirement."
    )
    @ApiResponse(responseCode = "200", description = "Benchmark executed successfully",
            content = @Content(schema = @Schema(implementation = BenchmarkResponse.class)))
    public ResponseEntity<BenchmarkResponse> runBenchmark(
            @Parameter(description = "Number of sample queries to evaluate (default 50)")
            @RequestParam(defaultValue = "50") int iterations,
            @Parameter(description = "Page size per query (default 20)")
            @RequestParam(defaultValue = "20") int pageSize) {

        int boundedIterations = Math.max(10, Math.min(iterations, 200));
        int boundedPageSize = Math.max(5, Math.min(pageSize, 100));

        BenchmarkResponse response = benchmarkService.runBenchmark(boundedIterations, boundedPageSize);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/seed")
    @Operation(summary = "Seed 1,000 book records", description = "Idempotently populates the database with 1,000 realistic books and authors for latency benchmarking.")
    @ApiResponse(responseCode = "200", description = "Seeding completed")
    public ResponseEntity<Map<String, Object>> seedDatabase(
            @Parameter(description = "Target total book count (default 1000)")
            @RequestParam(defaultValue = "1000") int count) {

        int target = Math.max(100, Math.min(count, 5000));
        long total = benchmarkService.seedDataIfNecessary(target);
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "totalBookRecords", total,
                "message", "Database ready for performance benchmarking."
        ));
    }
}
