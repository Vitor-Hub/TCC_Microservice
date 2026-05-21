package com.mstcc.commentms.services.impl;

import com.mstcc.commentms.dto.UserDTO;
import com.mstcc.commentms.entities.Comment;
import com.mstcc.commentms.repositories.CommentRepository;
import com.mstcc.commentms.services.CommentAsyncHelper;
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
 * Unit tests for {@link CommentServiceImpl}.
 *
 * <p>Async helpers are mocked to return immediately-resolved futures,
 * eliminating thread pool and network dependencies from unit tests.
 */
@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CommentAsyncHelper asyncHelper;

    @InjectMocks
    private CommentServiceImpl commentService;

    private Comment comment;
    private UserDTO userDTO;

    @BeforeEach
    void setUp() {
        comment = new Comment();
        comment.setId(1L);
        comment.setUserId(10L);
        comment.setPostId(20L);
        comment.setContent("Test comment");

        userDTO = new UserDTO();
        userDTO.setId(10L);
        userDTO.setUsername("vitor");
    }

    // -------------------------------------------------------------------------
    // createAndValidateComment
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("createComment persists comment when upstream validation succeeds")
    void createComment_whenValidationSucceeds_savesComment() throws Exception {
        when(asyncHelper.getUserAsync(10L))
                .thenReturn(CompletableFuture.completedFuture(userDTO));
        when(asyncHelper.postExistsAsync(20L))
                .thenReturn(CompletableFuture.completedFuture(true));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        Comment saved = commentService.createAndValidateComment(comment);

        assertThat(saved).isEqualTo(comment);
        verify(commentRepository).save(comment);
    }

    @Test
    @DisplayName("createComment throws RuntimeException when upstream validation fails")
    void createComment_whenValidationFails_throwsRuntimeException() {
        when(asyncHelper.getUserAsync(10L))
                .thenReturn(CompletableFuture.failedFuture(
                        new RuntimeException("User not found: 10")));
        when(asyncHelper.postExistsAsync(20L))
                .thenReturn(CompletableFuture.completedFuture(true));

        assertThatThrownBy(() -> commentService.createAndValidateComment(comment))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Validation failed");

        verify(commentRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // getCommentById
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getCommentById returns comment when found")
    void getCommentById_whenExists_returnsComment() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        Optional<Comment> result = commentService.getCommentById(1L);

        assertThat(result).isPresent().contains(comment);
    }

    @Test
    @DisplayName("getCommentById returns empty when not found")
    void getCommentById_whenNotExists_returnsEmpty() {
        when(commentRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Comment> result = commentService.getCommentById(99L);

        assertThat(result).isEmpty();
    }

    // -------------------------------------------------------------------------
    // findCommentsByPostId
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("findCommentsByPostId returns all comments for given post")
    void findCommentsByPostId_returnsList() {
        when(commentRepository.findByPostId(20L)).thenReturn(List.of(comment));

        List<Comment> result = commentService.findCommentsByPostId(20L);

        assertThat(result).hasSize(1).containsExactly(comment);
    }

    @Test
    @DisplayName("findCommentsByPostId returns empty list when no comments exist")
    void findCommentsByPostId_whenNone_returnsEmptyList() {
        when(commentRepository.findByPostId(99L)).thenReturn(List.of());

        List<Comment> result = commentService.findCommentsByPostId(99L);

        assertThat(result).isEmpty();
    }

    // -------------------------------------------------------------------------
    // findCommentsByUserId
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("findCommentsByUserId returns all comments for given user")
    void findCommentsByUserId_returnsList() {
        when(commentRepository.findByUserId(10L)).thenReturn(List.of(comment));

        List<Comment> result = commentService.findCommentsByUserId(10L);

        assertThat(result).hasSize(1).containsExactly(comment);
    }

    // -------------------------------------------------------------------------
    // updateAndValidateComment
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("updateComment returns updated comment when found and validation succeeds")
    void updateComment_whenFoundAndValid_returnsUpdated() {
        Comment details = new Comment();
        details.setUserId(10L);
        details.setPostId(20L);
        details.setContent("Updated content");

        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(asyncHelper.getUserAsync(10L))
                .thenReturn(CompletableFuture.completedFuture(userDTO));
        when(asyncHelper.postExistsAsync(20L))
                .thenReturn(CompletableFuture.completedFuture(true));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<Comment> result = commentService.updateAndValidateComment(1L, details);

        assertThat(result).isPresent();
        assertThat(result.get().getContent()).isEqualTo("Updated content");
    }

    @Test
    @DisplayName("updateComment returns empty when comment not found")
    void updateComment_whenNotFound_returnsEmpty() {
        when(commentRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Comment> result = commentService.updateAndValidateComment(99L, comment);

        assertThat(result).isEmpty();
        verify(commentRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // deleteComment
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("deleteComment removes comment when found")
    void deleteComment_whenFound_deletesSuccessfully() {
        when(commentRepository.existsById(1L)).thenReturn(true);

        assertThatCode(() -> commentService.deleteComment(1L)).doesNotThrowAnyException();

        verify(commentRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteComment throws IllegalArgumentException when not found")
    void deleteComment_whenNotFound_throwsIllegalArgumentException() {
        when(commentRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> commentService.deleteComment(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Comment not found");

        verify(commentRepository, never()).deleteById(any());
    }
}
