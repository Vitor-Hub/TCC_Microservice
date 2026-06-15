package com.mstcc.monolith.post.dto;

/**
 * Lightweight comment projection embedded in {@link PostDTO}.
 * Mirrors the {@code CommentDTO} in {@code post-ms} for JSON shape parity.
 */
public class CommentDTO {

    private Long id;
    private Long userId;
    private String content;

    public CommentDTO() {}

    /**
     * @param id      comment identifier
     * @param userId  ID of the comment's author
     * @param content comment text
     */
    public CommentDTO(Long id, Long userId, String content) {
        this.id = id;
        this.userId = userId;
        this.content = content;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
