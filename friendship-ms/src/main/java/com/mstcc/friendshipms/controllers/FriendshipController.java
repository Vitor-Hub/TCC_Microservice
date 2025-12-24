package com.mstcc.friendshipms.controllers;

import com.mstcc.friendshipms.entities.Friendship;
import com.mstcc.friendshipms.services.FriendshipService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Friendship operations
 * Provides CRUD endpoints for friendship management
 */
@RestController
@RequestMapping("/api/friendships")
public class FriendshipController {

    private static final Logger logger = LoggerFactory.getLogger(FriendshipController.class);

    private final FriendshipService friendshipService;

    public FriendshipController(FriendshipService friendshipService) {
        this.friendshipService = friendshipService;
    }

    /**
     * Retrieves all friendships
     * @return list of all friendships
     */
    @GetMapping
    public ResponseEntity<List<Friendship>> getAllFriendships() {
        logger.info("GET /api/friendships - Fetching all friendships");
        List<Friendship> friendships = friendshipService.findAllFriendships();
        logger.info("Returning {} friendships", friendships.size());
        return ResponseEntity.ok(friendships);
    }

    /**
     * Retrieves a friendship by ID
     * @param id friendship ID
     * @return friendship if found, 404 otherwise
     */
    @GetMapping("/{id}")
    public ResponseEntity<Friendship> getFriendshipById(@PathVariable Long id) {
        logger.info("GET /api/friendships/{} - Fetching friendship by id", id);
        return friendshipService.getFriendshipById(id)
                .map(friendship -> {
                    logger.info("Friendship found: id={}", id);
                    return ResponseEntity.ok(friendship);
                })
                .orElseGet(() -> {
                    logger.warn("Friendship not found: id={}", id);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * Retrieves all friendships for a user
     * @param userId user ID
     * @return list of user's friendships
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getFriendshipsByUserId(@PathVariable Long userId) {
        logger.info("GET /api/friendships/user/{} - Fetching friendships by user", userId);
        
        try {
            List<Friendship> friendships = friendshipService.findFriendshipsByUserId(userId);
            
            if (friendships.isEmpty()) {
                logger.info("No friendships found for userId: {}", userId);
                return ResponseEntity.ok(friendships); // Return empty list instead of 404
            }
            
            logger.info("Returning {} friendships for userId: {}", friendships.size(), userId);
            return ResponseEntity.ok(friendships);
        } catch (RuntimeException e) {
            logger.error("Error fetching friendships for userId: {}", userId);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Creates a new friendship
     * @param friendship friendship data
     * @return created friendship with 201 status
     */
    @PostMapping
    public ResponseEntity<?> createFriendship(@RequestBody Friendship friendship) {
        logger.info("POST /api/friendships - Creating new friendship");
        
        try {
            Friendship savedFriendship = friendshipService.createAndValidateFriendship(friendship);
            logger.info("Friendship created: id={}", savedFriendship.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(savedFriendship);
        } catch (IllegalArgumentException e) {
            logger.error("Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            logger.error("Error creating friendship: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Updates an existing friendship
     * @param id friendship ID
     * @param friendshipDetails updated friendship data
     * @return updated friendship or 404 if not found
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateFriendship(
            @PathVariable Long id, 
            @RequestBody Friendship friendshipDetails) {
        logger.info("PUT /api/friendships/{} - Updating friendship", id);
        
        try {
            return friendshipService.updateAndValidateFriendship(id, friendshipDetails)
                    .map(friendship -> {
                        logger.info("Friendship updated: id={}", id);
                        return ResponseEntity.ok(friendship);
                    })
                    .orElseGet(() -> {
                        logger.warn("Friendship not found for update: id={}", id);
                        return ResponseEntity.notFound().build();
                    });
        } catch (IllegalArgumentException e) {
            logger.error("Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            logger.error("Error updating friendship: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Deletes a friendship
     * @param id friendship ID
     * @return 204 if successful, 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFriendship(@PathVariable Long id) {
        logger.info("DELETE /api/friendships/{} - Deleting friendship", id);
        
        try {
            friendshipService.deleteFriendship(id);
            logger.info("Friendship deleted: id={}", id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            logger.warn("{}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}