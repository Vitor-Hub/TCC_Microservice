package com.mstcc.monolith.user.controller;

import com.mstcc.monolith.user.dto.UserCreateDTO;
import com.mstcc.monolith.user.dto.UserResponseDTO;
import com.mstcc.monolith.user.entity.User;
import com.mstcc.monolith.user.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for User operations.
 *
 * <p>The request path prefix is {@code /user-ms/api/users} so that the K6 load
 * test script — which was written targeting the API Gateway of the microservices
 * stack — can run against the monolith without any modifications. The gateway
 * routes {@code /user-ms/**} to {@code user-ms:18081/api/**}; the monolith
 * controller mirrors that prefix directly.
 *
 * <p>All exception handling is delegated to
 * {@link com.mstcc.monolith.exception.GlobalExceptionHandler} (SRP).
 */
@RestController
@RequestMapping("/user-ms/api/users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    /**
     * Constructs the controller with its required service dependency.
     *
     * @param userService the user business logic service
     */
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
        logger.info("GET /user-ms/api/users - Fetching all users");
        List<UserResponseDTO> users = userService.findAllUsers().stream()
                .map(UserResponseDTO::from)
                .toList();
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
        logger.info("GET /user-ms/api/users/{} - Fetching user by id", id);
        return userService.findUserById(id)
                .map(user -> ResponseEntity.ok(UserResponseDTO.from(user)))
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
        logger.info("GET /user-ms/api/users/username/{}", username);
        return userService.findByUsername(username)
                .map(user -> ResponseEntity.ok(UserResponseDTO.from(user)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Creates a new user.
     *
     * @param userDto validated request body
     * @return 201 with created user DTO
     */
    @PostMapping
    public ResponseEntity<UserResponseDTO> createUser(@Valid @RequestBody UserCreateDTO userDto) {
        logger.info("POST /user-ms/api/users - Creating new user: {}", userDto.getUsername());
        User savedUser = userService.saveUser(userDto.toEntity());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponseDTO.from(savedUser));
    }

    /**
     * Updates an existing user.
     *
     * @param id      user ID
     * @param userDto validated request body with new values
     * @return 200 with updated user DTO, or 404 if not found
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserCreateDTO userDto) {
        logger.info("PUT /user-ms/api/users/{} - Updating user", id);
        return userService.updateUser(id, userDto.toEntity())
                .map(user -> ResponseEntity.ok(UserResponseDTO.from(user)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Deletes a user.
     *
     * @param id user ID
     * @return 204 if deleted
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        logger.info("DELETE /user-ms/api/users/{} - Deleting user", id);
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Lightweight existence check for cross-domain validation within the monolith.
     * Kept for API surface parity with the microservices gateway.
     *
     * @param id user ID
     * @return 200 with true if the user exists, false otherwise
     */
    @GetMapping("/{id}/exists")
    public ResponseEntity<Boolean> userExists(@PathVariable Long id) {
        return ResponseEntity.ok(userService.existsById(id));
    }
}
