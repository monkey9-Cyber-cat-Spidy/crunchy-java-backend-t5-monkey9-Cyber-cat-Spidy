package com.crunchycode.bookstore.controller;

import com.crunchycode.bookstore.dto.ApiErrorResponse;
import com.crunchycode.bookstore.dto.AuthorCreateRequest;
import com.crunchycode.bookstore.dto.AuthorResponse;
import com.crunchycode.bookstore.dto.AuthorUpdateRequest;
import com.crunchycode.bookstore.dto.PageResponse;
import com.crunchycode.bookstore.service.AuthorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/authors")
@Tag(name = "Authors", description = "Endpoints for querying and managing book authors")
public class AuthorController {

    private final AuthorService authorService;

    public AuthorController(AuthorService authorService) {
        this.authorService = Objects.requireNonNull(authorService);
    }

    // Public / Anyone can view authors
    @GetMapping
    @Operation(summary = "List authors (paginated)", description = "Retrieves a paginated list of authors with configurable page size, page index, and sort order. Public endpoint.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authors list retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PageResponse.class)))
    })
    public ResponseEntity<PageResponse<AuthorResponse>> getAllAuthors(
            @Parameter(description = "Pagination and sorting criteria (e.g. ?page=0&size=20&sort=name,asc)")
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(authorService.getAllAuthors(pageable));
    }

    // Public / Anyone can view author by ID
    @GetMapping("/{id}")
    @Operation(summary = "Get author by ID", description = "Retrieves author details by primary key ID. Public endpoint.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Author found",
                    content = @Content(schema = @Schema(implementation = AuthorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Author not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<AuthorResponse> getAuthorById(@PathVariable Long id) {
        return ResponseEntity.ok(authorService.getAuthorById(id));
    }

    // ROLE_ADMIN only can create authors
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create author (ADMIN only)", description = "Creates a new author record. Requires ROLE_ADMIN authentication.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Author created successfully",
                    content = @Content(schema = @Schema(implementation = AuthorResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request payload",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Full authentication is required",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied (Requires ROLE_ADMIN)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Email conflict",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<AuthorResponse> createAuthor(@Valid @RequestBody AuthorCreateRequest request) {
        AuthorResponse created = authorService.createAuthor(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    // ROLE_ADMIN only can update authors
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update author (ADMIN only)", description = "Updates an existing author. Requires ROLE_ADMIN authentication.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Author updated successfully",
                    content = @Content(schema = @Schema(implementation = AuthorResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failure",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthenticated",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied (Requires ROLE_ADMIN)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Author not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<AuthorResponse> updateAuthor(@PathVariable Long id,
                                                       @Valid @RequestBody AuthorUpdateRequest request) {
        return ResponseEntity.ok(authorService.updateAuthor(id, request));
    }

    // ROLE_ADMIN only can delete authors
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete author (ADMIN only)", description = "Deletes an author record by ID. Requires ROLE_ADMIN authentication.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Author successfully deleted"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied (Requires ROLE_ADMIN)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Author not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Cannot delete author with associated books",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteAuthor(@PathVariable Long id) {
        authorService.deleteAuthor(id);
        return ResponseEntity.noContent().build();
    }
}
