package com.mstcc.monolith;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the monolithic TCC reference application.
 *
 * <p>All five business domains (user, post, comment, like, friendship) are
 * co-located in this single deployable unit and share a single PostgreSQL
 * database. Inter-domain communication happens via direct Spring bean injection
 * instead of HTTP/Feign, eliminating network hops and simplifying failure modes.
 */
@SpringBootApplication
public class MonolithApplication {

    /**
     * Application entry point.
     *
     * @param args command-line arguments passed through to Spring
     */
    public static void main(String[] args) {
        SpringApplication.run(MonolithApplication.class, args);
    }
}
