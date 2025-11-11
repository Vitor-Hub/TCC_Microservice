package com.mstcc.userms.dto;

import com.mstcc.userms.entities.User;

public class UserCreateDTO {
    
    private String name;
    private String email;
    private String bio;

    public UserCreateDTO() {}

    public UserCreateDTO(String name, String email, String bio) {
        this.name = name;
        this.email = email;
        this.bio = bio;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public User toEntity() {
        User user = new User();
        user.setUsername(this.name);
        user.setEmail(this.email);
        user.setPassword(this.bio != null ? this.bio : "");
        return user;
    }

    public static UserCreateDTO fromEntity(User user) {
        UserCreateDTO dto = new UserCreateDTO();
        dto.setName(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setBio(user.getPassword());
        return dto;
    }
}
