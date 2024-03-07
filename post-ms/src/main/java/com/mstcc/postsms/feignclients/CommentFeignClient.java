package com.mstcc.postsms.feignclients;

import com.mstcc.postsms.dto.CommentDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "comment-ms", path = "/api/comments")
public interface CommentFeignClient {

    @GetMapping("/post/{postId}")
    ResponseEntity<List<CommentDTO>> getCommentsByPostId(@PathVariable("postId") Long postId);
}
