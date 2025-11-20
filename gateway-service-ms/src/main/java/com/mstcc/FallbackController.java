package com.mstcc;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Fallback controller for Circuit Breaker
 * Returns friendly error messages when services are unavailable
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/user")
    public ResponseEntity<Map<String, Object>> userServiceFallback() {
        return buildFallbackResponse("User Service", "temporarily unavailable");
    }

    @GetMapping("/post")
    public ResponseEntity<Map<String, Object>> postServiceFallback() {
        return buildFallbackResponse("Post Service", "temporarily unavailable");
    }

    @GetMapping("/comment")
    public ResponseEntity<Map<String, Object>> commentServiceFallback() {
        return buildFallbackResponse("Comment Service", "temporarily unavailable");
    }

    @GetMapping("/like")
    public ResponseEntity<Map<String, Object>> likeServiceFallback() {
        return buildFallbackResponse("Like Service", "temporarily unavailable");
    }

    @GetMapping("/friendship")
    public ResponseEntity<Map<String, Object>> friendshipServiceFallback() {
        return buildFallbackResponse("Friendship Service", "temporarily unavailable");
    }

    private ResponseEntity<Map<String, Object>> buildFallbackResponse(String serviceName, String reason) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("service", serviceName);
        response.put("status", "unavailable");
        response.put("message", serviceName + " is " + reason + ". Please try again later.");
        response.put("suggestion", "The service may be experiencing high load or is being restarted.");
        
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(response);
    }
}