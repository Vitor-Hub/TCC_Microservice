package com.mstcc.friendshipms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class FriendshipMsApplication {
    public static void main(String[] args) {
        SpringApplication.run(FriendshipMsApplication.class, args);
    }
}