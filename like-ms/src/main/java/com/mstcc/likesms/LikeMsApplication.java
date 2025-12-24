package com.mstcc.likesms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Main application class for Like Microservice
 * Enables Feign clients to communicate with User, Post, and Comment services
 */
@EnableFeignClients
@SpringBootApplication
public class LikeMsApplication {

    private static final Logger logger = LoggerFactory.getLogger(LikeMsApplication.class);

    public static void main(String[] args) {
        logger.info("========================================");
        logger.info("Starting Like Microservice...");
        logger.info("========================================");
        
        SpringApplication.run(LikeMsApplication.class, args);
        
        logger.info("========================================");
        logger.info("Like Microservice started successfully!");
        logger.info("========================================");
    }
}