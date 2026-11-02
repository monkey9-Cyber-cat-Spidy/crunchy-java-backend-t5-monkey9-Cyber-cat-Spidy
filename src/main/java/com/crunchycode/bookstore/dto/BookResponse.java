package com.crunchycode.bookstore.dto;

import java.math.BigDecimal;
import java.util.List;

public record BookResponse(
        Long id,
        String title,
        String isbn,
        BigDecimal price,
        Integer publicationYear,
        Long authorId,
        String authorName,
        List<String> categories
) {}
