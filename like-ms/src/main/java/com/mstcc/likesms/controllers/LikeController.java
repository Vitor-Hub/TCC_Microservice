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
        return likeService.findLikeById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Like> createLike(@RequestBody Like like) {
        Like savedLike = likeService.saveLike(like);
        return new ResponseEntity<>(savedLike, HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLike(@PathVariable Long id) {
        likeService.deleteLike(id);
        return ResponseEntity.ok().build();
    }
}