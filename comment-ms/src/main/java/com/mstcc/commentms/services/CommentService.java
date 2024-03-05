package com.mstcc.commentms.services;

import com.mstcc.commentms.dto.CommentDTO;
import com.mstcc.commentms.dto.PostDTO;
import com.mstcc.commentms.dto.UserDTO;
import com.mstcc.commentms.entities.Comment;
import com.mstcc.commentms.feignclients.PostFeignClient;
import com.mstcc.commentms.feignclients.UserFeignClient;
import com.mstcc.commentms.repositories.CommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final UserFeignClient userFeignClient;
    private final PostFeignClient postFeignClient;

    @Autowired
    public CommentService(CommentRepository commentRepository, UserFeignClient userFeignClient, PostFeignClient postFeignClient) {
        this.commentRepository = commentRepository;
        this.userFeignClient = userFeignClient;
        this.postFeignClient = postFeignClient;
    }

    public Optional<Comment> getCommentById(Long id) {
        return commentRepository.findById(id);
    }

    public List<Comment> getAllComments() {
        return commentRepository.findAll();
    }

    public void deleteComment(Long id) {
        commentRepository.deleteById(id);
    }

    public Comment saveComment(Comment comment) {
        validateUserAndPost(comment.getUserId(), comment.getPostId());
        return commentRepository.save(comment);
    }

    private void validateUserAndPost(Long userId, Long postId) {
        ResponseEntity<UserDTO> userResponse = userFeignClient.getUserById(userId);
        ResponseEntity<PostDTO> postResponse = postFeignClient.getPostById(postId);

        if (!userResponse.getStatusCode().is2xxSuccessful() || !postResponse.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("User or Post not found");
        }
    }

    public Optional<CommentDTO> updateComment(Long id, CommentDTO commentDto) {
        return commentRepository.findById(id).map(comment -> {
            comment.setContent(commentDto.getContent());
            Comment updatedComment = commentRepository.save(comment);
            return convertToDto(updatedComment);
        });
    }

    private CommentDTO convertToDto(Comment comment) {
        CommentDTO commentDto = new CommentDTO();
        commentDto.setId(comment.getId());
        commentDto.setContent(comment.getContent());
        commentDto.setCreatedAt(comment.getCreatedAt());

        ResponseEntity<UserDTO> userResponse = userFeignClient.getUserById(comment.getUserId());
        if(userResponse.getStatusCode().is2xxSuccessful() && userResponse.getBody() != null) {
            commentDto.setUser(userResponse.getBody());
        }
        ResponseEntity<PostDTO> postResponse = postFeignClient.getPostById(comment.getPostId());
        if(postResponse.getStatusCode().is2xxSuccessful() && postResponse.getBody() != null) {
            commentDto.setPost(postResponse.getBody());
        }
        return commentDto;
    }

    public List<Comment> getCommentsByPostId(Long postId) {
        return commentRepository.findByPostId(postId);
    }
}