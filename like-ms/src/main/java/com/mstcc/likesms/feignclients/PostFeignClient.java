package com.mstcc.likesms.feignclients;

import com.mstcc.likesms.dto.PostDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "post-ms", url = "localhost:8082", path = "/api/posts")
public interface PostFeignClient {

    @GetMapping("/{postId}")
    ResponseEntity<PostDTO> getPostById(@PathVariable("postId") Long postId);
}
