package com.mstcc.postsms.services;

import com.mstcc.postsms.dto.PostDTO;
import com.mstcc.postsms.dto.UserDTO;
import com.mstcc.postsms.entities.Post;
import com.mstcc.postsms.repositories.PostRepository;
import com.mstcc.postsms.feignclients.UserFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final UserFeignClient userFeignClient;

    @Autowired
    public PostService(PostRepository postRepository, UserFeignClient userFeignClient) {
        this.postRepository = postRepository;
        this.userFeignClient = userFeignClient;
    }

    public List<PostDTO> findAllPosts() {
        List<Post> posts = postRepository.findAll();
        return posts.stream().map(this::convertToPostDTO).collect(Collectors.toList());
    }

    public Optional<PostDTO> findPostById(Long id) {
        return postRepository.findById(id).map(this::convertToPostDTO);
    }

    public PostDTO savePost(PostDTO postDto) {
        ResponseEntity<UserDTO> userResponse = userFeignClient.getUserById(postDto.getUser().getId());
        if (!userResponse.getStatusCode().is2xxSuccessful() || userResponse.getBody() == null) {
            throw new RuntimeException("User not found");
        }

        Post post = new Post();
        post.setUserId(postDto.getUser().getId());
        post.setContent(postDto.getContent());
        Post savedPost = postRepository.save(post);
        return convertToPostDTO(savedPost);
    }

    public void deletePost(Long id) {
        postRepository.deleteById(id);
    }

    private PostDTO convertToPostDTO(Post post) {
        ResponseEntity<UserDTO> userResponse = userFeignClient.getUserById(post.getUserId());
        UserDTO userDTO = userResponse.getBody();
        return new PostDTO(post, userDTO);
    }

    public Optional<PostDTO> updatePostContentByUser(Long userId, Long postId, String newContent) {
        return postRepository.findByIdAndUserId(postId, userId).map(post -> {
            post.setContent(newContent);
            Post updatedPost = postRepository.save(post);
            return new PostDTO(updatedPost, userFeignClient.getUserById(userId).getBody());
        });
    }

    public List<PostDTO> getPostsByUser(Long userId) {
        ResponseEntity<UserDTO> userResponse = userFeignClient.getUserById(userId);
        if (!userResponse.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("User not found");
        }

        List<Post> posts = postRepository.findByUserId(userId);
        return posts.stream()
                .map(post -> new PostDTO(post, userResponse.getBody()))
                .collect(Collectors.toList());
    }
}

