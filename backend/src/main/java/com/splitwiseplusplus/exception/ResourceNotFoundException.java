package com.splitwiseplusplus.exception;

// ── Custom Exception Types ────────────────────────────────────

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) { super(message); }
}
