package com.mstcc.userms.controllers;

import com.mstcc.userms.dto.UserCreateDTO;
import com.mstcc.userms.entities.User;
import com.mstcc.userms.services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for User operations
 * Provides CRUD endpoints for user management
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
     * Retrieves all users
     * @return list of all users
     */
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        logger.info("GET /api/users - Fetching all users");
        List<User> users = userService.findAllUsers();
        logger.info("Returning {} users", users.size());
        return ResponseEntity.ok(users);
    }

    /**
     * Retrieves a user by ID
     * @param id user ID
     * @return user if found, 404 otherwise
     */
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        logger.info("GET /api/users/{} - Fetching user by id", id);
        return userService.findUserById(id)
                .map(user -> {
                    logger.info("User found: id={}, username={}", user.getId(), user.getUsername());
                    return ResponseEntity.ok(user);
                })
                .orElseGet(() -> {
                    logger.warn("  User not found: id={}", id);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * Retrieves a user by username
     * @param username the username
     * @return user if found, 404 otherwise
     */
    @GetMapping("/username/{username}")
    public ResponseEntity<User> getUserByUsername(@PathVariable String username) {
        logger.info("GET /api/users/username/{} - Fetching user by username", username);
        return userService.findByUsername(username)
                .map(ResponseEntity::ok)
                .orElseGet(() -> {
                    logger.warn("  User not found: username={}", username);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * Creates a new user
     * @param userDto user data
     * @return created user with 201 status
     */
    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody UserCreateDTO userDto) {
        logger.info("POST /api/users - Creating new user: {}", userDto.getName());
        
        try {
            User user = userDto.toEntity();
            User savedUser = userService.saveUser(user);
            logger.info("User created: id={}", savedUser.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
        } catch (IllegalArgumentException e) {
            logger.error("  Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Updates an existing user
     * @param id user ID
     * @param userDto updated user data
     * @return updated user or 404 if not found
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody UserCreateDTO userDto) {
        logger.info("PUT /api/users/{} - Updating user", id);
        
        try {
            User userDetails = userDto.toEntity();
            return userService.updateUser(id, userDetails)
                    .map(user -> {
                        logger.info("User updated: id={}", id);
                        return ResponseEntity.ok(user);
                    })
                    .orElseGet(() -> {
                        logger.warn("  User not found for update: id={}", id);
                        return ResponseEntity.notFound().build();
                    });
        } catch (IllegalArgumentException e) {
            logger.error("  Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Deletes a user
     * @param id user ID
     * @return 204 if successful, 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        logger.info("DELETE /api/users/{} - Deleting user", id);
        
        try {
            userService.deleteUser(id);
            logger.info("User deleted: id={}", id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            logger.warn("  {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Checks if a user exists
     * @param id user ID
     * @return 200 with boolean, or 200 with false if not found
     */
    @GetMapping("/{id}/exists")
    public ResponseEntity<Boolean> userExists(@PathVariable Long id) {
        logger.info("GET /api/users/{}/exists - Checking user existence", id);
        boolean exists = userService.existsById(id);
        return ResponseEntity.ok(exists);
    }
}