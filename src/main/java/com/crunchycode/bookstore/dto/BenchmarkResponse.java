package com.crunchycode.bookstore.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Performance benchmark telemetry proving sub-500ms latency requirement at 1,000 records")
public record BenchmarkResponse(
        @Schema(description = "Total book records in the database", example = "1000")
        long totalRecords,

        @Schema(description = "Number of sample queries executed", example = "50")
        int iterations,

        @Schema(description = "Page size requested per paginated query", example = "20")
        int pageSize,

        @Schema(description = "Whether database indexes are active on queried columns", example = "true")
        boolean indexedQueries,

        @Schema(description = "Target latency threshold requirement in ms", example = "500.0")
        double targetThresholdMs,

        @Schema(description = "Arithmetic mean latency in milliseconds", example = "12.45")
        double meanLatencyMs,

        @Schema(description = "50th percentile (median) latency in milliseconds", example = "10.82")
        double p50LatencyMs,

        @Schema(description = "95th percentile latency in milliseconds", example = "23.15")
        double p95LatencyMs,

        @Schema(description = "99th percentile latency in milliseconds", example = "34.60")
        double p99LatencyMs,

        @Schema(description = "Minimum recorded query latency in ms", example = "6.20")
        double minLatencyMs,

        @Schema(description = "Maximum recorded query latency in ms", example = "42.10")
        double maxLatencyMs,

        @Schema(description = "Whether the measured response time satisfies < 500ms at 1,000 records", example = "true")
        boolean meetsRequirement,

        @Schema(description = "Formal evaluation statement", example = "Pass: Response times under 500ms at 1,000 records verified.")
        String summary
) {
}
