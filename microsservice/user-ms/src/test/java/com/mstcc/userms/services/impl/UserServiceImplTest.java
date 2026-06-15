package com.mstcc.userms.services.impl;

import com.mstcc.userms.entities.User;
import com.mstcc.userms.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link UserServiceImpl}.
 *
 * <p>Pure unit tests — no Spring context loaded. All dependencies are mocked
 * with Mockito so each test exercises only the service logic in isolation.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("vitor");
        user.setEmail("vitor@example.com");
        user.setPassword("secret");
    }

    // -------------------------------------------------------------------------
    // findAllUsers
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("findAllUsers returns list from repository")
    void findAllUsers_returnsAllUsers() {
        when(userRepository.findAll()).thenReturn(List.of(user));

        List<User> result = userService.findAllUsers();

        assertThat(result).hasSize(1).containsExactly(user);
        verify(userRepository).findAll();
    }

    // -------------------------------------------------------------------------
    // findUserById
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("findUserById returns user when found")
    void findUserById_whenExists_returnsUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        Optional<User> result = userService.findUserById(1L);

        assertThat(result).isPresent().contains(user);
    }

    @Test
    @DisplayName("findUserById returns empty when not found")
    void findUserById_whenNotExists_returnsEmpty() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<User> result = userService.findUserById(99L);

        assertThat(result).isEmpty();
    }

    // -------------------------------------------------------------------------
    // saveUser
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("saveUser persists and returns user when username is unique")
    void saveUser_withUniqueUsername_savesSuccessfully() {
        when(userRepository.findByUsername("vitor")).thenReturn(Optional.empty());
        when(userRepository.save(user)).thenReturn(user);

        User saved = userService.saveUser(user);

        assertThat(saved).isEqualTo(user);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("saveUser throws IllegalArgumentException when username already exists")
    void saveUser_withDuplicateUsername_throwsIllegalArgumentException() {
        when(userRepository.findByUsername("vitor")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.saveUser(user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username already exists");

        verify(userRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // updateUser
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("updateUser returns updated user when found")
    void updateUser_whenFound_updatesFields() {
        User updated = new User();
        updated.setId(1L);
        updated.setUsername("vitor_updated");
        updated.setEmail("updated@example.com");
        updated.setPassword("newpass");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findByUsername("vitor_updated")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<User> result = userService.updateUser(1L, updated);

        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("vitor_updated");
        assertThat(result.get().getEmail()).isEqualTo("updated@example.com");
    }

    @Test
    @DisplayName("updateUser returns empty when user not found")
    void updateUser_whenNotFound_returnsEmpty() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<User> result = userService.updateUser(99L, user);

        assertThat(result).isEmpty();
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateUser throws IllegalArgumentException when new username is taken by another user")
    void updateUser_withTakenUsername_throwsIllegalArgumentException() {
        User other = new User();
        other.setId(2L);
        other.setUsername("taken");

        User details = new User();
        details.setUsername("taken");
        details.setEmail("new@example.com");
        details.setPassword("pass");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findByUsername("taken")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> userService.updateUser(1L, details))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username already exists");
    }

    // -------------------------------------------------------------------------
    // deleteUser
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("deleteUser removes user when found")
    void deleteUser_whenFound_deletesSuccessfully() {
        when(userRepository.existsById(1L)).thenReturn(true);

        assertThatCode(() -> userService.deleteUser(1L)).doesNotThrowAnyException();

        verify(userRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteUser throws IllegalArgumentException when user does not exist")
    void deleteUser_whenNotFound_throwsIllegalArgumentException() {
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> userService.deleteUser(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");

        verify(userRepository, never()).deleteById(any());
    }

    // -------------------------------------------------------------------------
    // existsById
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("existsById returns true when user exists")
    void existsById_whenExists_returnsTrue() {
        when(userRepository.existsById(1L)).thenReturn(true);

        assertThat(userService.existsById(1L)).isTrue();
    }

    @Test
    @DisplayName("existsById returns false when user does not exist")
    void existsById_whenNotExists_returnsFalse() {
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThat(userService.existsById(99L)).isFalse();
    }
}
