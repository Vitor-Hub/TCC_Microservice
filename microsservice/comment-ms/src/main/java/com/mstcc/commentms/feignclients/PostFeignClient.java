package com.mstcc.commentms.feignclients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client for post-ms.
 * Uses the /exists endpoint to avoid circular Feign call chains:
 * comment-ms must not call getPostById because post-ms.getPostById calls comment-ms back.
 */
@FeignClient(name = "post-ms", path = "/api/posts")
public interface PostFeignClient {

    /**
     * Checks whether a post exists by ID.
     * Lightweight alternative to getPostById — does not trigger downstream Feign calls.
     * @param postId the post ID to check
     * @return ResponseEntity containing true if the post exists
     */
    @GetMapping("/{postId}/exists")
    ResponseEntity<Boolean> postExists(@PathVariable("postId") Long postId);
}
