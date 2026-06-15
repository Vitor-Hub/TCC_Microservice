package com.mstcc.likesms.feignclients;

import com.mstcc.likesms.dto.CommentDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "comment-ms", path = "/api/comments")
public interface CommentFeignClient {

    @GetMapping("/{commentId}")
    ResponseEntity<CommentDTO> getCommentById(@PathVariable("commentId") Long commentId);
}

