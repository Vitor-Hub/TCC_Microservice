package com.mstcc.likesms.controllers;

import com.mstcc.likesms.entities.Like;
import com.mstcc.likesms.services.LikeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/likes")
public class LikeController {

    private final LikeService likeService;

    @Autowired
    public LikeController(LikeService likeService) {
        this.likeService = likeService;
    }

    @GetMapping
    public ResponseEntity<List<Like>> getAllLikes() {
        List<Like> likes = likeService.findAllLikes();
        return ResponseEntity.ok(likes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Like> getLikeById(@PathVariable Long id) {
        return likeService.getLikeById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Like> createLike(@RequestBody Like like) {
        Like savedLike = likeService.createAndValidateLike(like);
        if (savedLike == null) {
            return ResponseEntity.badRequest().build();
        }
        return new ResponseEntity<>(savedLike, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Like> updateLike(@PathVariable Long id, @RequestBody Like likeDetails) {
        return likeService.updateAndValidateLike(id, likeDetails)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLike(@PathVariable Long id) {
        likeService.deleteLike(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/post/{postId}")
    public ResponseEntity<List<Like>> getLikesByPostId(@PathVariable Long postId) {
        List<Like> likes = likeService.findLikesByPostId(postId);
        return ResponseEntity.ok(likes);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Like>> getLikesByUserId(@PathVariable Long userId) {
        List<Like> likes = likeService.findLikesByUserId(userId);
        return ResponseEntity.ok(likes);
    }

    @GetMapping("/comment/{commentId}")
    public ResponseEntity<List<Like>> getLikesByCommentId(@PathVariable Long commentId) {
        List<Like> likes = likeService.findLikesByCommentId(commentId);
        return ResponseEntity.ok(likes);
    }
}