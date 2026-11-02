package com.crunchycode.bookstore.model;

/**
 * Role definitions for Role-Based Access Control (RBAC).
 * ROLE_USER: Can view books and authors.
 * ROLE_ADMIN: Can create, update, and delete books and authors.
 */
public enum Role {
    ROLE_USER,
    ROLE_ADMIN
}
