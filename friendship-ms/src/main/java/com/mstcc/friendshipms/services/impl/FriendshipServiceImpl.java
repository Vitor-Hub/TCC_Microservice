package com.mstcc.friendshipms.services.impl;

import com.mstcc.friendshipms.dto.UserDTO;
import com.mstcc.friendshipms.entities.Friendship;
import com.mstcc.friendshipms.exception.FriendshipValidationException;
import com.mstcc.friendshipms.repositories.FriendshipRepository;
import com.mstcc.friendshipms.services.FriendshipAsyncHelper;
import com.mstcc.friendshipms.services.FriendshipService;
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
 * Default implementation of {@link FriendshipService}.
 *
 * <p>Handles friendship persistence and orchestrates parallel upstream validation
 * via {@link FriendshipAsyncHelper} (user-ms).
 *
 * <p>Applying SRP: this class is responsible solely for friendship business logic.
 * Exception formatting is delegated to {@code GlobalExceptionHandler}.
 *
 * <p>ID normalisation invariant: userId1 is always the smaller of the two IDs,
 * ensuring that (A,B) and (B,A) resolve to the same database row and preventing
 * duplicate friendship entries with swapped user IDs.
 */
@Service
@Transactional(readOnly = true)
public class FriendshipServiceImpl implements FriendshipService {

    private static final Logger logger = LoggerFactory.getLogger(FriendshipServiceImpl.class);
    private static final int VALIDATION_TIMEOUT_SECONDS = 5;

    private final FriendshipRepository friendshipRepository;
    private final FriendshipAsyncHelper asyncHelper;

    /**
     * Constructs the service with its required dependencies.
     *
     * @param friendshipRepository JPA repository for friendship persistence
     * @param asyncHelper          helper that issues parallel async calls to upstream services
     */
    public FriendshipServiceImpl(FriendshipRepository friendshipRepository,
                                 FriendshipAsyncHelper asyncHelper) {
        this.friendshipRepository = friendshipRepository;
        this.asyncHelper = asyncHelper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    @CacheEvict(value = {"friendships", "userFriendships"}, allEntries = true)
    public Friendship createAndValidateFriendship(Friendship friendship) {
        long startTime = System.currentTimeMillis();
        logger.info("Creating friendship - userId1: {}, userId2: {}, status: {}",
                friendship.getUserId1(), friendship.getUserId2(), friendship.getStatus());

        if (friendship.getUserId1().equals(friendship.getUserId2())) {
            logger.error("Cannot create friendship with same user: userId={}", friendship.getUserId1());
            throw new IllegalArgumentException("Cannot create friendship with yourself");
        }

        // Normalise ID order so (A,B) and (B,A) map to the same row,
        // preventing duplicate friendship entries with swapped user IDs.
        normaliseUserIdOrder(friendship);

        if (friendshipRepository.existsByUserId1AndUserId2(
                friendship.getUserId1(), friendship.getUserId2())) {
            logger.warn("Duplicate friendship request - userId1: {}, userId2: {}",
                    friendship.getUserId1(), friendship.getUserId2());
            throw new IllegalArgumentException(
                    "Friendship already exists between users "
                    + friendship.getUserId1() + " and " + friendship.getUserId2());
        }

        try {
            CompletableFuture<UserDTO> user1Future = asyncHelper.getUserAsync(friendship.getUserId1());
            CompletableFuture<UserDTO> user2Future = asyncHelper.getUserAsync(friendship.getUserId2());

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
            throw new FriendshipValidationException("Validation timeout: external services took too long", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Validation interrupted");
            throw new FriendshipValidationException("Validation interrupted", e);
        } catch (ExecutionException e) {
            logger.error("Validation failed: {}", e.getCause().getMessage());
            throw new FriendshipValidationException("Validation failed: " + e.getCause().getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Cacheable(value = "friendships", key = "#id")
    public Optional<Friendship> getFriendshipById(Long id) {
        logger.info("Fetching friendship by id: {}", id);
        return friendshipRepository.findById(id);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Pure read against the local database — no upstream calls.
     * Upstream availability must not affect the ability to list friendships that are already persisted.
     * User existence is implicitly guaranteed by the create path, which validates both users before saving.
     */
    @Override
    @Cacheable(value = "userFriendships", key = "#userId")
    public List<Friendship> findFriendshipsByUserId(Long userId) {
        logger.info("Fetching friendships for userId: {}", userId);
        long startTime = System.currentTimeMillis();

        List<Friendship> friendships = friendshipRepository.findByUserId1OrUserId2(userId, userId);

        long duration = System.currentTimeMillis() - startTime;
        logger.info("Friendships fetched in {}ms - userId: {}, count: {}",
                duration, userId, friendships.size());

        return friendships;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "friendships", key = "#id"),
            @CacheEvict(value = "userFriendships", allEntries = true)
    })
    public Optional<Friendship> updateAndValidateFriendship(Long id, Friendship friendshipDetails) {
        logger.info("Updating friendship - id: {}", id);

        if (friendshipDetails.getUserId1().equals(friendshipDetails.getUserId2())) {
            logger.error("Cannot update friendship with same user: userId={}", friendshipDetails.getUserId1());
            throw new IllegalArgumentException("Cannot update friendship with yourself");
        }

        // Normalise ID order before any lookup or persistence so that (A,B) and (B,A)
        // resolve to the same canonical form, consistent with the create invariant.
        normaliseUserIdOrder(friendshipDetails);

        // Detect collision with an existing friendship for the same pair, but exclude
        // the record being updated — otherwise a no-op update on the same users would
        // always be rejected because the row itself matches the check.
        //
        // The previous implementation used findById(id).ifPresent(...) which silently
        // ignored the collision when the id did not exist (Optional.empty() skips ifPresent),
        // and also had the condition inverted (threw only when IDs were different, which is
        // correct, but swallowed the entire check when findById returned empty).
        // The correct approach: find the owner of the pair and compare its ID to the one
        // being updated. If they differ, it is a collision with a different row.
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

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new FriendshipValidationException("Validation interrupted", e);
            } catch (ExecutionException | TimeoutException e) {
                logger.error("Failed to update friendship: {}", e.getMessage());
                throw new FriendshipValidationException("Failed to update friendship: " + e.getMessage(), e);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
     * {@inheritDoc}
     */
    @Override
    public List<Friendship> findAllFriendships() {
        logger.info("Fetching all friendships");
        List<Friendship> friendships = friendshipRepository.findAll();
        logger.info("Returning {} friendships", friendships.size());
        return friendships;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Ensures userId1 is always less than userId2.
     * This invariant prevents duplicate rows for (A,B) and (B,A).
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
