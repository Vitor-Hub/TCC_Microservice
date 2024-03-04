package com.mstcc.friendshipms.controllers;

import com.mstcc.friendshipms.dto.UserDTO;
import com.mstcc.friendshipms.entities.Friendship;
import com.mstcc.friendshipms.feignclients.UserFeignClient;
import com.mstcc.friendshipms.services.FriendshipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/friendships")
public class FriendshipController {

    private final FriendshipService friendshipService;
    private final UserFeignClient userFeignClient;

    @Autowired
    public FriendshipController(FriendshipService friendshipService, UserFeignClient userFeignClient) {
        this.friendshipService = friendshipService;
        this.userFeignClient = userFeignClient;
    }

    @PostMapping
    public ResponseEntity<Friendship> createFriendship(@RequestBody Friendship friendship) {
        validateUsersExistence(friendship);
        Friendship savedFriendship = friendshipService.saveFriendship(friendship);
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
        return friendshipService.getFriendshipById(id).map(friendship -> {
            validateUsersExistence(friendshipDetails);
            friendship.setStatus(friendshipDetails.getStatus());
            Friendship updatedFriendship = friendshipService.saveFriendship(friendship);
            return ResponseEntity.ok(updatedFriendship);
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFriendship(@PathVariable Long id) {
        friendshipService.deleteFriendship(id);
        return ResponseEntity.noContent().build();
    }

    private void validateUsersExistence(Friendship friendship) {
        ResponseEntity<UserDTO> userResponse1 = userFeignClient.getUserById(friendship.getUserId1());
        ResponseEntity<UserDTO> userResponse2 = userFeignClient.getUserById(friendship.getUserId2());
        if (!userResponse1.getStatusCode().is2xxSuccessful() || !userResponse2.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("One or both users not found");
        }
    }
}