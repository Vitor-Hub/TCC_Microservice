package com.mstcc.userms.dto;

import com.mstcc.userms.entities.User;

/**
 * Read-only projection of {@link User} returned by the API.
 * Intentionally omits {@code password} to prevent sensitive data exposure (SRP).
 *
 * @param id       unique identifier
 * @param username display name
 * @param email    email address
 */
public record UserResponseDTO(Long id, String username, String email) {

    /**
     * Creates a response DTO from a {@link User} entity.
     *
     * @param user source entity
     * @return populated DTO
     */
    public static UserResponseDTO from(User user) {
        return new UserResponseDTO(user.getId(), user.getUsername(), user.getEmail());
    }
}
