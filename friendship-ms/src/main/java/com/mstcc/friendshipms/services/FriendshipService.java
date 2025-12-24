package com.mstcc.friendshipms.services;

import com.mstcc.friendshipms.dto.UserDTO;
import com.mstcc.friendshipms.entities.Friendship;
import com.mstcc.friendshipms.repositories.FriendshipRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Service for managing Friendship entities with async validation
 * and parallel processing capabilities
 */
@Service
@Transactional(readOnly = true)
public class FriendshipService {

    private static final Logger logger = LoggerFactory.getLogger(FriendshipService.class);
    private static final int VALIDATION_TIMEOUT_SECONDS = 5;

    private final FriendshipRepository friendshipRepository;
    private final FriendshipAsyncHelper asyncHelper;

    public FriendshipService(FriendshipRepository friendshipRepository, 
                            FriendshipAsyncHelper asyncHelper) {
        this.friendshipRepository = friendshipRepository;
        this.asyncHelper = asyncHelper;
    }

    /**
     * Creates a friendship with parallel validation of both users
     * @param friendship the friendship entity to create
     * @return the saved friendship entity
     * @throws RuntimeException if validation fails
     */
    @Transactional
    @CacheEvict(value = {"friendships", "userFriendships"}, allEntries = true)
    public Friendship createAndValidateFriendship(Friendship friendship) {
        long startTime = System.currentTimeMillis();
        logger.info("Creating friendship - userId1: {}, userId2: {}, status: {}", 
                   friendship.getUserId1(), friendship.getUserId2(), friendship.getStatus());
        
        // Validate that users are different
        if (friendship.getUserId1().equals(friendship.getUserId2())) {
            logger.error("Cannot create friendship with same user: userId={}", friendship.getUserId1());
            throw new IllegalArgumentException("Cannot create friendship with yourself");
        }
        
        try {
            // Parallel validation of both users
            CompletableFuture<UserDTO> user1Future = asyncHelper.getUserAsync(friendship.getUserId1());
            CompletableFuture<UserDTO> user2Future = asyncHelper.getUserAsync(friendship.getUserId2());
            
            // Wait for both validations with timeout
            CompletableFuture.allOf(user1Future, user2Future)
                .get(VALIDATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
            friendship.setCreatedAt(LocalDateTime.now());
            Friendship savedFriendship = friendshipRepository.save(friendship);
            
            long duration = System.currentTimeMillis() - startTime;
            logger.info("Friendship created successfully in {}ms - friendshipId: {}", 
                       duration, savedFriendship.getId());
            
            return savedFriendship;
            
        } catch (TimeoutException e) {
            logger.error("Validation timeout after {}s", VALIDATION_TIMEOUT_SECONDS);
            throw new RuntimeException("Validation timeout: external services took too long", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Validation interrupted");
            throw new RuntimeException("Validation interrupted", e);
        } catch (ExecutionException e) {
            logger.error("Validation failed: {}", e.getCause().getMessage());
            throw new RuntimeException("Validation failed: " + e.getCause().getMessage(), e);
        }
    }

    /**
     * Retrieves a friendship by ID
     * @param id friendship ID
     * @return optional containing friendship if found
     */
    @Cacheable(value = "friendships", key = "#id")
    public Optional<Friendship> getFriendshipById(Long id) {
        logger.info("Fetching friendship by id: {}", id);
        return friendshipRepository.findById(id);
    }

    /**
     * Retrieves all friendships for a user
     * @param userId user ID
     * @return list of friendships involving the user
     */
    @Cacheable(value = "userFriendships", key = "#userId")
    public List<Friendship> findFriendshipsByUserId(Long userId) {
        logger.info("Fetching friendships for userId: {}", userId);
        long startTime = System.currentTimeMillis();
        
        // Validate user exists first
        try {
            asyncHelper.getUserAsync(userId)
                .get(VALIDATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.error("User not found: userId={}", userId);
            throw new RuntimeException("User not found: " + userId, e);
        }
        
        List<Friendship> friendships = friendshipRepository.findByUserId1OrUserId2(userId, userId);
        
        long duration = System.currentTimeMillis() - startTime;
        logger.info("Friendships fetched in {}ms - userId: {}, count: {}", 
                   duration, userId, friendships.size());
        
        return friendships;
    }

    /**
     * Updates a friendship with parallel validation
     * @param id friendship ID
     * @param friendshipDetails updated friendship details
     * @return optional containing updated friendship
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "friendships", key = "#id"),
        @CacheEvict(value = "userFriendships", allEntries = true)
    })
    public Optional<Friendship> updateAndValidateFriendship(Long id, Friendship friendshipDetails) {
        logger.info("Updating friendship - id: {}", id);
        
        // Validate that users are different
        if (friendshipDetails.getUserId1().equals(friendshipDetails.getUserId2())) {
            logger.error("Cannot update friendship with same user: userId={}", friendshipDetails.getUserId1());
            throw new IllegalArgumentException("Cannot create friendship with yourself");
        }
        
        return friendshipRepository.findById(id).map(friendship -> {
            long startTime = System.currentTimeMillis();
            
            try {
                CompletableFuture<UserDTO> user1Future = asyncHelper.getUserAsync(friendshipDetails.getUserId1());
                CompletableFuture<UserDTO> user2Future = asyncHelper.getUserAsync(friendshipDetails.getUserId2());

                CompletableFuture.allOf(user1Future, user2Future)
                    .get(VALIDATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                
                friendship.setUserId1(friendshipDetails.getUserId1());
                friendship.setUserId2(friendshipDetails.getUserId2());
                friendship.setStatus(friendshipDetails.getStatus());
                
                Friendship updated = friendshipRepository.save(friendship);
                
                long duration = System.currentTimeMillis() - startTime;
                logger.info("Friendship updated successfully in {}ms", duration);
                
                return updated;
                
            } catch (Exception e) {
                logger.error("Failed to update friendship: {}", e.getMessage());
                throw new RuntimeException("Failed to update friendship: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Deletes a friendship
     * @param id friendship ID
     */
    @Transactional
    @CacheEvict(value = {"friendships", "userFriendships"}, allEntries = true)
    public void deleteFriendship(Long id) {
        logger.info("Deleting friendship - id: {}", id);
        
        if (!friendshipRepository.existsById(id)) {
            logger.warn("Friendship not found for deletion - id: {}", id);
            throw new IllegalArgumentException("Friendship not found: " + id);
        }
        
        friendshipRepository.deleteById(id);
        logger.info("Friendship deleted successfully - id: {}", id);
    }

    /**
     * Retrieves all friendships
     * @return list of all friendships
     */
    public List<Friendship> findAllFriendships() {
        logger.info("Fetching all friendships");
        List<Friendship> friendships = friendshipRepository.findAll();
        logger.info("Returning {} friendships", friendships.size());
        return friendships;
    }
}