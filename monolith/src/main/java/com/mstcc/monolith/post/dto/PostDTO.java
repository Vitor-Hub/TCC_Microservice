package com.mstcc.monolith.post.dto;

import com.mstcc.monolith.post.entity.Post;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Object for post responses.
 *
 * <p>The structure mirrors the {@code PostDTO} from {@code post-ms}: it carries
 * an embedded {@link UserDTO} and a list of {@link CommentDTO}s so the K6 script
 * — which parses {@code post.user.id} and iterates {@code post.comments} —
 * receives the same JSON shape it expects from the microservices gateway.
 *
 * <p>In the monolith, enrichment is performed synchronously via direct service
 * calls instead of parallel Feign/CompletableFuture chains. This is the key
 * architectural difference this TCC measures: the monolith pays no network cost
 * but also cannot run user and comment fetches in parallel across separate JVMs.
 */
public class PostDTO {

    private Long id;
    private String content;
    private LocalDateTime createdAt;
    private UserDTO user;
    private List<CommentDTO> comments;

    public PostDTO() {}

    /**
     * Creates a PostDTO from a {@link Post} entity with optional enrichment data.
     *
     * @param post     the post entity
     * @param user     the owning user (may be null if the user domain returns empty)
     * @param comments list of comments on the post (may be empty)
     */
    public PostDTO(Post post, UserDTO user, List<CommentDTO> comments) {
        this.id = post.getId();
        this.content = post.getContent();
        this.createdAt = post.getCreatedAt();
        this.user = user;
        this.comments = comments;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public UserDTO getUser() { return user; }
    public void setUser(UserDTO user) { this.user = user; }
    public List<CommentDTO> getComments() { return comments; }
    public void setComments(List<CommentDTO> comments) { this.comments = comments; }
}
