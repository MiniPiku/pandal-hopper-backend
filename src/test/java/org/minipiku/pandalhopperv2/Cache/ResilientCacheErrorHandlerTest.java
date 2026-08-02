package org.minipiku.pandalhopperv2.Cache;

import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.interceptor.AbstractCacheInvoker;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.data.redis.RedisConnectionFailureException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins the degrade-to-source behaviour: a Redis failure must surface as a cache
 * miss, not as an exception escaping to the caller.
 */
class ResilientCacheErrorHandlerTest {

    private final ResilientCacheErrorHandler handler = new ResilientCacheErrorHandler();
    private final Cache cache = new ConcurrentMapCache("pandalsByZone");
    private final RuntimeException redisDown =
            new RedisConnectionFailureException("Unable to connect to Redis");

    @Test
    void swallowsGetErrorsSoTheCallerSeesAMiss() {
        handler.handleCacheGetError(redisDown, cache, "north");
    }

    @Test
    void swallowsPutErrorsSoAnUncacheableValueIsStillReturned() {
        handler.handleCachePutError(redisDown, cache, "north", "value");
    }

    @Test
    void swallowsEvictAndClearErrors() {
        handler.handleCacheEvictError(redisDown, cache, "north");
        handler.handleCacheClearError(redisDown, cache);
    }

    /**
     * The behaviour that actually matters is what {@code AbstractCacheInvoker}
     * does around the handler, so exercise it through that rather than trusting
     * the handler in isolation.
     */
    @Test
    void invokerReturnsCacheMissWithThisHandlerButThrowsWithTheDefault() {
        Cache failing = new ConcurrentMapCache("failing") {
            @Override
            public ValueWrapper get(Object key) {
                throw new RedisConnectionFailureException("Unable to connect to Redis");
            }
        };

        // Default handler: exception escapes, request 500s, DB never consulted.
        assertThatThrownBy(() -> new TestInvoker(new SimpleCacheErrorHandler()).get(failing, "north"))
                .isInstanceOf(RedisConnectionFailureException.class);

        // Ours: null == cache miss, so the interceptor proceeds to the DB call.
        assertThat(new TestInvoker(handler).get(failing, "north")).isNull();
    }

    /** Exposes AbstractCacheInvoker's protected doGet for the test above. */
    private static class TestInvoker extends AbstractCacheInvoker {
        TestInvoker(org.springframework.cache.interceptor.CacheErrorHandler errorHandler) {
            super(errorHandler);
        }

        Cache.ValueWrapper get(Cache cache, Object key) {
            return doGet(cache, key);
        }
    }
}
