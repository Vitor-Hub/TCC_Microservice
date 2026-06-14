package com.mstcc.likesms.services.impl;

import com.mstcc.likesms.dto.PostDTO;
import com.mstcc.likesms.dto.UserDTO;
import com.mstcc.likesms.entities.Like;
import com.mstcc.likesms.exceptions.LikeValidationException;
import com.mstcc.likesms.repositories.LikeRepository;
import com.mstcc.likesms.services.LikeAsyncHelper;
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
 * Unit tests for {@link LikeServiceImpl}.
 *
 * <p>Async helpers are mocked to return immediately-resolved futures,
 * eliminating thread pool and network dependencies from unit tests.
 */
@ExtendWith(MockitoExtension.class)
class LikeServiceImplTest {

    @Mock
    private LikeRepository likeRepository;

    @Mock
    private LikeAsyncHelper asyncHelper;

    @InjectMocks
    private LikeServiceImpl likeService;

    private Like like;
    private UserDTO userDTO;
    private PostDTO postDTO;

    @BeforeEach
    void setUp() {
        like = new Like();
        like.setId(1L);
        like.setUserId(10L);
        like.setPostId(20L);

        userDTO = new UserDTO();
        userDTO.setId(10L);

        postDTO = new PostDTO();
        postDTO.setId(20L);
    }

    // -------------------------------------------------------------------------
    // createAndValidateLike
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("createLike persists like when validation succeeds for a post like")
    void createLike_withPostId_validationSucceeds_savesLike() {
        when(asyncHelper.getUserAsync(10L))
                .thenReturn(CompletableFuture.completedFuture(userDTO));
        when(asyncHelper.getPostAsync(20L))
                .thenReturn(CompletableFuture.completedFuture(postDTO));
        when(likeRepository.save(any(Like.class))).thenReturn(like);

        Like saved = likeService.createAndValidateLike(like);

        assertThat(saved).isEqualTo(like);
        verify(likeRepository).save(like);
    }

    @Test
    @DisplayName("createLike throws IllegalArgumentException when both postId and commentId are null")
    void createLike_withNoTarget_throwsIllegalArgumentException() {
        like.setPostId(null);
        like.setCommentId(null);

        assertThatThrownBy(() -> likeService.createAndValidateLike(like))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Like must reference a post or a comment");

        verify(likeRepository, never()).save(any());
    }

    @Test
    @DisplayName("createLike throws LikeValidationException when upstream user service fails")
    void createLike_whenUserValidationFails_throwsLikeValidationException() {
        when(asyncHelper.getUserAsync(10L))
                .thenReturn(CompletableFuture.failedFuture(
                        new RuntimeException("User not found: 10")));
        when(asyncHelper.getPostAsync(20L))
                .thenReturn(CompletableFuture.completedFuture(postDTO));

        assertThatThrownBy(() -> likeService.createAndValidateLike(like))
                .isInstanceOf(LikeValidationException.class)
                .hasMessageContaining("Validation failed");

        verify(likeRepository, never()).save(any());
    }

    @Test
    @DisplayName("createLike accepts a comment-only like (postId is null)")
    void createLike_withCommentIdOnly_savesLike() {
        like.setPostId(null);
        like.setCommentId(30L);

        when(asyncHelper.getUserAsync(10L))
                .thenReturn(CompletableFuture.completedFuture(userDTO));
        when(asyncHelper.getCommentAsync(30L))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(likeRepository.save(any(Like.class))).thenReturn(like);

        Like saved = likeService.createAndValidateLike(like);

        assertThat(saved).isEqualTo(like);
        verify(asyncHelper, never()).getPostAsync(any());
    }

    // -------------------------------------------------------------------------
    // getLikeById
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getLikeById returns like when found")
    void getLikeById_whenExists_returnsLike() {
        when(likeRepository.findById(1L)).thenReturn(Optional.of(like));

        Optional<Like> result = likeService.getLikeById(1L);

        assertThat(result).isPresent().contains(like);
    }

    @Test
    @DisplayName("getLikeById returns empty when not found")
    void getLikeById_whenNotExists_returnsEmpty() {
        when(likeRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Like> result = likeService.getLikeById(99L);

        assertThat(result).isEmpty();
    }

    // -------------------------------------------------------------------------
    // findLikesByPostId / findLikesByUserId / findLikesByCommentId
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("findLikesByPostId returns likes for given post")
    void findLikesByPostId_returnsList() {
        when(likeRepository.findByPostId(20L)).thenReturn(List.of(like));

        List<Like> result = likeService.findLikesByPostId(20L);

        assertThat(result).hasSize(1).containsExactly(like);
    }

    @Test
    @DisplayName("findLikesByUserId returns likes for given user")
    void findLikesByUserId_returnsList() {
        when(likeRepository.findByUserId(10L)).thenReturn(List.of(like));

        List<Like> result = likeService.findLikesByUserId(10L);

        assertThat(result).hasSize(1).containsExactly(like);
    }

    @Test
    @DisplayName("findLikesByCommentId returns likes for given comment")
    void findLikesByCommentId_returnsList() {
        Like commentLike = new Like();
        commentLike.setId(2L);
        commentLike.setCommentId(30L);

        when(likeRepository.findByCommentId(30L)).thenReturn(List.of(commentLike));

        List<Like> result = likeService.findLikesByCommentId(30L);

        assertThat(result).hasSize(1).containsExactly(commentLike);
    }

    // -------------------------------------------------------------------------
    // updateAndValidateLike
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("updateLike returns updated like when found and validation succeeds")
    void updateLike_whenFoundAndValid_returnsUpdated() {
        Like details = new Like();
        details.setUserId(10L);
        details.setPostId(20L);

        when(likeRepository.findById(1L)).thenReturn(Optional.of(like));
        when(asyncHelper.getUserAsync(10L))
                .thenReturn(CompletableFuture.completedFuture(userDTO));
        when(asyncHelper.getPostAsync(20L))
                .thenReturn(CompletableFuture.completedFuture(postDTO));
        when(likeRepository.save(any(Like.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<Like> result = likeService.updateAndValidateLike(1L, details);

        assertThat(result).isPresent();
    }

    @Test
    @DisplayName("updateLike returns empty when like not found")
    void updateLike_whenNotFound_returnsEmpty() {
        when(likeRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Like> result = likeService.updateAndValidateLike(99L, like);

        assertThat(result).isEmpty();
        verify(likeRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // deleteLike
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("deleteLike removes like by ID when it exists")
    void deleteLike_deletesSuccessfully() {
        when(likeRepository.existsById(1L)).thenReturn(true);

        assertThatCode(() -> likeService.deleteLike(1L)).doesNotThrowAnyException();

        verify(likeRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteLike throws IllegalArgumentException when like does not exist")
    void deleteLike_whenNotFound_throwsIllegalArgumentException() {
        when(likeRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> likeService.deleteLike(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Like not found");

        verify(likeRepository, never()).deleteById(any());
    }

    // -------------------------------------------------------------------------
    // findAllLikes
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("findAllLikes returns list from repository")
    void findAllLikes_returnsList() {
        when(likeRepository.findAll()).thenReturn(List.of(like));

        List<Like> result = likeService.findAllLikes();

        assertThat(result).hasSize(1);
    }
}
