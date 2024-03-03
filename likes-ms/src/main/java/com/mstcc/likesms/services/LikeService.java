package com.mstcc.likesms.services;

import com.mstcc.likesms.entities.Like;
import com.mstcc.likesms.repositories.LikeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class LikeService {

    private final LikeRepository likeRepository;

    @Autowired
    public LikeService(LikeRepository likeRepository) {
        this.likeRepository = likeRepository;
    }

    public Like saveLike(Like like) {
        return likeRepository.save(like);
    }

    public Optional<Like> getLikeById(Long id) {
        return likeRepository.findById(id);
    }

    public void deleteLike(Long id) {
        likeRepository.deleteById(id);
    }
}