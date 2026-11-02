package com.crunchycode.bookstore.dto;

public record AuthorResponse(
        Long id,
        String name,
        String email,
        String bio,
        int bookCount
) {}
