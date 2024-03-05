package com.mstcc.postsms.controllers;

import com.mstcc.postsms.dto.PostDTO;
import com.mstcc.postsms.services.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    @Autowired
    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping
    public ResponseEntity<List<PostDTO>> getAllPosts() {
        List<PostDTO> posts = postService.getAllPosts();
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostDTO> getPostById(@PathVariable Long id) {
        return postService.getPostById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<PostDTO> createPost(@RequestBody PostDTO postDto) {
        PostDTO savedPostDto = postService.createPost(postDto);
        return ResponseEntity.ok(savedPostDto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id) {
        postService.deletePost(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/user/{userId}/posts/{postId}")
    public ResponseEntity<PostDTO> updatePostContent(@PathVariable Long userId, @PathVariable Long postId, @RequestBody PostDTO postDto) {
        return postService.updatePostContentByUser(userId, postId, postDto.getContent())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PostDTO>> getPostsByUser(@PathVariable Long userId) {
        List<PostDTO> postDTOs = postService.getPostsByUser(userId);
        return ResponseEntity.ok(postDTOs);
    }
}