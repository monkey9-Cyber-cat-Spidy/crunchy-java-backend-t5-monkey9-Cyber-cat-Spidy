package com.crunchycode.bookstore.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthorUpdateRequest(
        @NotBlank(message = "Author name is required")
        @Size(min = 2, max = 100, message = "Author name must be between 2 and 100 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @Size(max = 500, message = "Bio cannot exceed 500 characters")
        String bio
) {}
