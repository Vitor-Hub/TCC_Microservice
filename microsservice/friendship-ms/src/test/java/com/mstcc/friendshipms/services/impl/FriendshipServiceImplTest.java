package com.mstcc.friendshipms.services.impl;

import com.mstcc.friendshipms.dto.UserDTO;
import com.mstcc.friendshipms.entities.Friendship;
import com.mstcc.friendshipms.exception.FriendshipValidationException;
import com.mstcc.friendshipms.repositories.FriendshipRepository;
import com.mstcc.friendshipms.services.FriendshipAsyncHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link FriendshipServiceImpl}.
 *
 * <p>Key business rules under test:
 * <ul>
 *   <li>Self-friendship prevention</li>
 *   <li>ID normalisation (userId1 must always be the smaller value)</li>
 *   <li>Upstream user validation via async helper</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class FriendshipServiceImplTest {

    @Mock
    private FriendshipRepository friendshipRepository;

    @Mock
    private FriendshipAsyncHelper asyncHelper;

    @InjectMocks
    private FriendshipServiceImpl friendshipService;

    private Friendship friendship;
    private UserDTO user1DTO;
    private UserDTO user2DTO;

    @BeforeEach
    void setUp() {
        friendship = new Friendship();
        friendship.setId(1L);
        friendship.setUserId1(1L);
        friendship.setUserId2(2L);
        friendship.setStatus("PENDING");

        user1DTO = new UserDTO();
        user1DTO.setId(1L);

        user2DTO = new UserDTO();
        user2DTO.setId(2L);
    }

    // -------------------------------------------------------------------------
    // createAndValidateFriendship — happy path
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("createFriendship persists friendship when both users exist")
    void createFriendship_whenBothUsersExist_savesFriendship() {
        when(asyncHelper.getUserAsync(1L))
                .thenReturn(CompletableFuture.completedFuture(user1DTO));
        when(asyncHelper.getUserAsync(2L))
                .thenReturn(CompletableFuture.completedFuture(user2DTO));
        when(friendshipRepository.save(any(Friendship.class))).thenReturn(friendship);

        Friendship saved = friendshipService.createAndValidateFriendship(friendship);

        assertThat(saved).isEqualTo(friendship);
        verify(friendshipRepository).save(friendship);
    }

    // -------------------------------------------------------------------------
    // createAndValidateFriendship — self-friendship
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("createFriendship throws IllegalArgumentException when both IDs are the same")
    void createFriendship_withSameUserId_throwsIllegalArgumentException() {
        friendship.setUserId1(5L);
        friendship.setUserId2(5L);

        assertThatThrownBy(() -> friendshipService.createAndValidateFriendship(friendship))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot create friendship with yourself");

        verify(friendshipRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // createAndValidateFriendship — ID normalisation
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("createFriendship swaps IDs so userId1 is always the smaller value")
    void createFriendship_withReversedIds_normalisesOrder() {
        friendship.setUserId1(10L);
        friendship.setUserId2(3L);

        when(asyncHelper.getUserAsync(3L))
                .thenReturn(CompletableFuture.completedFuture(user1DTO));
        when(asyncHelper.getUserAsync(10L))
                .thenReturn(CompletableFuture.completedFuture(user2DTO));
        when(friendshipRepository.save(any(Friendship.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Friendship saved = friendshipService.createAndValidateFriendship(friendship);

        assertThat(saved.getUserId1()).isLessThan(saved.getUserId2());
        assertThat(saved.getUserId1()).isEqualTo(3L);
        assertThat(saved.getUserId2()).isEqualTo(10L);
    }

    @Test
    @DisplayName("createFriendship keeps IDs as-is when userId1 is already smaller")
    void createFriendship_withCorrectOrder_doesNotSwapIds() {
        friendship.setUserId1(1L);
        friendship.setUserId2(5L);

        when(asyncHelper.getUserAsync(1L))
                .thenReturn(CompletableFuture.completedFuture(user1DTO));
        when(asyncHelper.getUserAsync(5L))
                .thenReturn(CompletableFuture.completedFuture(user2DTO));
        when(friendshipRepository.save(any(Friendship.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Friendship saved = friendshipService.createAndValidateFriendship(friendship);

        assertThat(saved.getUserId1()).isEqualTo(1L);
        assertThat(saved.getUserId2()).isEqualTo(5L);
    }

    // -------------------------------------------------------------------------
    // createAndValidateFriendship — upstream failure
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("createFriendship throws FriendshipValidationException when upstream user lookup fails")
    void createFriendship_whenUserValidationFails_throwsFriendshipValidationException() {
        when(asyncHelper.getUserAsync(1L))
                .thenReturn(CompletableFuture.failedFuture(
                        new RuntimeException("User not found: 1")));
        when(asyncHelper.getUserAsync(2L))
                .thenReturn(CompletableFuture.completedFuture(user2DTO));

        assertThatThrownBy(() -> friendshipService.createAndValidateFriendship(friendship))
                .isInstanceOf(FriendshipValidationException.class)
                .hasMessageContaining("Validation failed");

        verify(friendshipRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // getFriendshipById
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getFriendshipById returns friendship when found")
    void getFriendshipById_whenExists_returnsFriendship() {
        when(friendshipRepository.findById(1L)).thenReturn(Optional.of(friendship));

        Optional<Friendship> result = friendshipService.getFriendshipById(1L);

        assertThat(result).isPresent().contains(friendship);
    }

    @Test
    @DisplayName("getFriendshipById returns empty when not found")
    void getFriendshipById_whenNotExists_returnsEmpty() {
        when(friendshipRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Friendship> result = friendshipService.getFriendshipById(99L);

        assertThat(result).isEmpty();
    }

    // -------------------------------------------------------------------------
    // findFriendshipsByUserId
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("findFriendshipsByUserId returns result from repository without calling asyncHelper")
    void findFriendshipsByUserId_returnsFriendshipsFromRepository() {
        when(friendshipRepository.findByUserId1OrUserId2(1L, 1L))
                .thenReturn(List.of(friendship));

        List<Friendship> result = friendshipService.findFriendshipsByUserId(1L);

        assertThat(result).hasSize(1).containsExactly(friendship);
        verify(asyncHelper, never()).getUserAsync(any());
    }

    // -------------------------------------------------------------------------
    // deleteFriendship
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("deleteFriendship removes friendship when found")
    void deleteFriendship_whenFound_deletesSuccessfully() {
        when(friendshipRepository.existsById(1L)).thenReturn(true);

        assertThatCode(() -> friendshipService.deleteFriendship(1L)).doesNotThrowAnyException();

        verify(friendshipRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteFriendship throws IllegalArgumentException when not found")
    void deleteFriendship_whenNotFound_throwsIllegalArgumentException() {
        when(friendshipRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> friendshipService.deleteFriendship(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Friendship not found");

        verify(friendshipRepository, never()).deleteById(any());
    }

    // -------------------------------------------------------------------------
    // findAllFriendships
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("findAllFriendships returns all friendships from repository")
    void findAllFriendships_returnsList() {
        when(friendshipRepository.findAll()).thenReturn(List.of(friendship));

        List<Friendship> result = friendshipService.findAllFriendships();

        assertThat(result).hasSize(1).containsExactly(friendship);
    }
}
