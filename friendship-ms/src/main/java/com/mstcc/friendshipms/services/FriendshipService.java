package com.mstcc.friendshipms.services;

import com.mstcc.friendshipms.entities.Friendship;
import com.mstcc.friendshipms.repositories.FriendshipRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;

    @Autowired
    public FriendshipService(FriendshipRepository friendshipRepository) {
        this.friendshipRepository = friendshipRepository;
    }

    public Friendship saveFriendship(Friendship friendship) {
        return friendshipRepository.save(friendship);
    }

    public Optional<Friendship> getFriendshipById(Long id) {
        return friendshipRepository.findById(id);
    }

    public void deleteFriendship(Long id) {
        friendshipRepository.deleteById(id);
    }
}