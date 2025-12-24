package com.mstcc.friendshipms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Main application class for Friendship Microservice
 * Enables Feign clients to communicate with User service
 */
@EnableFeignClients
@SpringBootApplication
public class FriendshipMsApplication {

    private static final Logger logger = LoggerFactory.getLogger(FriendshipMsApplication.class);

    public static void main(String[] args) {
        logger.info("========================================");
        logger.info("Starting Friendship Microservice...");
        logger.info("========================================");
        
        SpringApplication.run(FriendshipMsApplication.class, args);
        
        logger.info("========================================");
        logger.info("Friendship Microservice started successfully!");
        logger.info("========================================");
    }
}