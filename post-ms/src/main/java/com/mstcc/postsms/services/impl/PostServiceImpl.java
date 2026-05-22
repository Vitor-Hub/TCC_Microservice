package com.mstcc.postsms.services.impl;

import com.mstcc.postsms.dto.CommentDTO;
import com.mstcc.postsms.dto.PostDTO;
import com.mstcc.postsms.dto.UserDTO;
import com.mstcc.postsms.entities.Post;
import com.mstcc.postsms.repositories.PostRepository;
import com.mstcc.postsms.services.PostAsyncHelper;
import com.mstcc.postsms.services.PostService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Service for managing Post entities with async validation
 * and parallel processing capabilities.
 * <p>
 * SRP: this class owns all post business logic.
 * DIP: implements {@link PostService}; callers depend on the interface, not this class.
 */
@Service
@Transactional(readOnly = true)
public class PostServiceImpl implements PostService {

    private static final Logger logger = LoggerFactory.getLogger(PostServiceImpl.class);
    private static final int FETCH_TIMEOUT_SECONDS = 5;

    private final PostRepository postRepository;
    private final PostAsyncHelper asyncHelper;
    private final Executor taskExecutor;

    public PostServiceImpl(PostRepository postRepository,
                           PostAsyncHelper asyncHelper,
                           @Qualifier("taskExecutor") Executor taskExecutor) {
        this.postRepository = postRepository;
        this.asyncHelper = asyncHelper;
        this.taskExecutor = taskExecutor;
    }

