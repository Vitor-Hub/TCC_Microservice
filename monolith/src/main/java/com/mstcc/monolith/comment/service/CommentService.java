package com.mstcc.monolith.comment.service;

import com.mstcc.monolith.comment.entity.Comment;
import com.mstcc.monolith.comment.repository.CommentRepository;
import com.mstcc.monolith.exception.ValidationException;
import com.mstcc.monolith.post.service.PostService;
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
 * Business logic for the Comment domain.
 *
 * <p>Cross-domain validation (user existence, post existence) is performed via
 * direct calls to {@link UserService} and {@link PostService} instead of the
 * Feign + CompletableFuture pattern used in the microservices stack. The
 * monolith does this sequentially within the same JVM, paying no network cost
 * but losing the parallel execution benefit.
 *
 * <p>Both upstream services are injected with {@code @Lazy} to avoid circular
 * dependency resolution at startup (PostService depends on CommentRepository,
 * which is separate from CommentService; the cycle risk is indirect but real
 * if Spring tries to eagerly initialise the full graph).
 */
@Service
@Transactional(readOnly = true)
public class CommentService {

    private static final Logger logger = LoggerFactory.getLogger(CommentService.class);

    private final CommentRepository commentRepository;
    private final UserService userService;
    private final PostService postService;

    /**
     * Constructs the service with its required dependencies.
     *
     * @param commentRepository JPA repository for Comment persistence
     * @param userService       user domain service for existence validation
     * @param postService       post domain service for existence validation
     */
    public CommentService(CommentRepository commentRepository,
                          @Lazy UserService userService,
                          @Lazy PostService postService) {
        this.commentRepository = commentRepository;
        this.userService = userService;
        this.postService = postService;
    }

    /**
     * Creates a comment after validating that the referenced user and post exist.
     *
     * <p>In the microservices stack this validation involved two parallel Feign
     * calls with a shared timeout. In the monolith they are sequential in-process
     * calls. Both architectures throw an exception and return an error to the
     * caller if validation fails.
     *
     * @param comment the comment entity to create
     * @return the saved comment entity
     * @throws ValidationException if the user or post does not exist
     */
    @Transactional
    @CacheEvict(value = {"comments", "postComments", "userComments"}, allEntries = true)
    public Comment createAndValidateComment(Comment comment) {
        logger.info("Creating comment - userId: {}, postId: {}",
                comment.getUserId(), comment.getPostId());

        if (!userService.existsById(comment.getUserId())) {
            throw new ValidationException("User not found: " + comment.getUserId());
        }

        if (!postService.existsById(comment.getPostId())) {
            throw new ValidationException("Post not found: " + comment.getPostId());
        }

        comment.setCreatedAt(LocalDateTime.now());
        Comment saved = commentRepository.save(comment);
        logger.info("Comment created successfully - commentId: {}", saved.getId());
        return saved;
    }

    /**
     * Retrieves a comment by ID.
     *
     * @param id comment ID
     * @return optional containing comment if found
     */
    @Cacheable(value = "comments", key = "#id")
    public Optional<Comment> getCommentById(Long id) {
        logger.info("Fetching comment by id: {}", id);
        return commentRepository.findById(id);
    }

    /**
     * Retrieves all comments for a post.
     *
     * @param postId post ID
     * @return list of comments (may be empty)
     */
    @Cacheable(value = "postComments", key = "#postId")
    public List<Comment> findCommentsByPostId(Long postId) {
        logger.info("Fetching comments for postId: {}", postId);
        return commentRepository.findByPostId(postId);
    }

    /**
     * Retrieves all comments by a user.
     *
     * @param userId user ID
     * @return list of the user's comments
     */
    @Cacheable(value = "userComments", key = "#userId")
    public List<Comment> findCommentsByUserId(Long userId) {
        logger.info("Fetching comments for userId: {}", userId);
        return commentRepository.findByUserId(userId);
    }

    /**
     * Returns all comments without any filter.
     *
     * @return list of all comments
     */
    public List<Comment> findAllComments() {
        logger.info("Fetching all comments");
        return commentRepository.findAll();
    }

    /**
     * Updates a comment with re-validation of the referenced user and post.
     *
     * @param id             comment ID
     * @param commentDetails updated comment details
     * @return optional containing the updated comment, or empty if not found
     * @throws ValidationException if the user or post does not exist
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "comments", key = "#id"),
        @CacheEvict(value = "postComments", allEntries = true),
        @CacheEvict(value = "userComments", allEntries = true)
    })
    public Optional<Comment> updateAndValidateComment(Long id, Comment commentDetails) {
        logger.info("Updating comment - id: {}", id);

        if (!userService.existsById(commentDetails.getUserId())) {
            throw new ValidationException("User not found: " + commentDetails.getUserId());
        }

        if (!postService.existsById(commentDetails.getPostId())) {
            throw new ValidationException("Post not found: " + commentDetails.getPostId());
        }

        return commentRepository.findById(id).map(comment -> {
            comment.setContent(commentDetails.getContent());
            comment.setUserId(commentDetails.getUserId());
            comment.setPostId(commentDetails.getPostId());
            Comment updated = commentRepository.save(comment);
            logger.info("Comment updated successfully - id: {}", id);
            return updated;
        });
    }

    /**
     * Deletes a comment by ID.
     *
     * @param id comment ID
     * @throws IllegalArgumentException if no comment with the given ID exists
     */
    @Transactional
    @CacheEvict(value = {"comments", "postComments", "userComments"}, allEntries = true)
    public void deleteComment(Long id) {
        logger.info("Deleting comment - id: {}", id);

        if (!commentRepository.existsById(id)) {
            throw new IllegalArgumentException("Comment not found: " + id);
        }

        commentRepository.deleteById(id);
        logger.info("Comment deleted successfully - id: {}", id);
    }
}
