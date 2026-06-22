package com.eswarr.tokenoptimizer.scheduler;

import com.eswarr.tokenoptimizer.service.SemanticCacheService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled cache maintenance jobs.
 * Logs cache stats and reports metrics to Prometheus every hour.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheEvictionScheduler {

    private final SemanticCacheService cacheService;
    private final MeterRegistry meterRegistry;

    /**
     * Log cache size and update Prometheus gauge every hour.
     * Redis handles TTL-based eviction automatically; this just reports stats.
     */
    @Scheduled(fixedRate = 3_600_000) // 1 hour
    public void reportCacheStats() {
        cacheService.getCacheSize()
            .doOnNext(size -> {
                log.info("Semantic cache size: {} entries", size);
                meterRegistry.gauge("cache.entries.total", size);
            })
            .subscribe();
    }

    /**
     * Warm-up log on startup — useful for verifying Redis connectivity.
     */
    @Scheduled(initialDelay = 5_000, fixedRate = Long.MAX_VALUE)
    public void warmUpLog() {
        cacheService.getCacheSize()
            .doOnNext(size -> log.info("Cache warm-up check: {} existing entries in Redis", size))
            .subscribe();
    }
}
