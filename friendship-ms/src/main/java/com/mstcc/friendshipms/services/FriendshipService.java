package com.mstcc.friendshipms.services;

import com.mstcc.friendshipms.dto.UserDTO;
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
    private final UserFeignClient userFeignClient;

    @Autowired
    public FriendshipService(FriendshipRepository friendshipRepository, UserFeignClient userFeignClient) {
        this.friendshipRepository = friendshipRepository;
        this.userFeignClient = userFeignClient;
    }

    public List<Friendship> findAllFriendships() {
        return friendshipRepository.findAll();
    }

    public Optional<Friendship> getFriendshipById(Long id) {
        return friendshipRepository.findById(id);
    }

    public void deleteFriendship(Long id) {
        friendshipRepository.deleteById(id);
    }

    public Friendship createAndValidateFriendship(Friendship friendship) {
        if (validateUsersExistence(friendship.getUserId1(), friendship.getUserId2())) {
            return friendshipRepository.save(friendship);
        } else {
            return null; // Ou você pode lançar uma exceção específica aqui
        }
    }

    public Optional<Friendship> updateAndValidateFriendship(Long id, Friendship friendshipDetails) {
        return friendshipRepository.findById(id).map(existingFriendship -> {
            if (validateUsersExistence(friendshipDetails.getUserId1(), friendshipDetails.getUserId2())) {
                existingFriendship.setStatus(friendshipDetails.getStatus());
                return Optional.of(friendshipRepository.save(existingFriendship));
            } else {
                return Optional.<Friendship>empty(); // Ou você pode lançar uma exceção específica aqui
            }
        }).orElse(Optional.empty());
    }

    private boolean validateUsersExistence(Long userId1, Long userId2) {
        ResponseEntity<UserDTO> user1Response = userFeignClient.getUserById(userId1);
        ResponseEntity<UserDTO> user2Response = userFeignClient.getUserById(userId2);

        return user1Response.getStatusCode().is2xxSuccessful() && user2Response.getStatusCode().is2xxSuccessful();
    }

    public List<Friendship> findFriendshipsByUserId(Long userId) {
        return friendshipRepository.findByUserId1OrUserId2(userId, userId);
    }
}