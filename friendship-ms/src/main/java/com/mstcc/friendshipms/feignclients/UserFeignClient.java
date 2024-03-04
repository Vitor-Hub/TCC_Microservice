package com.mstcc.friendshipms.feignclients;

import com.mstcc.friendshipms.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Component
@FeignClient(name = "user-ms", url = "localhost:8081", path = "/api/users")
public interface UserFeignClient {

    @GetMapping("/{id}")
    ResponseEntity<UserDTO> getUserById(@PathVariable Long id);
}
