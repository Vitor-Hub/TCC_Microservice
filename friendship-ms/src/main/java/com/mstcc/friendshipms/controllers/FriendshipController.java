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
 * REST controller for Friendship operations.
 *
 * <p>Applying SRP: this controller is responsible only for routing and delegating to
 * {@link FriendshipService}. All exception formatting is handled by
 * {@code GlobalExceptionHandler} — no try-catch blocks here.
 *
 * <p>Applying DIP: the controller depends on the {@link FriendshipService} abstraction,
 * not on the concrete {@code FriendshipServiceImpl}.
 */
@RestController
@RequestMapping("/api/friendships")
public class FriendshipController {

    private static final Logger logger = LoggerFactory.getLogger(FriendshipController.class);

    private final FriendshipService friendshipService;

    /**
     * Constructs the controller with its required service dependency.
     *
     * @param friendshipService the friendship business logic abstraction
     */
    public FriendshipController(FriendshipService friendshipService) {
        this.friendshipService = friendshipService;
    }

    /**
     * Retrieves all friendships.
     *
     * @return 200 with list of all friendships
     */
    @GetMapping
    public ResponseEntity<List<Friendship>> getAllFriendships() {
        logger.info("GET /api/friendships - Fetching all friendships");
        return ResponseEntity.ok(friendshipService.findAllFriendships());
    }

    /**
     * Retrieves a friendship by ID.
     *
     * @param id friendship ID
     * @return 200 with the friendship, or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<Friendship> getFriendshipById(@PathVariable Long id) {
        logger.info("GET /api/friendships/{} - Fetching friendship by id", id);
        return friendshipService.getFriendshipById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Retrieves all friendships for a given user.
     * Exceptions are propagated to {@code GlobalExceptionHandler}.
     *
     * @param userId user ID
     * @return 200 with list of friendships (may be empty)
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Friendship>> getFriendshipsByUserId(@PathVariable Long userId) {
        logger.info("GET /api/friendships/user/{} - Fetching friendships by user", userId);
        return ResponseEntity.ok(friendshipService.findFriendshipsByUserId(userId));
    }

    /**
     * Creates a new friendship.
     * Exceptions are propagated to {@code GlobalExceptionHandler}.
     *
     * @param friendship friendship data
     * @return 201 with the created friendship
     */
    @PostMapping
    public ResponseEntity<Friendship> createFriendship(@RequestBody Friendship friendship) {
        logger.info("POST /api/friendships - Creating new friendship");
        Friendship savedFriendship = friendshipService.createAndValidateFriendship(friendship);
        logger.info("Friendship created: id={}", savedFriendship.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(savedFriendship);
    }

    /**
     * Updates an existing friendship.
     * Exceptions are propagated to {@code GlobalExceptionHandler}.
     *
     * @param id                friendship ID
     * @param friendshipDetails updated friendship data
     * @return 200 with the updated friendship, or 404 if not found
     */
    @PutMapping("/{id}")
    public ResponseEntity<Friendship> updateFriendship(
            @PathVariable Long id,
            @RequestBody Friendship friendshipDetails) {
        logger.info("PUT /api/friendships/{} - Updating friendship", id);
        return friendshipService.updateAndValidateFriendship(id, friendshipDetails)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Deletes a friendship.
     * Exceptions are propagated to {@code GlobalExceptionHandler}.
     *
     * @param id friendship ID
     * @return 204 No Content, or 400 if not found (via GlobalExceptionHandler)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFriendship(@PathVariable Long id) {
        logger.info("DELETE /api/friendships/{} - Deleting friendship", id);
        friendshipService.deleteFriendship(id);
        return ResponseEntity.noContent().build();
    }
}
