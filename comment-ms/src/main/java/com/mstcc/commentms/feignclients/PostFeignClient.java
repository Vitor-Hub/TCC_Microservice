package com.mstcc.commentms.feignclients;

import com.mstcc.commentms.dto.PostDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "post-service", url = "localhost:8082")
public interface PostFeignClient {

    @GetMapping("/api/posts/{postId}")
    ResponseEntity<PostDTO> getPostById(@PathVariable("postId") Long postId);
}