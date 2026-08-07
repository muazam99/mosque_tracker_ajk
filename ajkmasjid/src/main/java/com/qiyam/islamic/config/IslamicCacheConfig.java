package com.qiyam.islamic.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Backs {@code @Cacheable} on the public Islamic API with per-cache TTL/size limits via
 * Caffeine (an in-memory Spring Cache implementation — no Redis is deployed for this app).
 * Cache key granularity is deliberately per location+date / per date / per year+month, matching
 * the spec, so a public landing page under real traffic doesn't hit the upstream providers once
 * per visitor.
 */
@Configuration
@EnableCaching
public class IslamicCacheConfig {

    @Bean
    public CacheManager cacheManager() {
        var manager = new CaffeineCacheManager();
        manager.registerCustomCache("prayerTimes",
                Caffeine.newBuilder().expireAfterWrite(Duration.ofHours(12)).maximumSize(20_000).build());
        manager.registerCustomCache("hijriDates",
                Caffeine.newBuilder().expireAfterWrite(Duration.ofHours(24)).maximumSize(2_000).build());
        manager.registerCustomCache("islamicEvents",
                Caffeine.newBuilder().expireAfterWrite(Duration.ofHours(24)).maximumSize(500).build());
        manager.registerCustomCache("quran",
                Caffeine.newBuilder().expireAfterWrite(Duration.ofDays(7)).maximumSize(10_000).build());
        manager.registerCustomCache("hadith",
                Caffeine.newBuilder().expireAfterWrite(Duration.ofDays(7)).maximumSize(10_000).build());
        return manager;
    }
}
