package com.mstcc.friendshipms.controllers;

import com.mstcc.friendshipms.entities.Friendship;
import com.mstcc.friendshipms.services.FriendshipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/friendships")
public class FriendshipController {

    private final FriendshipService friendshipService;

    @Autowired
    public FriendshipController(FriendshipService friendshipService) {
        this.friendshipService = friendshipService;
    }

    @PostMapping
    public ResponseEntity<Friendship> createFriendship(@RequestBody Friendship friendship) {
        Friendship savedFriendship = friendshipService.createAndValidateFriendship(friendship);
        if (savedFriendship == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(savedFriendship);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Friendship> getFriendshipById(@PathVariable Long id) {
        return friendshipService.getFriendshipById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<Friendship>> getAllFriendships() {
        List<Friendship> friendships = friendshipService.findAllFriendships();
        return ResponseEntity.ok(friendships);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Friendship> updateFriendship(@PathVariable Long id, @RequestBody Friendship friendshipDetails) {
        return friendshipService.updateAndValidateFriendship(id, friendshipDetails)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFriendship(@PathVariable Long id) {
        friendshipService.deleteFriendship(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Friendship>> getFriendshipsByUserId(@PathVariable Long userId) {
        List<Friendship> friendships = friendshipService.findFriendshipsByUserId(userId);
        if (friendships.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(friendships);
    }
}