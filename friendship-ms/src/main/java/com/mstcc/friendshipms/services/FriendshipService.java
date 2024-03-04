package com.mstcc.friendshipms.services;

import com.mstcc.friendshipms.entities.Friendship;
import com.mstcc.friendshipms.feignclients.UserFeignClient;
import com.mstcc.friendshipms.repositories.FriendshipRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final UserFeignClient userFeignClient; // Injeção do UserFeignClient

    @Autowired
    public FriendshipService(FriendshipRepository friendshipRepository, UserFeignClient userFeignClient) {
        this.friendshipRepository = friendshipRepository;
        this.userFeignClient = userFeignClient;
    }

    public Friendship saveFriendship(Friendship friendship) {
        return friendshipRepository.save(friendship);
    }

    public Optional<Friendship> getFriendshipById(Long id) {
        return friendshipRepository.findById(id);
    }

    public List<Friendship> findAllFriendships() {
        return friendshipRepository.findAll();
    }

    public void deleteFriendship(Long id) {
        friendshipRepository.deleteById(id);
    }

    public Optional<Friendship> updateFriendship(Long id, Friendship friendshipDetails) {
        return friendshipRepository.findById(id).map(friendship -> {
            friendship.setStatus(friendshipDetails.getStatus());
            return friendshipRepository.save(friendship);
        });
    }

    public boolean validateUsersExistence(Long userId1, Long userId2) {
        try {
            ResponseEntity<?> user1Response = userFeignClient.getUserById(userId1);
            ResponseEntity<?> user2Response = userFeignClient.getUserById(userId2);

            return user1Response.getStatusCode().is2xxSuccessful() && user2Response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }
}