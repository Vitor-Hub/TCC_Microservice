package com.mstcc.postsms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Main application class for Post Microservice
 * Enables Feign clients to communicate with User and Comment services
 */
@EnableFeignClients
@SpringBootApplication
public class PostMsApplication {

    private static final Logger logger = LoggerFactory.getLogger(PostMsApplication.class);

    public static void main(String[] args) {
        logger.info("========================================");
        logger.info("Starting Post Microservice...");
        logger.info("========================================");
        
        SpringApplication.run(PostMsApplication.class, args);
        
        logger.info("========================================");
        logger.info("Post Microservice started successfully!");
        logger.info("========================================");
    }
}