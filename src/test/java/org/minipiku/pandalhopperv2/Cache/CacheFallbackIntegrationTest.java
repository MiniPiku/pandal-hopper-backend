package org.minipiku.pandalhopperv2.Cache;

import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.RedisConnectionFailureException;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Reproduces the reported failure through a real Spring cache proxy: when the
 * cache store is unreachable, a {@code @Cacheable} method must still reach its
 * body (the DB call) instead of propagating the connection error.
 *
 * <p>Uses an always-failing in-memory cache rather than a real Redis so the
 * outage is deterministic and the test needs no infrastructure.
 */
class CacheFallbackIntegrationTest {

    /** Stands in for PandalService: counts how often the "DB" was consulted. */
    static class PandalRepositoryStub {
        private final AtomicInteger dbCalls = new AtomicInteger();

        @Cacheable(value = "pandalsByZone", key = "#zone")
        public List<String> getPandalsByZone(String zone) {
            dbCalls.incrementAndGet();
            return List.of("pandal-in-" + zone);
        }

        // Must be a method, not a field read: the injected bean is a CGLIB
        // subclass proxy whose own fields are never populated, so
        // `bean.dbCalls` would read null rather than the target's counter.
        public int dbCalls() {
            return dbCalls.get();
        }
    }

    /** Every operation fails, as Redis would during an outage. */
    static class UnreachableCache extends ConcurrentMapCache {
        UnreachableCache(String name) {
            super(name);
        }

        @Override
        public ValueWrapper get(Object key) {
            throw new RedisConnectionFailureException("Unable to connect to Redis");
        }

        @Override
        public void put(Object key, Object value) {
            throw new RedisConnectionFailureException("Unable to connect to Redis");
        }
    }

    private static CacheManager unreachableCacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of((Cache) new UnreachableCache("pandalsByZone")));
        return manager;
    }

    /** Current behaviour before the fix: no error handler configured. */
    @Configuration
    @EnableCaching
    static class WithoutHandler {
        @Bean
        public CacheManager cacheManager() {
            return unreachableCacheManager();
        }

        @Bean
        public PandalRepositoryStub service() {
            return new PandalRepositoryStub();
        }
    }

    /**
     * Mirrors the real {@link CacheConfig}: same wiring plus the handler.
     * Declared flat rather than extending WithoutHandler — @Configuration
     * inheritance combined with CachingConfigurer's default cacheManager()
     * breaks factory-method resolution.
     */
    @Configuration
    @EnableCaching
    static class WithHandler implements CachingConfigurer {
        @Bean
        public CacheManager cacheManager() {
            return unreachableCacheManager();
        }

        @Bean
        public PandalRepositoryStub service() {
            return new PandalRepositoryStub();
        }

        @Override
        public CacheErrorHandler errorHandler() {
            return new ResilientCacheErrorHandler();
        }
    }

    @Test
    void withoutHandler_redisOutageBreaksTheRequest() {
        try (var ctx = new AnnotationConfigApplicationContext(WithoutHandler.class)) {
            PandalRepositoryStub service = ctx.getBean(PandalRepositoryStub.class);

            assertThatThrownBy(() -> service.getPandalsByZone("north"))
                    .isInstanceOf(RedisConnectionFailureException.class);

            // The reported symptom: the database was never reached.
            assertThat(service.dbCalls()).isZero();
        }
    }

    @Test
    void withHandler_redisOutageFallsThroughToTheDatabase() {
        try (var ctx = new AnnotationConfigApplicationContext(WithHandler.class)) {
            PandalRepositoryStub service = ctx.getBean(PandalRepositoryStub.class);

            assertThat(service.getPandalsByZone("north")).containsExactly("pandal-in-north");
            assertThat(service.dbCalls()).isEqualTo(1);

            // Still serving on repeat calls; each is a miss that hits the DB,
            // which is the correct degraded mode.
            assertThat(service.getPandalsByZone("north")).containsExactly("pandal-in-north");
            assertThat(service.dbCalls()).isEqualTo(2);
        }
    }
}
