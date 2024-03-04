package com.mstcc.likesms.services;

import com.mstcc.likesms.dto.CommentDTO;
import com.mstcc.likesms.dto.PostDTO;
import com.mstcc.likesms.dto.UserDTO;
import com.mstcc.likesms.entities.Like;
import com.mstcc.likesms.feignclients.CommentFeignClient;
import com.mstcc.likesms.feignclients.PostFeignClient;
import com.mstcc.likesms.feignclients.UserFeignClient;
import com.mstcc.likesms.repositories.LikeRepository;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class LikeService {

    private final LikeRepository likeRepository;
    private final UserFeignClient userFeignClient;
    private final PostFeignClient postFeignClient;
    private final CommentFeignClient commentFeignClient;

    private static final Logger logger = LoggerFactory.getLogger(LikeService.class);

    @Autowired
    public LikeService(LikeRepository likeRepository, UserFeignClient userFeignClient, PostFeignClient postFeignClient, CommentFeignClient commentFeignClient) {
        this.likeRepository = likeRepository;
        this.userFeignClient = userFeignClient;
        this.postFeignClient = postFeignClient;
        this.commentFeignClient = commentFeignClient;
    }

    public List<Like> findAllLikes() {
        return likeRepository.findAll();
    }

    public Optional<Like> findLikeById(Long id) {
        return likeRepository.findById(id);
    }

    public Like saveLike(Like like) {
        validateEntitiesExistence(like);
        return likeRepository.save(like);
    }

    public void deleteLike(Long id) {
        likeRepository.deleteById(id);
    }

    private void validateEntitiesExistence(Like like) {
        try {
            ResponseEntity<UserDTO> userResponse = userFeignClient.getUserById(like.getUserId());
            if (!userResponse.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("User not found");
            }

            if (like.getPostId() != null) {
                ResponseEntity<PostDTO> postResponse = postFeignClient.getPostById(like.getPostId());
                if (!postResponse.getStatusCode().is2xxSuccessful()) {
                    throw new RuntimeException("Post not found");
                }
            }

            if (like.getCommentId() != null) {
                ResponseEntity<CommentDTO> commentResponse = commentFeignClient.getCommentById(like.getCommentId());
                if (!commentResponse.getStatusCode().is2xxSuccessful()) {
                    throw new RuntimeException("Comment not found");
                }
            }
        } catch (FeignException e) {
            // Log and handle Feign-specific exceptions, such as FeignException.NotFound if a 404 occurred
            logger.error("Error during Feign client call", e);
            throw new RuntimeException("External service call failed", e);
        } catch (Exception e) {
            // Catch other exceptions
            logger.error("Unexpected error", e);
            throw new RuntimeException("An unexpected error occurred", e);
        }
    }

}