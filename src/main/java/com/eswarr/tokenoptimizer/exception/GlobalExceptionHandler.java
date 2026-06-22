package com.eswarr.tokenoptimizer.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.Instant;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BudgetExceededException.class)
    public ResponseEntity<Map<String, Object>> handleBudgetExceeded(BudgetExceededException ex) {
        log.warn("Budget exceeded: teamId={} usage={}/{}", ex.getTeamId(), ex.getCurrentUsage(), ex.getLimit());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .body(Map.of(
                "error",        "BUDGET_EXCEEDED",
                "message",       ex.getMessage(),
                "teamId",        ex.getTeamId(),
                "usagePercent",  String.format("%.1f%%", ex.getUsagePercent() * 100),
                "timestamp",     Instant.now().toString(),
                "retryAfter",    "Try again after midnight UTC when daily budget resets"
            ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
            .body(Map.of("error", "BAD_REQUEST", "message", ex.getMessage(), "timestamp", Instant.now().toString()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(RuntimeException ex) {
        log.error("Unhandled runtime exception: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("error", "INTERNAL_ERROR", "message", "An error occurred processing the request",
                         "timestamp", Instant.now().toString()));
    }
}
