package com.mstcc.commentms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class CommentMsApplication {
    public static void main(String[] args) {
        SpringApplication.run(CommentMsApplication.class, args);
    }
}