package com.mstcc.monolith.friendship.service;

import com.mstcc.monolith.exception.ValidationException;
import com.mstcc.monolith.friendship.entity.Friendship;
import com.mstcc.monolith.friendship.repository.FriendshipRepository;
import com.mstcc.monolith.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Business logic for the Friendship domain.
 *
 * <p>ID normalisation invariant: {@code userId1} is always the smaller of the two
 * IDs so that (A,B) and (B,A) resolve to the same database row. This mirrors the
 * behaviour in {@code FriendshipServiceImpl} of the microservices stack exactly.
 *
 * <p>Cross-domain validation (user existence) uses direct calls to
 * {@link UserService} instead of Feign. In the microservices stack two parallel
 * Feign calls are made; in the monolith they are sequential in-process calls.
 */
@Service
@Transactional(readOnly = true)
public class FriendshipService {

    private static final Logger logger = LoggerFactory.getLogger(FriendshipService.class);

    private final FriendshipRepository friendshipRepository;
    private final UserService userService;

    /**
     * Constructs the service with its required dependencies.
     *
     * @param friendshipRepository JPA repository for Friendship persistence
     * @param userService          user domain service for existence validation
     */
    public FriendshipService(FriendshipRepository friendshipRepository,
                             @Lazy UserService userService) {
        this.friendshipRepository = friendshipRepository;
        this.userService = userService;
    }

    /**
     * Creates a friendship after validating both users and checking for duplicates.
     *
     * @param friendship the friendship entity to create
     * @return the saved friendship entity
     * @throws IllegalArgumentException if the users are the same or a friendship already exists
     * @throws ValidationException      if either user does not exist
     */
    @Transactional
    @CacheEvict(value = {"friendships", "userFriendships"}, allEntries = true)
    public Friendship createAndValidateFriendship(Friendship friendship) {
        logger.info("Creating friendship - userId1: {}, userId2: {}, status: {}",
                friendship.getUserId1(), friendship.getUserId2(), friendship.getStatus());

        if (friendship.getUserId1().equals(friendship.getUserId2())) {
            throw new IllegalArgumentException("Cannot create friendship with yourself");
        }

        normaliseUserIdOrder(friendship);

        if (friendshipRepository.existsByUserId1AndUserId2(
                friendship.getUserId1(), friendship.getUserId2())) {
            throw new IllegalArgumentException(
                    "Friendship already exists between users "
                    + friendship.getUserId1() + " and " + friendship.getUserId2());
        }

        if (!userService.existsById(friendship.getUserId1())) {
            throw new ValidationException("User not found: " + friendship.getUserId1());
        }

        if (!userService.existsById(friendship.getUserId2())) {
            throw new ValidationException("User not found: " + friendship.getUserId2());
        }

        friendship.setCreatedAt(LocalDateTime.now());
        Friendship saved = friendshipRepository.save(friendship);
        logger.info("Friendship created successfully - friendshipId: {}", saved.getId());
        return saved;
    }

    /**
     * Retrieves a friendship by ID.
     *
     * @param id friendship ID
     * @return optional containing the friendship if found
     */
    @Cacheable(value = "friendships", key = "#id")
    public Optional<Friendship> getFriendshipById(Long id) {
        logger.info("Fetching friendship by id: {}", id);
        return friendshipRepository.findById(id);
    }

    /**
     * Retrieves all friendships involving a given user.
     *
     * @param userId user ID
     * @return list of friendships (may be empty)
     */
    @Cacheable(value = "userFriendships", key = "#userId")
    public List<Friendship> findFriendshipsByUserId(Long userId) {
        logger.info("Fetching friendships for userId: {}", userId);
        return friendshipRepository.findByUserId1OrUserId2(userId, userId);
    }

    /**
     * Retrieves all friendships.
     *
     * @return list of all friendships
     */
    public List<Friendship> findAllFriendships() {
        logger.info("Fetching all friendships");
        return friendshipRepository.findAll();
    }

    /**
     * Updates a friendship with re-validation of both users.
     *
     * @param id                friendship ID
     * @param friendshipDetails updated friendship data
     * @return optional containing the updated friendship, or empty if not found
     * @throws IllegalArgumentException if the users are the same or the new pair already exists
     * @throws ValidationException      if either user does not exist
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "friendships", key = "#id"),
        @CacheEvict(value = "userFriendships", allEntries = true)
    })
    public Optional<Friendship> updateAndValidateFriendship(Long id, Friendship friendshipDetails) {
        logger.info("Updating friendship - id: {}", id);

        if (friendshipDetails.getUserId1().equals(friendshipDetails.getUserId2())) {
            throw new IllegalArgumentException("Cannot update friendship with yourself");
        }

        normaliseUserIdOrder(friendshipDetails);

        if (friendshipRepository.existsByUserId1AndUserId2(
                friendshipDetails.getUserId1(), friendshipDetails.getUserId2())) {
            friendshipRepository
                    .findByUserId1AndUserId2(friendshipDetails.getUserId1(), friendshipDetails.getUserId2())
                    .filter(owner -> !owner.getId().equals(id))
                    .ifPresent(owner -> {
                        throw new IllegalArgumentException(
                                "Friendship already exists between users "
                                + friendshipDetails.getUserId1() + " and " + friendshipDetails.getUserId2());
                    });
        }

        if (!userService.existsById(friendshipDetails.getUserId1())) {
            throw new ValidationException("User not found: " + friendshipDetails.getUserId1());
        }

        if (!userService.existsById(friendshipDetails.getUserId2())) {
            throw new ValidationException("User not found: " + friendshipDetails.getUserId2());
        }

        return friendshipRepository.findById(id).map(friendship -> {
            friendship.setUserId1(friendshipDetails.getUserId1());
            friendship.setUserId2(friendshipDetails.getUserId2());
            friendship.setStatus(friendshipDetails.getStatus());
            Friendship updated = friendshipRepository.save(friendship);
            logger.info("Friendship updated successfully - id: {}", id);
            return updated;
        });
    }

    /**
     * Deletes a friendship by ID.
     *
     * @param id friendship ID
     * @throws IllegalArgumentException if no friendship exists with that ID
     */
    @Transactional
    @CacheEvict(value = {"friendships", "userFriendships"}, allEntries = true)
    public void deleteFriendship(Long id) {
        logger.info("Deleting friendship - id: {}", id);

        if (!friendshipRepository.existsById(id)) {
            throw new IllegalArgumentException("Friendship not found: " + id);
        }

        friendshipRepository.deleteById(id);
        logger.info("Friendship deleted successfully - id: {}", id);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Ensures {@code userId1} is always less than {@code userId2}.
     * This prevents duplicate rows for (A,B) and (B,A).
     *
     * @param friendship the friendship entity to normalise in place
     */
    private void normaliseUserIdOrder(Friendship friendship) {
        if (friendship.getUserId1() > friendship.getUserId2()) {
            Long tmp = friendship.getUserId1();
            friendship.setUserId1(friendship.getUserId2());
            friendship.setUserId2(tmp);
        }
    }
}
