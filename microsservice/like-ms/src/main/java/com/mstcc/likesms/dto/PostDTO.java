package com.mstcc.likesms.dto;

import java.time.LocalDateTime;

public class PostDTO {
    private Long id;
    private String content;
    private LocalDateTime createdAt;
    private Long userId; // Assuming you want to include the userId in the PostDTO

    // Constructors, Getters and Setters
    public PostDTO() {}

    public PostDTO(Long id, String content, LocalDateTime createdAt, Long userId) {
        this.id = id;
        this.content = content;
        this.createdAt = createdAt;
        this.userId = userId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}