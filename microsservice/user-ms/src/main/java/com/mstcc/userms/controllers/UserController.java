package com.mstcc.userms.controllers;

import com.mstcc.userms.dto.UserCreateDTO;
import com.mstcc.userms.dto.UserResponseDTO;
import com.mstcc.userms.entities.User;
import com.mstcc.userms.services.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for User operations.
 * Delegates all business logic to {@link UserService} (DIP).
 * Exception handling is centralised in {@code GlobalExceptionHandler} (SRP) —
 * no try/catch blocks are needed here.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Returns all registered users.
     *
     * @return 200 with list of user response DTOs
     */
    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        logger.info("GET /api/users - Fetching all users");
        List<UserResponseDTO> users = userService.findAllUsers().stream()
                .map(UserResponseDTO::from)
                .toList();
        logger.info("Returning {} users", users.size());
        return ResponseEntity.ok(users);
    }

    /**
     * Returns a single user by ID.
     *
     * @param id user ID
     * @return 200 with user DTO, or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long id) {
        logger.info("GET /api/users/{} - Fetching user by id", id);
        return userService.findUserById(id)
                .map(user -> {
                    logger.info("User found: id={}, username={}", user.getId(), user.getUsername());
                    return ResponseEntity.ok(UserResponseDTO.from(user));
                })
                .orElseGet(() -> {
                    logger.warn("User not found: id={}", id);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * Returns a single user by username.
     *
     * @param username the username to look up
     * @return 200 with user DTO, or 404 if not found
     */
    @GetMapping("/username/{username}")
    public ResponseEntity<UserResponseDTO> getUserByUsername(@PathVariable String username) {
        logger.info("GET /api/users/username/{} - Fetching user by username", username);
        return userService.findByUsername(username)
                .map(user -> ResponseEntity.ok(UserResponseDTO.from(user)))
                .orElseGet(() -> {
                    logger.warn("User not found: username={}", username);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * Creates a new user.
     * {@code @Valid} triggers Bean Validation before the method body executes (SRP).
     *
     * @param userDto validated request body
     * @return 201 with created user DTO
     */
    @PostMapping
    public ResponseEntity<UserResponseDTO> createUser(@Valid @RequestBody UserCreateDTO userDto) {
        logger.info("POST /api/users - Creating new user: {}", userDto.getUsername());
        User savedUser = userService.saveUser(userDto.toEntity());
        logger.info("User created: id={}", savedUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponseDTO.from(savedUser));
    }

    /**
     * Updates an existing user.
     * {@code @Valid} triggers Bean Validation before the method body executes (SRP).
     *
     * @param id      user ID
     * @param userDto validated request body with new values
     * @return 200 with updated user DTO, or 404 if not found
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserCreateDTO userDto) {
        logger.info("PUT /api/users/{} - Updating user", id);
        return userService.updateUser(id, userDto.toEntity())
                .map(user -> {
                    logger.info("User updated: id={}", id);
                    return ResponseEntity.ok(UserResponseDTO.from(user));
                })
                .orElseGet(() -> {
                    logger.warn("User not found for update: id={}", id);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * Deletes a user.
     *
     * @param id user ID
     * @return 204 if deleted, 404 if not found (via GlobalExceptionHandler)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        logger.info("DELETE /api/users/{} - Deleting user", id);
        userService.deleteUser(id);
        logger.info("User deleted: id={}", id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Lightweight existence check used by other microservices for cross-service validation.
     *
     * @param id user ID
     * @return 200 with true if the user exists, false otherwise
     */
    @GetMapping("/{id}/exists")
    public ResponseEntity<Boolean> userExists(@PathVariable Long id) {
        logger.info("GET /api/users/{}/exists - Checking user existence", id);
        return ResponseEntity.ok(userService.existsById(id));
    }
}