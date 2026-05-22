package com.mstcc.likesms.services.impl;

import com.mstcc.likesms.dto.CommentDTO;
import com.mstcc.likesms.dto.PostDTO;
import com.mstcc.likesms.dto.UserDTO;
import com.mstcc.likesms.entities.Like;
import com.mstcc.likesms.exceptions.LikeValidationException;
import com.mstcc.likesms.repositories.LikeRepository;
import com.mstcc.likesms.services.LikeAsyncHelper;
import com.mstcc.likesms.services.LikeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Default implementation of {@link LikeService}.
 *
 * <p>Handles like persistence and orchestrates parallel upstream validation
 * via {@link LikeAsyncHelper} (user-ms, post-ms, comment-ms).
 *
 * <p>Applying SRP: this class is responsible solely for like business logic.
 * Exception formatting is delegated to {@code GlobalExceptionHandler}.
 */
@Service
@Transactional(readOnly = true)
public class LikeServiceImpl implements LikeService {

    private static final Logger logger = LoggerFactory.getLogger(LikeServiceImpl.class);
    private static final int VALIDATION_TIMEOUT_SECONDS = 5;

    private final LikeRepository likeRepository;
    private final LikeAsyncHelper asyncHelper;

    /**
     * Constructs the service with its required dependencies.
     *
     * @param likeRepository JPA repository for like persistence
     * @param asyncHelper    helper that issues parallel async calls to upstream services
     */
    public LikeServiceImpl(LikeRepository likeRepository,
                           LikeAsyncHelper asyncHelper) {
        this.likeRepository = likeRepository;
        this.asyncHelper = asyncHelper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    @CacheEvict(value = {"likes", "postLikes", "userLikes"}, allEntries = true)
    public Like createAndValidateLike(Like like) {
        long startTime = System.currentTimeMillis();
        logger.info("Creating like - userId: {}, postId: {}, commentId: {}",
                like.getUserId(), like.getPostId(), like.getCommentId());

        if (like.getPostId() == null && like.getCommentId() == null) {
            throw new IllegalArgumentException("Like must reference a post or a comment");
        }

        try {
            // User validation is always mandatory
            CompletableFuture<UserDTO> userFuture = asyncHelper.getUserAsync(like.getUserId());

            // Post validation only when postId is present
            CompletableFuture<PostDTO> postFuture = like.getPostId() != null
                    ? asyncHelper.getPostAsync(like.getPostId())
                    : CompletableFuture.completedFuture(null);

            // Comment validation only when commentId is present
            CompletableFuture<CommentDTO> commentFuture = like.getCommentId() != null
                    ? asyncHelper.getCommentAsync(like.getCommentId())
                    : CompletableFuture.completedFuture(null);

            CompletableFuture.allOf(userFuture, postFuture, commentFuture)
                    .get(VALIDATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            Like savedLike = likeRepository.save(like);

            long duration = System.currentTimeMillis() - startTime;
            logger.info("Like created successfully in {}ms - likeId: {}", duration, savedLike.getId());

            return savedLike;

        } catch (TimeoutException e) {
            logger.error("Validation timeout after {}s", VALIDATION_TIMEOUT_SECONDS);
            throw new LikeValidationException("Validation timeout: external services took too long", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Validation interrupted");
            throw new LikeValidationException("Validation interrupted", e);
        } catch (ExecutionException e) {
            logger.error("Validation failed: {}", e.getCause().getMessage());
            throw new LikeValidationException("Validation failed: " + e.getCause().getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    @CacheEvict(value = {"likes", "postLikes", "userLikes"}, allEntries = true)
    public Optional<Like> updateAndValidateLike(Long id, Like likeDetails) {
        logger.info("Updating like - id: {}", id);

        return likeRepository.findById(id).map(like -> {
            long startTime = System.currentTimeMillis();

            try {
                CompletableFuture<UserDTO> userFuture = asyncHelper.getUserAsync(likeDetails.getUserId());
                CompletableFuture<PostDTO> postFuture = asyncHelper.getPostAsync(likeDetails.getPostId());

                CompletableFuture.allOf(userFuture, postFuture)
                        .get(VALIDATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                like.setUserId(likeDetails.getUserId());
                like.setPostId(likeDetails.getPostId());
                like.setCommentId(likeDetails.getCommentId());

                Like updated = likeRepository.save(like);

                long duration = System.currentTimeMillis() - startTime;
                logger.info("Like updated successfully in {}ms", duration);

                return updated;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new LikeValidationException("Validation interrupted", e);
            } catch (ExecutionException | TimeoutException e) {
                logger.error("Failed to update like: {}", e.getMessage());
                throw new LikeValidationException("Failed to update like: " + e.getMessage(), e);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Cacheable(value = "likes", key = "#id")
    public Optional<Like> getLikeById(Long id) {
        return likeRepository.findById(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Cacheable(value = "postLikes", key = "#postId")
    public List<Like> findLikesByPostId(Long postId) {
        return likeRepository.findByPostId(postId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Cacheable(value = "userLikes", key = "#userId")
    public List<Like> findLikesByUserId(Long userId) {
        return likeRepository.findByUserId(userId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Like> findLikesByCommentId(Long commentId) {
        return likeRepository.findByCommentId(commentId);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if no like exists with the given ID
     */
    @Override
    @Transactional
    @CacheEvict(value = {"likes", "postLikes", "userLikes"}, allEntries = true)
    public void deleteLike(Long id) {
        logger.info("Deleting like - id: {}", id);

        if (!likeRepository.existsById(id)) {
            logger.warn("Like not found for deletion - id: {}", id);
            throw new IllegalArgumentException("Like not found: " + id);
        }

        likeRepository.deleteById(id);
        logger.info("Like deleted successfully - id: {}", id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Like> findAllLikes() {
        return likeRepository.findAll();
    }
}
