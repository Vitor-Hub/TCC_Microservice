package com.mstcc.postsms.dto;

import com.mstcc.postsms.entities.Post;

import java.time.LocalDateTime;

public class PostDTO {

    private Long id;
    private String content;
    private LocalDateTime createdAt;
    private UserDTO user;

    public PostDTO() {
    }

    public PostDTO(Post post, UserDTO user) {
        this.id = post.getId();
        this.content = post.getContent();
        this.createdAt = post.getCreatedAt();
        this.user = user;
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

    public UserDTO getUser() {
        return user;
    }

    public void setUser(UserDTO user) {
        this.user = user;
    }
}