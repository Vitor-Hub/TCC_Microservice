package com.mstcc.postsms.services.impl;

import com.mstcc.postsms.dto.CommentDTO;
import com.mstcc.postsms.dto.PostDTO;
import com.mstcc.postsms.dto.UserDTO;
import com.mstcc.postsms.entities.Post;
import com.mstcc.postsms.repositories.PostRepository;
import com.mstcc.postsms.services.PostAsyncHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PostServiceImpl}.
 *
 * <p>The service is constructed manually so a synchronous {@link Executor} can
 * be injected — this runs {@code CompletableFuture.supplyAsync} tasks on the
 * calling thread, making async flows deterministic in tests without a thread pool.
 *
 * <p>Async helpers are mocked to return immediately-resolved futures.
 */
@ExtendWith(MockitoExtension.class)
class PostServiceImplTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostAsyncHelper asyncHelper;

    /** Synchronous executor: runs tasks on the calling thread, eliminating async non-determinism. */
    private final Executor syncExecutor = Runnable::run;

    private PostServiceImpl postService;

    private Post post;
    private UserDTO userDTO;
    private List<CommentDTO> comments;

    @BeforeEach
    void setUp() {
        postService = new PostServiceImpl(postRepository, asyncHelper, syncExecutor);

        post = new Post();
        post.setId(1L);
        post.setUserId(10L);
        post.setContent("Hello world");
        post.setCreatedAt(LocalDateTime.now());

        userDTO = new UserDTO();
        userDTO.setId(10L);
        userDTO.setUsername("vitor");

        comments = List.of();
    }

    // -------------------------------------------------------------------------
    // getPostById
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getPostById returns PostDTO with user and comments when post exists")
    void getPostById_whenExists_returnsPostDTO() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(asyncHelper.getUserAsync(10L))
                .thenReturn(CompletableFuture.completedFuture(userDTO));
        when(asyncHelper.getCommentsAsync(1L))
                .thenReturn(CompletableFuture.completedFuture(comments));

        Optional<PostDTO> result = postService.getPostById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getContent()).isEqualTo("Hello world");
        assertThat(result.get().getUser().getId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("getPostById returns empty when post does not exist")
    void getPostById_whenNotExists_returnsEmpty() {
        when(postRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<PostDTO> result = postService.getPostById(99L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getPostById returns PostDTO with null user when upstream user service fails (graceful degradation)")
    void getPostById_whenUpstreamFails_returnsDTOWithNullUser() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(asyncHelper.getUserAsync(10L))
                .thenReturn(CompletableFuture.failedFuture(
                        new RuntimeException("user-ms unavailable")));
        when(asyncHelper.getCommentsAsync(1L))
                .thenReturn(CompletableFuture.completedFuture(comments));

        Optional<PostDTO> result = postService.getPostById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getUser()).isNull();
        assertThat(result.get().getContent()).isEqualTo("Hello world");
    }

    // -------------------------------------------------------------------------
    // createPost
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("createPost persists post and returns DTO with user data")
    void createPost_whenUserExists_returnsPostDTO() {
        PostDTO input = new PostDTO();
        input.setContent("New post");
        input.setUser(userDTO);

        when(postRepository.save(any(Post.class))).thenReturn(post);
        when(asyncHelper.getUserAsync(10L))
                .thenReturn(CompletableFuture.completedFuture(userDTO));

        PostDTO result = postService.createPost(input);

        assertThat(result.getContent()).isEqualTo("Hello world");
        assertThat(result.getUser()).isEqualTo(userDTO);
        verify(postRepository).save(any(Post.class));
    }

    @Test
    @DisplayName("createPost returns PostDTO with null user when user fetch fails after save (graceful degradation)")
    void createPost_whenUserFetchFails_returnsDTOWithNullUser() {
        PostDTO input = new PostDTO();
        input.setContent("New post");
        input.setUser(userDTO);

        when(postRepository.save(any(Post.class))).thenReturn(post);
        when(asyncHelper.getUserAsync(10L))
                .thenReturn(CompletableFuture.failedFuture(
                        new RuntimeException("user-ms down")));

        PostDTO result = postService.createPost(input);

        assertThat(result).isNotNull();
        assertThat(result.getUser()).isNull();
        assertThat(result.getContent()).isEqualTo("Hello world");
        verify(postRepository).save(any(Post.class));
    }

    // -------------------------------------------------------------------------
    // updatePostContentByUser
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("updatePostContentByUser returns updated PostDTO when user is owner")
    void updatePostContentByUser_whenOwner_returnsUpdated() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));
        when(asyncHelper.getUserAsync(10L))
                .thenReturn(CompletableFuture.completedFuture(userDTO));
        when(asyncHelper.getCommentsAsync(1L))
                .thenReturn(CompletableFuture.completedFuture(comments));

        Optional<PostDTO> result = postService.updatePostContentByUser(10L, 1L, "Updated content");

        assertThat(result).isPresent();
        assertThat(result.get().getContent()).isEqualTo("Updated content");
    }

    @Test
    @DisplayName("updatePostContentByUser returns empty when userId does not match post owner")
    void updatePostContentByUser_whenNotOwner_returnsEmpty() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));

        Optional<PostDTO> result = postService.updatePostContentByUser(99L, 1L, "Hacked");

        assertThat(result).isEmpty();
        verify(postRepository, never()).save(any());
    }

    @Test
    @DisplayName("updatePostContentByUser returns empty when post not found")
    void updatePostContentByUser_whenPostNotFound_returnsEmpty() {
        when(postRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<PostDTO> result = postService.updatePostContentByUser(10L, 99L, "Content");

        assertThat(result).isEmpty();
    }

    // -------------------------------------------------------------------------
    // deletePost
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("deletePost removes post when it exists")
    void deletePost_whenExists_deletesSuccessfully() {
        when(postRepository.existsById(1L)).thenReturn(true);

        assertThatCode(() -> postService.deletePost(1L)).doesNotThrowAnyException();

        verify(postRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deletePost throws IllegalArgumentException when post does not exist")
    void deletePost_whenNotFound_throwsIllegalArgumentException() {
        when(postRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> postService.deletePost(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Post not found");

        verify(postRepository, never()).deleteById(any());
    }

    // -------------------------------------------------------------------------
    // existsById
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("existsById returns true when post exists")
    void existsById_whenExists_returnsTrue() {
        when(postRepository.existsById(1L)).thenReturn(true);

        assertThat(postService.existsById(1L)).isTrue();
    }

    @Test
    @DisplayName("existsById returns false when post does not exist")
    void existsById_whenNotExists_returnsFalse() {
        when(postRepository.existsById(99L)).thenReturn(false);

        assertThat(postService.existsById(99L)).isFalse();
    }
}