    /**
     * Retrieves a post by ID with user and comments enrichment where available.
     *
     * <p>Upstream calls to user-ms and comment-ms are made in parallel. If either
     * service times out or fails, the post is still returned with whatever data is
     * available (null user and/or empty comments). The response is always 200 when
     * the post exists — upstream availability does not affect the HTTP status.
     *
     * @param id post ID
     * @return optional containing post DTO if found; enrichment fields may be null on upstream failure
     */
    @Cacheable(value = "posts", key = "#id")
    @Override
    public Optional<PostDTO> getPostById(Long id) {
        logger.info("Fetching post by id: {}", id);
        long startTime = System.currentTimeMillis();

        return postRepository.findById(id).map(post -> {
            UserDTO user = null;
            List<CommentDTO> comments = List.of();

            try {
                // Parallel fetch user and comments
                CompletableFuture<UserDTO> userFuture = asyncHelper.getUserAsync(post.getUserId());
                CompletableFuture<List<CommentDTO>> commentsFuture = asyncHelper.getCommentsAsync(post.getId());

                CompletableFuture.allOf(userFuture, commentsFuture)
                    .get(FETCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                user = userFuture.get();
                comments = commentsFuture.get();

            } catch (TimeoutException e) {
                logger.warn("Timeout fetching enrichment data for postId: {} after {}s — returning partial response",
                           id, FETCH_TIMEOUT_SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Interrupted fetching enrichment data for postId: {}", id);
            } catch (ExecutionException e) {
                logger.warn("Failed to fetch enrichment data for postId: {}, error: {}",
                           id, e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
            }

            long duration = System.currentTimeMillis() - startTime;
            logger.info("Post fetched in {}ms - postId: {}, comments: {}",
                       duration, id, comments.size());

            return new PostDTO(post, user, comments);
        });
    }

    /**
     * Retrieves recent posts as a lightweight feed — no cross-service enrichment.
     *
     * <p>List endpoints return only the data owned by post-ms (id, userId, content,
     * createdAt). User and comment data are omitted to avoid N×2 upstream calls
     * (one user fetch + one comment fetch per post) that would saturate user-ms
     * and comment-ms under high concurrency. Callers that need the full view
     * should request individual posts via {@link #getPostById(Long)}.
     *
     * @param limit maximum number of posts to return (capped at 100)
     * @return list of recent post DTOs without user or comment enrichment
     */
    @Cacheable(value = "allPosts", key = "'feed_recent_' + #limit")
    @Override
    public List<PostDTO> getRecentPosts(int limit) {
        logger.info("Fetching recent {} posts for feed", limit);
        long startTime = System.currentTimeMillis();

        List<Post> posts = postRepository
            .findAll(PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt")))
            .getContent();

        List<PostDTO> result = posts.stream()
            .map(post -> new PostDTO(post, null, List.of()))
            .collect(Collectors.toList());

        long duration = System.currentTimeMillis() - startTime;
        logger.info("Recent posts fetched in {}ms - returned: {}/{}",
                   duration, result.size(), posts.size());

        return result;
    }

    /**
     * Retrieves all posts by a specific user as lightweight DTOs — no cross-service enrichment.
     *
     * <p>Pure local read: queries only the post-ms database. No upstream calls are made.
     * User existence is implicitly validated by the data — if no posts exist for the given
     * userId, an empty list is returned. Callers requiring user-existence guarantees should
     * validate through user-ms directly before calling this endpoint.
     *
     * <p>This avoids a blocking upstream call on every GET request that would cascade
     * into 503s whenever user-ms is under pressure.
     *
     * @param userId user ID
     * @return list of user's post DTOs without user or comment enrichment
     */
    @Cacheable(value = "userPosts", key = "#userId")
    @Override
    public List<PostDTO> getPostsByUser(Long userId) {
        logger.info("Fetching posts for userId: {}", userId);
        long startTime = System.currentTimeMillis();

        List<Post> posts = postRepository.findByUserId(userId);

        List<PostDTO> result = posts.stream()
            .map(post -> new PostDTO(post, null, List.of()))
            .collect(Collectors.toList());

        long duration = System.currentTimeMillis() - startTime;
        logger.info("User posts fetched in {}ms - userId: {}, count: {}",
                   duration, userId, result.size());

        return result;
    }

    /**
     * Creates a new post.
     * @param postDTO post data (must have user.id set)
     * @return created post DTO
     */
    @Transactional
    @CacheEvict(value = {"posts", "allPosts", "userPosts"}, allEntries = true)
    @Override
    public PostDTO createPost(PostDTO postDTO) {
        logger.info("Creating new post for userId: {}", postDTO.getUser().getId());
        long startTime = System.currentTimeMillis();

        Post post = new Post();
        post.setUserId(postDTO.getUser().getId());
        post.setContent(postDTO.getContent());
        post.setCreatedAt(LocalDateTime.now());

        Post savedPost = postRepository.save(post);

        // Attempt to enrich with user data; degrade gracefully if user-ms is unavailable.
        // The post is already persisted — do NOT fail the request due to an enrichment error.
        UserDTO user = null;
        try {
            user = asyncHelper.getUserAsync(savedPost.getUserId())
                .get(FETCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.warn("Could not fetch user data for response enrichment after creating post - postId: {}, userId: {}",
                       savedPost.getId(), savedPost.getUserId());
        }

        long duration = System.currentTimeMillis() - startTime;
        logger.info("Post created successfully in {}ms - postId: {}", duration, savedPost.getId());

        return new PostDTO(savedPost, user, List.of());
    }

    /**
     * Updates post content.
     * @param userId  owning user ID (for authorization check)
     * @param postId  post to update
     * @param content new content string
     * @return optional containing updated post DTO, or empty if not found or not owner
     */
    @Transactional
    @CacheEvict(value = {"posts", "allPosts", "userPosts"}, allEntries = true)
    @Override
    public Optional<PostDTO> updatePostContentByUser(Long userId, Long postId, String content) {
        logger.info("Updating post - postId: {}, userId: {}", postId, userId);

        return postRepository.findById(postId)
            .filter(post -> {
                boolean isOwner = post.getUserId().equals(userId);
                if (!isOwner) {
                    logger.warn("Unauthorized update attempt - postId: {}, userId: {}", postId, userId);
                }
                return isOwner;
            })
            .map(post -> {
                long startTime = System.currentTimeMillis();

                post.setContent(content);
                Post updatedPost = postRepository.save(post);

                // Fetch user and comments in parallel — same pattern as getPostById.
                // Enrichment failures degrade gracefully: the post is already saved and
                // upstream errors must not roll back the update or return 500 to the caller.
                UserDTO user = null;
                List<CommentDTO> comments = List.of();

                try {
                    CompletableFuture<UserDTO> userFuture =
                            asyncHelper.getUserAsync(updatedPost.getUserId());
                    CompletableFuture<List<CommentDTO>> commentsFuture =
                            asyncHelper.getCommentsAsync(updatedPost.getId());

                    CompletableFuture.allOf(userFuture, commentsFuture)
                            .get(FETCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                    user = userFuture.get();
                    comments = commentsFuture.get();

                } catch (TimeoutException e) {
                    logger.warn("Timeout fetching enrichment after update for postId: {} — returning partial response", postId);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("Interrupted fetching enrichment after update for postId: {}", postId);
                } catch (ExecutionException e) {
                    logger.warn("Failed to fetch enrichment after update for postId: {}, error: {}",
                            postId, e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
                }

                long duration = System.currentTimeMillis() - startTime;
                logger.info("Post updated successfully in {}ms - postId: {}", duration, postId);

                return new PostDTO(updatedPost, user, comments);
            });
    }

    /**
     * Deletes a post.
     * @param id post ID
     * @throws IllegalArgumentException if no post exists with that ID
     */
    @Transactional
    @CacheEvict(value = {"posts", "allPosts", "userPosts"}, allEntries = true)
    @Override
    public void deletePost(Long id) {
        logger.info("Deleting post - postId: {}", id);

        if (!postRepository.existsById(id)) {
            logger.warn("Post not found for deletion - postId: {}", id);
            throw new IllegalArgumentException("Post not found: " + id);
        }

        postRepository.deleteById(id);
        logger.info("Post deleted successfully - postId: {}", id);
    }

    /**
     * Checks if a post exists by ID.
     * Does not trigger any Feign calls — safe for cross-service existence validation.
     * @param id post ID
     * @return true if post exists
     */
    @Override
    public boolean existsById(Long id) {
        return postRepository.existsById(id);
    }
}
