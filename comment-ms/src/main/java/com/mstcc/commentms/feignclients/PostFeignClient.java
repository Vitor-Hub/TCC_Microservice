package com.mstcc.commentms.feignclients;

import com.mstcc.commentms.dto.PostDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "post-ms", path = "/api/posts")
public interface PostFeignClient {

    @GetMapping("/{postId}")
    ResponseEntity<PostDTO> getPostById(@PathVariable("postId") Long postId);
}