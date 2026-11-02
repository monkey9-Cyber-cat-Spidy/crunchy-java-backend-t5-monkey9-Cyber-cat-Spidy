package com.crunchycode.bookstore.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Set;

public record BookUpdateRequest(
        @NotBlank(message = "Book title is required")
        @Size(min = 1, max = 200, message = "Title must be between 1 and 200 characters")
        String title,

        @NotBlank(message = "ISBN is required")
        @Pattern(regexp = "^(97(8|9))?\\d{9}(\\d|X)$", message = "Invalid ISBN format")
        String isbn,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.01", message = "Price must be greater than zero")
        @Digits(integer = 6, fraction = 2, message = "Price format must have up to 6 digits and 2 decimals")
        BigDecimal price,

        @NotNull(message = "Publication year is required")
        @Min(value = 1450, message = "Publication year must be 1450 or later")
        @Max(value = 2026, message = "Publication year cannot be in the future")
        Integer publicationYear,

        @NotNull(message = "authorId is required")
        @Positive(message = "authorId must be a positive number")
        Long authorId,

        Set<Long> categoryIds
) {}
