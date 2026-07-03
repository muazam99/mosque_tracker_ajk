package com.qiyam.user.controller;

import com.qiyam.user.dto.UserRequest;
import com.qiyam.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Manage platform users including CRUD, role management, and password reset")
public class UserController {
    private final UserService userService;

    @GetMapping
    @Operation(summary = "Get all users", description = "Returns a paginated list of all platform users")
    public ResponseEntity<List<Map<String, Object>>> getAll(
            @Parameter(description = "Maximum number of records to return")
            @RequestParam(defaultValue = "20") int limit,
            @Parameter(description = "Number of records to skip")
            @RequestParam(defaultValue = "0") int offset) {
        return ResponseEntity.ok(userService.getAll(limit, offset));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Returns a single user by their unique identifier")
    public ResponseEntity<Map<String, Object>> getById(@Parameter(description = "User ID") @PathVariable String id) {
        return userService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create a user", description = "Creates a new platform user")
    public ResponseEntity<Map<String, Object>> create(@RequestBody UserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a user", description = "Updates an existing user record by ID")
    public ResponseEntity<Map<String, Object>> update(@Parameter(description = "User ID") @PathVariable String id, @RequestBody UserRequest request) {
        return ResponseEntity.ok(userService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a user", description = "Deletes a user record by ID")
    public ResponseEntity<Void> delete(@Parameter(description = "User ID") @PathVariable String id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/role")
    @Operation(summary = "Change user role", description = "Updates the role assigned to a user")
    public ResponseEntity<Map<String, Object>> changeRole(
            @Parameter(description = "User ID") @PathVariable String id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(userService.changeRole(id, body.get("role")));
    }

    @PostMapping("/{id}/reset-password")
    @Operation(summary = "Reset user password", description = "Triggers a password reset for the specified user")
    public ResponseEntity<Map<String, Object>> resetPassword(@Parameter(description = "User ID") @PathVariable String id) {
        return ResponseEntity.ok(userService.resetPassword(id));
    }
}
