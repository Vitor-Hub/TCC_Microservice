package com.mstcc.friendshipms.controllers;

import com.mstcc.friendshipms.entities.Friendship;
import com.mstcc.friendshipms.services.FriendshipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
        Friendship savedFriendship = friendshipService.saveFriendship(friendship);
        return ResponseEntity.ok(savedFriendship);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Friendship> getFriendshipById(@PathVariable Long id) {
        return friendshipService.getFriendshipById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFriendship(@PathVariable Long id) {
        friendshipService.deleteFriendship(id);
        return ResponseEntity.ok().build();
    }
}