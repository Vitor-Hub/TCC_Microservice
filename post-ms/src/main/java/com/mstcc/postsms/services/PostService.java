package com.mstcc.postsms.services;

import com.mstcc.postsms.dto.CommentDTO;
import com.mstcc.postsms.dto.PostDTO;
import com.mstcc.postsms.dto.UserDTO;
import com.mstcc.postsms.entities.Post;
import com.mstcc.postsms.repositories.PostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Service for managing Post entities with async validation
 * and parallel processing capabilities
 */
@Service
@Transactional(readOnly = true)
public class PostService {

    private static final Logger logger = LoggerFactory.getLogger(PostService.class);
    private static final int FETCH_TIMEOUT_SECONDS = 5;

    private final PostRepository postRepository;
    private final PostAsyncHelper asyncHelper;

    public PostService(PostRepository postRepository, 
                      PostAsyncHelper asyncHelper) {
        this.postRepository = postRepository;
        this.asyncHelper = asyncHelper;
    }

    /**
     * Retrieves a post by ID with user and comments populated
     * @param id post ID
     * @return optional containing post DTO if found
     */
    @Cacheable(value = "posts", key = "#id")
    public Optional<PostDTO> getPostById(Long id) {
        logger.info("Fetching post by id: {}", id);
        long startTime = System.currentTimeMillis();
        
        return postRepository.findById(id).map(post -> {
            try {
                // Parallel fetch user and comments
                CompletableFuture<UserDTO> userFuture = asyncHelper.getUserAsync(post.getUserId());
                CompletableFuture<List<CommentDTO>> commentsFuture = asyncHelper.getCommentsAsync(post.getId());
                
                // Wait for both with timeout
                CompletableFuture.allOf(userFuture, commentsFuture)
                    .get(FETCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                
                UserDTO user = userFuture.get();
                List<CommentDTO> comments = commentsFuture.get();
                
                long duration = System.currentTimeMillis() - startTime;
                logger.info("Post fetched successfully in {}ms - postId: {}, comments: {}", 
                           duration, id, comments.size());
                
                return new PostDTO(post, user, comments);
                
            } catch (TimeoutException e) {
                logger.error("Timeout fetching post data after {}s for postId: {}", 
                            FETCH_TIMEOUT_SECONDS, id);
                throw new RuntimeException("Timeout fetching post data", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while fetching post data", e);
            } catch (ExecutionException e) {
                logger.error("Failed to fetch post data for postId: {}, error: {}", 
                            id, e.getCause().getMessage());
                throw new RuntimeException("Failed to fetch post data", e);
            }
        });
    }

    /**
     * Retrieves recent posts with user and comments populated
     * Uses parallel processing for better performance
     * @param limit maximum number of posts to return
     * @return list of recent post DTOs
     */
    @Cacheable(value = "allPosts", key = "'feed_recent_' + #limit")
    public List<PostDTO> getRecentPosts(int limit) {
        logger.info("Fetching recent {} posts for feed", limit);
        long startTime = System.currentTimeMillis();
        
        List<Post> posts = postRepository
            .findAll(PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt")))
            .getContent();
        
        logger.info("Found {} posts in database", posts.size());
        
        // Process all posts in parallel
        List<CompletableFuture<PostDTO>> futures = posts.stream()
            .map(post -> CompletableFuture.supplyAsync(() -> {
                try {
                    UserDTO user = asyncHelper.getUserAsync(post.getUserId())
                        .get(FETCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    List<CommentDTO> comments = asyncHelper.getCommentsAsync(post.getId())
                        .get(FETCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    return new PostDTO(post, user, comments);
                } catch (Exception e) {
                    logger.error("Failed to fetch data for post {}: {}", post.getId(), e.getMessage());
                    return null;
                }
            }))
            .collect(Collectors.toList());

        List<PostDTO> result = futures.stream()
            .map(CompletableFuture::join)
            .filter(dto -> dto != null)
            .collect(Collectors.toList());
        
        long duration = System.currentTimeMillis() - startTime;
        logger.info("Recent posts fetched in {}ms - returned: {}/{}", 
                   duration, result.size(), posts.size());
        
        return result;
    }

    /**
     * Retrieves all posts by a specific user
     * @param userId user ID
     * @return list of user's post DTOs
     */
    @Cacheable(value = "userPosts", key = "#userId")
    public List<PostDTO> getPostsByUser(Long userId) {
        logger.info("Fetching posts for userId: {}", userId);
        long startTime = System.currentTimeMillis();
        
        // Validate user exists first
        try {
            asyncHelper.getUserAsync(userId)
                .get(FETCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.error("User not found: userId={}", userId);
            throw new RuntimeException("User not found: " + userId, e);
        }
        
        List<Post> posts = postRepository.findByUserId(userId);
        logger.info("Found {} posts for userId: {}", posts.size(), userId);
        
        // Process posts in parallel
        List<CompletableFuture<PostDTO>> futures = posts.stream()
            .map(post -> asyncHelper.getCommentsAsync(post.getId())
                .thenApply(comments -> {
                    try {
                        UserDTO user = asyncHelper.getUserAsync(post.getUserId())
                            .get(FETCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                        return new PostDTO(post, user, comments);
                    } catch (Exception e) {
                        logger.error("Failed to fetch data for post {}: {}", post.getId(), e.getMessage());
                        return null;
                    }
                }))
            .collect(Collectors.toList());

        List<PostDTO> result = futures.stream()
            .map(CompletableFuture::join)
            .filter(dto -> dto != null)
            .collect(Collectors.toList());
        
        long duration = System.currentTimeMillis() - startTime;
        logger.info("User posts fetched in {}ms - userId: {}, count: {}", 
                   duration, userId, result.size());
        
        return result;
    }

    /**
     * Creates a new post
     * @param postDTO post data
     * @return created post DTO
     */
    @Transactional
    @CacheEvict(value = {"posts", "allPosts", "userPosts"}, allEntries = true)
    public PostDTO createPost(PostDTO postDTO) {
        logger.info("Creating new post for userId: {}", postDTO.getUser().getId());
        long startTime = System.currentTimeMillis();
        
        Post post = new Post();
        post.setUserId(postDTO.getUser().getId());
        post.setContent(postDTO.getContent());
        post.setCreatedAt(LocalDateTime.now());
        
        Post savedPost = postRepository.save(post);
        
        try {
            UserDTO user = asyncHelper.getUserAsync(savedPost.getUserId())
                .get(FETCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
            long duration = System.currentTimeMillis() - startTime;
            logger.info("Post created successfully in {}ms - postId: {}", duration, savedPost.getId());
            
            return new PostDTO(savedPost, user, List.of());
        } catch (Exception e) {
            logger.error("Failed to fetch user data after creating post");
            throw new RuntimeException("Failed to fetch user data after creating post", e);
        }
    }

    /**
     * Updates post content
     * @param userId user ID (for authorization)
     * @param postId post ID
     * @param content new content
     * @return optional containing updated post DTO
     */
    @Transactional
    @CacheEvict(value = {"posts", "allPosts", "userPosts"}, allEntries = true)
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
                
                try {
                    UserDTO user = asyncHelper.getUserAsync(updatedPost.getUserId())
                        .get(FETCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    List<CommentDTO> comments = asyncHelper.getCommentsAsync(updatedPost.getId())
                        .get(FETCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    
                    long duration = System.currentTimeMillis() - startTime;
                    logger.info("Post updated successfully in {}ms - postId: {}", duration, postId);
                    
                    return new PostDTO(updatedPost, user, comments);
                } catch (Exception e) {
                    logger.error("Failed to fetch post data after update");
                    throw new RuntimeException("Failed to fetch post data after update", e);
                }
            });
    }

    /**
     * Deletes a post
     * @param id post ID
     */
    @Transactional
    @CacheEvict(value = {"posts", "allPosts", "userPosts"}, allEntries = true)
    public void deletePost(Long id) {
        logger.info("Deleting post - postId: {}", id);
        
        if (!postRepository.existsById(id)) {
            logger.warn("Post not found for deletion - postId: {}", id);
            throw new IllegalArgumentException("Post not found: " + id);
        }
        
        postRepository.deleteById(id);
        logger.info("Post deleted successfully - postId: {}", id);
    }
}