package com.eswarr.tokenoptimizer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * AI Token Optimizer — LLM API Gateway
 *
 * Reduces AI token costs by 60-80% through:
 *  - Semantic caching (Redis vector similarity)
 *  - Intelligent model routing (cheap vs expensive models)
 *  - Prompt compression (strip redundant tokens)
 *  - Budget enforcement (per-agent/team caps)
 *
 * @author Eswarr Dasi
 */
@SpringBootApplication
@EnableScheduling
public class TokenOptimizerApplication {

    public static void main(String[] args) {
        SpringApplication.run(TokenOptimizerApplication.class, args);
    }
}
