package com.mstcc.likesms.feignclients;

import com.mstcc.likesms.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Component
@FeignClient(name = "user-ms", path = "/api/users")
public interface UserFeignClient {

    @GetMapping("/{id}")
    ResponseEntity<UserDTO> getUserById(@PathVariable Long id);
}
