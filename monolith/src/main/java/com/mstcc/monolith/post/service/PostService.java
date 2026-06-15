package com.mstcc.monolith.post.service;

import com.mstcc.monolith.comment.entity.Comment;
import com.mstcc.monolith.comment.repository.CommentRepository;
import com.mstcc.monolith.post.dto.CommentDTO;
import com.mstcc.monolith.post.dto.PostDTO;
import com.mstcc.monolith.post.dto.UserDTO;
import com.mstcc.monolith.post.entity.Post;
import com.mstcc.monolith.post.repository.PostRepository;
import com.mstcc.monolith.user.entity.User;
import com.mstcc.monolith.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Business logic for the Post domain.
 *
 * <p>Cross-domain enrichment (fetching the owning user and the post's comments)
 * is performed synchronously via direct Spring bean injection. In the microservices
 * stack these calls go over the network via Feign. In the monolith they are
 * in-process method calls with no serialisation overhead.
 *
 * <p>{@link UserService} is injected with {@code @Lazy} to break the potential
 * circular dependency that would arise if UserService ever depended on PostService
 * (it currently does not, but the pattern is defensive).
 */
@Service
@Transactional(readOnly = true)
public class PostService {

    private static final Logger logger = LoggerFactory.getLogger(PostService.class);

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserService userService;

    /**
     * Constructs the service with its required dependencies.
     *
     * @param postRepository    JPA repository for Post persistence
     * @param commentRepository JPA repository used for comment enrichment
     * @param userService       user domain service for cross-domain enrichment
     */
    public PostService(PostRepository postRepository,
                       CommentRepository commentRepository,
                       @Lazy UserService userService) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.userService = userService;
    }

    /**
     * Retrieves a post by ID with user and comments enrichment.
     *
     * <p>Enrichment is synchronous and in-process. If the user or comments are
     * not found, the post is still returned with null user / empty comments,
     * preserving the same partial-response behaviour as the microservices stack.
     *
     * @param id post ID
     * @return optional containing the enriched post DTO if found
     */
    @Cacheable(value = "posts", key = "#id")
    public Optional<PostDTO> getPostById(Long id) {
        logger.info("Fetching post by id: {}", id);
        return postRepository.findById(id).map(post -> {
            UserDTO user = buildUserDTO(post.getUserId());
            List<CommentDTO> comments = buildCommentDTOs(post.getId());
            return new PostDTO(post, user, comments);
        });
    }

    /**
     * Retrieves recent posts as a lightweight feed — no enrichment.
     *
     * @param limit maximum number of posts to return (capped at 100)
     * @return list of recent post DTOs without user or comment enrichment
     */
    @Cacheable(value = "allPosts", key = "'feed_recent_' + #limit")
    public List<PostDTO> getRecentPosts(int limit) {
        logger.info("Fetching recent {} posts for feed", limit);
        return postRepository
                .findAll(PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt")))
                .getContent()
                .stream()
                .map(post -> new PostDTO(post, null, List.of()))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all posts by a specific user — no enrichment.
     *
     * @param userId user ID
     * @return list of the user's post DTOs
     */
    @Cacheable(value = "userPosts", key = "#userId")
    public List<PostDTO> getPostsByUser(Long userId) {
        logger.info("Fetching posts for userId: {}", userId);
        return postRepository.findByUserId(userId)
                .stream()
                .map(post -> new PostDTO(post, null, List.of()))
                .collect(Collectors.toList());
    }

    /**
     * Creates a new post with optional user enrichment in the response.
     *
     * @param postDTO post data (must have user.id set)
     * @return created post DTO
     * @throws IllegalArgumentException if the user field is missing
     */
    @Transactional
    @CacheEvict(value = {"posts", "allPosts", "userPosts"}, allEntries = true)
    public PostDTO createPost(PostDTO postDTO) {
        if (postDTO.getUser() == null) {
            throw new IllegalArgumentException("User is required");
        }

        logger.info("Creating new post for userId: {}", postDTO.getUser().getId());

        Post post = new Post();
        post.setUserId(postDTO.getUser().getId());
        post.setContent(postDTO.getContent());
        post.setCreatedAt(LocalDateTime.now());

        Post savedPost = postRepository.save(post);
        UserDTO user = buildUserDTO(savedPost.getUserId());

        logger.info("Post created successfully: postId={}", savedPost.getId());
        return new PostDTO(savedPost, user, List.of());
    }

    /**
     * Updates post content for a specific user (ownership check included).
     *
     * @param userId  owning user ID
     * @param postId  post to update
     * @param content new content string
     * @return optional containing the updated enriched post DTO, or empty if not found or not owner
     */
    @Transactional
    @CacheEvict(value = {"posts", "allPosts", "userPosts"}, allEntries = true)
    public Optional<PostDTO> updatePostContentByUser(Long userId, Long postId, String content) {
        logger.info("Updating post - postId: {}, userId: {}", postId, userId);

        return postRepository.findById(postId)
                .filter(post -> post.getUserId().equals(userId))
                .map(post -> {
                    post.setContent(content);
                    Post updated = postRepository.save(post);
                    UserDTO user = buildUserDTO(updated.getUserId());
                    List<CommentDTO> comments = buildCommentDTOs(updated.getId());
                    logger.info("Post updated successfully: postId={}", postId);
                    return new PostDTO(updated, user, comments);
                });
    }

    /**
     * Deletes a post by ID.
     *
     * @param id post ID
     * @throws IllegalArgumentException if no post exists with that ID
     */
    @Transactional
    @CacheEvict(value = {"posts", "allPosts", "userPosts"}, allEntries = true)
    public void deletePost(Long id) {
        logger.info("Deleting post - postId: {}", id);

        if (!postRepository.existsById(id)) {
            throw new IllegalArgumentException("Post not found: " + id);
        }

        postRepository.deleteById(id);
        logger.info("Post deleted successfully - postId: {}", id);
    }

    /**
     * Checks whether a post exists by ID.
     *
     * @param id post ID
     * @return true if the post exists
     */
    public boolean existsById(Long id) {
        return postRepository.existsById(id);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Builds a {@link UserDTO} from the user domain service.
     * Returns null if the user is not found (graceful degradation).
     *
     * @param userId user ID
     * @return populated UserDTO or null
     */
    private UserDTO buildUserDTO(Long userId) {
        return userService.findUserById(userId)
                .map(u -> new UserDTO(u.getId(), u.getUsername(), u.getEmail()))
                .orElse(null);
    }

    /**
     * Builds a list of {@link CommentDTO}s for a given post.
     *
     * @param postId post ID
     * @return list of comment DTOs (may be empty)
     */
    private List<CommentDTO> buildCommentDTOs(Long postId) {
        return commentRepository.findByPostId(postId)
                .stream()
                .map(c -> new CommentDTO(c.getId(), c.getUserId(), c.getContent()))
                .collect(Collectors.toList());
    }
}
