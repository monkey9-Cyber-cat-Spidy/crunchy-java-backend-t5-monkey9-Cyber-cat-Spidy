package com.crunchycode.bookstore.controller;

import com.crunchycode.bookstore.dto.ApiErrorResponse;
import com.crunchycode.bookstore.dto.BookCreateRequest;
import com.crunchycode.bookstore.dto.BookResponse;
import com.crunchycode.bookstore.dto.BookUpdateRequest;
import com.crunchycode.bookstore.dto.PageResponse;
import com.crunchycode.bookstore.service.BookService;
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
@RequestMapping("/api/v1/books")
@Tag(name = "Books", description = "Endpoints for querying and managing books and categories")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = Objects.requireNonNull(bookService);
    }

    // Public / Anyone can view books
    @GetMapping
    @Operation(summary = "List books (paginated & optimized)", description = "Retrieves a paginated list of books with eager author & category fetching (@EntityGraph) to prevent N+1 queries. Supports authorId filter. Public endpoint.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Books list retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PageResponse.class)))
    })
    public ResponseEntity<PageResponse<BookResponse>> getAllBooks(
            @Parameter(description = "Optional filter by Author ID")
            @RequestParam(required = false) Long authorId,
            @Parameter(description = "Pagination parameters (e.g. ?page=0&size=20&sort=title,asc)")
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {

        if (authorId != null) {
            return ResponseEntity.ok(bookService.getBooksByAuthor(authorId, pageable));
        }
        return ResponseEntity.ok(bookService.getAllBooks(pageable));
    }

    // Public / Anyone can view book by ID
    @GetMapping("/{id}")
    @Operation(summary = "Get book by ID", description = "Retrieves book details including author and categories. Public endpoint.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Book found",
                    content = @Content(schema = @Schema(implementation = BookResponse.class))),
            @ApiResponse(responseCode = "404", description = "Book not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<BookResponse> getBookById(@PathVariable Long id) {
        return ResponseEntity.ok(bookService.getBookById(id));
    }

    // ROLE_USER or ROLE_ADMIN can create books
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create book (USER or ADMIN)", description = "Creates a new book. Requires authenticated user with ROLE_USER or ROLE_ADMIN.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Book created successfully",
                    content = @Content(schema = @Schema(implementation = BookResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request payload",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Referenced author not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "ISBN already exists",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<BookResponse> createBook(@Valid @RequestBody BookCreateRequest request) {
        BookResponse created = bookService.createBook(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    // ROLE_ADMIN only can update books
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update book (ADMIN only)", description = "Updates an existing book record. Requires ROLE_ADMIN authentication.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Book updated successfully",
                    content = @Content(schema = @Schema(implementation = BookResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failure",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied (Requires ROLE_ADMIN)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Book not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<BookResponse> updateBook(@PathVariable Long id,
                                                   @Valid @RequestBody BookUpdateRequest request) {
        return ResponseEntity.ok(bookService.updateBook(id, request));
    }

    // ROLE_ADMIN only can delete books
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete book (ADMIN only)", description = "Deletes a book by ID. Requires ROLE_ADMIN authentication.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Book deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied (Requires ROLE_ADMIN)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Book not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }
}
