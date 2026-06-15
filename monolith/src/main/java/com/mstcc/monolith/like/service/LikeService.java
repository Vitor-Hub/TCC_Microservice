package com.mstcc.monolith.like.service;

import com.mstcc.monolith.comment.service.CommentService;
import com.mstcc.monolith.exception.ValidationException;
import com.mstcc.monolith.like.entity.Like;
import com.mstcc.monolith.like.repository.LikeRepository;
import com.mstcc.monolith.post.service.PostService;
import com.mstcc.monolith.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Business logic for the Like domain.
 *
 * <p>In the microservices stack, creating a like triggers up to three parallel
 * Feign calls: user validation, post validation (if postId is set), and comment
 * validation (if commentId is set). In the monolith these are sequential
 * in-process calls. The validation logic and error semantics are identical.
 */
@Service
@Transactional(readOnly = true)
public class LikeService {

    private static final Logger logger = LoggerFactory.getLogger(LikeService.class);

    private final LikeRepository likeRepository;
    private final UserService userService;
    private final PostService postService;
    private final CommentService commentService;

    /**
     * Constructs the service with its required dependencies.
     * All cross-domain services are injected lazily to prevent circular
     * dependency resolution at context startup.
     *
     * @param likeRepository   JPA repository for Like persistence
     * @param userService      user domain service for existence validation
     * @param postService      post domain service for existence validation
     * @param commentService   comment domain service for existence validation
     */
    public LikeService(LikeRepository likeRepository,
                       @Lazy UserService userService,
                       @Lazy PostService postService,
                       @Lazy CommentService commentService) {
        this.likeRepository = likeRepository;
        this.userService = userService;
        this.postService = postService;
        this.commentService = commentService;
    }

    /**
     * Creates a like after validating the referenced user, post (if any), and
     * comment (if any).
     *
     * @param like the like entity to create
     * @return the saved like entity
     * @throws IllegalArgumentException if neither postId nor commentId is set
     * @throws ValidationException      if any referenced entity does not exist
     */
    @Transactional
    @CacheEvict(value = {"likes", "postLikes", "userLikes"}, allEntries = true)
    public Like createAndValidateLike(Like like) {
        logger.info("Creating like - userId: {}, postId: {}, commentId: {}",
                like.getUserId(), like.getPostId(), like.getCommentId());

        if (like.getPostId() == null && like.getCommentId() == null) {
            throw new IllegalArgumentException("Like must reference a post or a comment");
        }

        if (!userService.existsById(like.getUserId())) {
            throw new ValidationException("User not found: " + like.getUserId());
        }

        if (like.getPostId() != null && !postService.existsById(like.getPostId())) {
            throw new ValidationException("Post not found: " + like.getPostId());
        }

        if (like.getCommentId() != null
                && commentService.getCommentById(like.getCommentId()).isEmpty()) {
            throw new ValidationException("Comment not found: " + like.getCommentId());
        }

        Like saved = likeRepository.save(like);
        logger.info("Like created successfully - likeId: {}", saved.getId());
        return saved;
    }

    /**
     * Updates a like with re-validation of all referenced entities.
     *
     * @param id          like ID
     * @param likeDetails updated like data
     * @return optional containing the updated like, or empty if not found
     * @throws ValidationException if any referenced entity does not exist
     */
    @Transactional
    @CacheEvict(value = {"likes", "postLikes", "userLikes"}, allEntries = true)
    public Optional<Like> updateAndValidateLike(Long id, Like likeDetails) {
        logger.info("Updating like - id: {}", id);

        if (!userService.existsById(likeDetails.getUserId())) {
            throw new ValidationException("User not found: " + likeDetails.getUserId());
        }

        if (likeDetails.getPostId() != null && !postService.existsById(likeDetails.getPostId())) {
            throw new ValidationException("Post not found: " + likeDetails.getPostId());
        }

        if (likeDetails.getCommentId() != null
                && commentService.getCommentById(likeDetails.getCommentId()).isEmpty()) {
            throw new ValidationException("Comment not found: " + likeDetails.getCommentId());
        }

        return likeRepository.findById(id).map(like -> {
            like.setUserId(likeDetails.getUserId());
            like.setPostId(likeDetails.getPostId());
            like.setCommentId(likeDetails.getCommentId());
            Like updated = likeRepository.save(like);
            logger.info("Like updated successfully - id: {}", id);
            return updated;
        });
    }

    /**
     * Retrieves a like by ID.
     *
     * @param id like ID
     * @return optional containing the like if found
     */
    @Cacheable(value = "likes", key = "#id")
    public Optional<Like> getLikeById(Long id) {
        return likeRepository.findById(id);
    }

    /**
     * Retrieves all likes for a given post.
     *
     * @param postId post ID
     * @return list of likes (may be empty)
     */
    @Cacheable(value = "postLikes", key = "#postId")
    public List<Like> findLikesByPostId(Long postId) {
        return likeRepository.findByPostId(postId);
    }

    /**
     * Retrieves all likes made by a given user.
     *
     * @param userId user ID
     * @return list of likes (may be empty)
     */
    @Cacheable(value = "userLikes", key = "#userId")
    public List<Like> findLikesByUserId(Long userId) {
        return likeRepository.findByUserId(userId);
    }

    /**
     * Retrieves all likes for a given comment.
     *
     * @param commentId comment ID
     * @return list of likes (may be empty)
     */
    public List<Like> findLikesByCommentId(Long commentId) {
        return likeRepository.findByCommentId(commentId);
    }

    /**
     * Retrieves all likes.
     *
     * @return list of all likes
     */
    public List<Like> findAllLikes() {
        return likeRepository.findAll();
    }

    /**
     * Deletes a like by ID.
     *
     * @param id like ID
     * @throws IllegalArgumentException if no like exists with that ID
     */
    @Transactional
    @CacheEvict(value = {"likes", "postLikes", "userLikes"}, allEntries = true)
    public void deleteLike(Long id) {
        logger.info("Deleting like - id: {}", id);

        if (!likeRepository.existsById(id)) {
            throw new IllegalArgumentException("Like not found: " + id);
        }

        likeRepository.deleteById(id);
        logger.info("Like deleted successfully - id: {}", id);
    }
}
