package com.mstcc.postsms.services;

import com.mstcc.postsms.dto.CommentDTO;
import com.mstcc.postsms.dto.PostDTO;
import com.mstcc.postsms.dto.UserDTO;
import com.mstcc.postsms.entities.Post;
import com.mstcc.postsms.feignclients.CommentFeignClient;
import com.mstcc.postsms.repositories.PostRepository;
import com.mstcc.postsms.feignclients.UserFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final UserFeignClient userFeignClient;
    private final CommentFeignClient commentFeignClient;

    @Autowired
    public PostService(PostRepository postRepository, UserFeignClient userFeignClient, CommentFeignClient commentFeignClient) {
        this.postRepository = postRepository;
        this.userFeignClient = userFeignClient;
        this.commentFeignClient = commentFeignClient;
    }

    public List<PostDTO> getAllPosts() {
        List<PostDTO> posts = postRepository.findAll().stream()
                .map(this::convertToPostDTOWithComments)
                .collect(Collectors.toList());
        return posts;
    }

    public Optional<PostDTO> getPostById(Long id) {
        Optional<Post> post = postRepository.findById(id);
        return post.map(this::convertToPostDTOWithComments);
    }

    public PostDTO createPost(PostDTO postDto) {
        ResponseEntity<UserDTO> userResponse = userFeignClient.getUserById(postDto.getUser().getId());
        if (!userResponse.getStatusCode().is2xxSuccessful() || userResponse.getBody() == null) {
            throw new RuntimeException("User not found");
        }

        Post post = new Post();
        post.setUserId(postDto.getUser().getId());
        post.setContent(postDto.getContent());
        Post savedPost = postRepository.save(post);
        return convertToPostDTOWithComments(savedPost);
    }

    public void deletePost(Long id) {
        postRepository.deleteById(id);
    }

    public PostDTO updatePost(Long id, PostDTO postDto) {
        Optional<Post> optionalPost = postRepository.findById(id);
        if (optionalPost.isPresent()) {
            Post post = optionalPost.get();
            post.setContent(postDto.getContent());
            postRepository.save(post);
            return convertToPostDTOWithComments(post);
        } else {
            throw new RuntimeException("Post not found");
        }
    }

    private PostDTO convertToPostDTOWithComments(Post post) {
        ResponseEntity<UserDTO> userResponse = userFeignClient.getUserById(post.getUserId());
        UserDTO userDTO = userResponse.getBody();
        ResponseEntity<List<CommentDTO>> commentsResponse = commentFeignClient.getCommentsByPostId(post.getId());
        List<CommentDTO> comments = commentsResponse.getBody();
        return new PostDTO(post, userDTO, comments);
    }

    public Optional<PostDTO> updatePostContentByUser(Long userId, Long postId, String newContent) {
        return postRepository.findByIdAndUserId(postId, userId).map(post -> {
            post.setContent(newContent);
            Post updatedPost = postRepository.save(post);
            ResponseEntity<UserDTO> userResponse = userFeignClient.getUserById(userId);
            if (!userResponse.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("User not found");
            }
            ResponseEntity<List<CommentDTO>> commentsResponse = commentFeignClient.getCommentsByPostId(postId);
            List<CommentDTO> comments = commentsResponse.getBody();
            return new PostDTO(updatedPost, userResponse.getBody(), comments);
        });
    }

    public List<PostDTO> getPostsByUser(Long userId) {
        ResponseEntity<UserDTO> userResponse = userFeignClient.getUserById(userId);
        if (!userResponse.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("User not found");
        }

        List<Post> posts = postRepository.findByUserId(userId);
        List<PostDTO> postDTOs = new ArrayList<>();

        for (Post post : posts) {
            ResponseEntity<List<CommentDTO>> commentsResponse = commentFeignClient.getCommentsByPostId(post.getId());
            if (commentsResponse.getStatusCode().is2xxSuccessful()) {
                List<CommentDTO> comments = commentsResponse.getBody();
                PostDTO postDTO = new PostDTO(post, userResponse.getBody(), comments);
                postDTOs.add(postDTO);
            }
        }

        return postDTOs;
    }
}