package com.mstcc.monolith.friendship.controller;

import com.mstcc.monolith.friendship.entity.Friendship;
import com.mstcc.monolith.friendship.service.FriendshipService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Friendship operations.
 *
 * <p>Path prefix {@code /friendship-ms/api/friendships} mirrors the API Gateway
 * route so that K6 scripts need no changes when switching between stacks.
 */
@RestController
@RequestMapping("/friendship-ms/api/friendships")
public class FriendshipController {

    private static final Logger logger = LoggerFactory.getLogger(FriendshipController.class);

    private final FriendshipService friendshipService;

    /**
     * Constructs the controller with its required service dependency.
     *
     * @param friendshipService the friendship business logic service
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
        logger.info("GET /friendship-ms/api/friendships");
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
        logger.info("GET /friendship-ms/api/friendships/{}", id);
        return friendshipService.getFriendshipById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Retrieves all friendships for a given user.
     *
     * @param userId user ID
     * @return 200 with list of friendships (may be empty)
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Friendship>> getFriendshipsByUserId(@PathVariable Long userId) {
        logger.info("GET /friendship-ms/api/friendships/user/{}", userId);
        return ResponseEntity.ok(friendshipService.findFriendshipsByUserId(userId));
    }

    /**
     * Creates a new friendship.
     *
     * @param friendship friendship data
     * @return 201 with the created friendship
     */
    @PostMapping
    public ResponseEntity<Friendship> createFriendship(@RequestBody Friendship friendship) {
        logger.info("POST /friendship-ms/api/friendships");
        Friendship saved = friendshipService.createAndValidateFriendship(friendship);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * Updates an existing friendship.
     *
     * @param id                friendship ID
     * @param friendshipDetails updated friendship data
     * @return 200 with the updated friendship, or 404 if not found
     */
    @PutMapping("/{id}")
    public ResponseEntity<Friendship> updateFriendship(
            @PathVariable Long id,
            @RequestBody Friendship friendshipDetails) {
        logger.info("PUT /friendship-ms/api/friendships/{}", id);
        return friendshipService.updateAndValidateFriendship(id, friendshipDetails)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Deletes a friendship.
     *
     * @param id friendship ID
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFriendship(@PathVariable Long id) {
        logger.info("DELETE /friendship-ms/api/friendships/{}", id);
        friendshipService.deleteFriendship(id);
        return ResponseEntity.noContent().build();
    }
}
