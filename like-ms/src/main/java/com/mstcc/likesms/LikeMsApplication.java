package com.mstcc.likesms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class LikeMsApplication {

    public static void main(String[] args) {
        SpringApplication.run(LikeMsApplication.class, args);
    }

}