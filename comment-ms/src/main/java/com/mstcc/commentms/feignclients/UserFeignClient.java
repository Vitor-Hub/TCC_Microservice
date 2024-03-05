package com.mstcc.commentms.feignclients;

import com.mstcc.commentms.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", url = "localhost:8081")
public interface UserFeignClient {

    @GetMapping("/api/users/{userId}")
    ResponseEntity<UserDTO> getUserById(@PathVariable("userId") Long userId);
}