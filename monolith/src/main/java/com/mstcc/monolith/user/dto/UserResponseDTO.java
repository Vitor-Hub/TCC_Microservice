package com.mstcc.monolith.user.dto;

import com.mstcc.monolith.user.entity.User;

/**
 * Read-only projection of {@link User} returned by the API.
 * Intentionally omits {@code password} to prevent sensitive data exposure.
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
