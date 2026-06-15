package com.mstcc.monolith.user.repository;

import com.mstcc.monolith.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link User} entities.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by exact username match.
     *
     * @param username the username to look up
     * @return an Optional containing the user, or empty if not found
     */
    Optional<User> findByUsername(String username);
}
