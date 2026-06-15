package com.mstcc.monolith.user.dto;

import com.mstcc.monolith.user.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for user creation and update requests.
 * Bean Validation annotations enforce contract at the HTTP boundary.
 */
public class UserCreateDTO {

    @NotBlank(message = "username is required")
    @Size(min = 3, max = 50, message = "username must be between 3 and 50 characters")
    private String username;

    @NotBlank(message = "email is required")
    @Email(message = "must be a valid email address")
    private String email;

    @NotBlank(message = "password is required")
    @Size(min = 6, message = "password must be at least 6 characters")
    private String password;

    public UserCreateDTO() {}

    /**
     * @param username the desired username (3-50 chars)
     * @param email    the user's email address
     * @param password the user's password (min 6 chars)
     */
    public UserCreateDTO(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    /**
     * Converts this DTO to a {@link User} entity.
     *
     * @return a new User populated with this DTO's fields
     */
    public User toEntity() {
        User user = new User();
        user.setUsername(this.username);
        user.setEmail(this.email);
        user.setPassword(this.password);
        return user;
    }
}
