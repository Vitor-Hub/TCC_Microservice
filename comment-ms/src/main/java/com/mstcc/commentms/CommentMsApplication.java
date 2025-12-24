package com.mstcc.commentms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Main application class for Comment Microservice
 * Enables Feign clients to communicate with User and Post services
 */
@EnableFeignClients
@SpringBootApplication
public class CommentMsApplication {

    private static final Logger logger = LoggerFactory.getLogger(CommentMsApplication.class);

    public static void main(String[] args) {
        logger.info("========================================");
        logger.info("Starting Comment Microservice...");
        logger.info("========================================");
        
        SpringApplication.run(CommentMsApplication.class, args);
        
        logger.info("========================================");
        logger.info("Comment Microservice started successfully!");
        logger.info("========================================");
    }
}