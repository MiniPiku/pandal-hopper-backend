package org.minipiku.pandalhopperv2.Cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

/**
 * Treats Redis as an optional accelerator rather than a hard dependency.
 *
 * <p>Spring's caching layer already contains the fallback path — {@code
 * AbstractCacheInvoker.doGet} catches the exception, calls the error handler,
 * and then {@code return null}, which the interceptor reads as a cache miss and
 * responds to by invoking the real method (the DB call). That {@code return
 * null} is only reached if the handler returns normally.
 *
 * <p>The default {@link org.springframework.cache.interceptor.SimpleCacheErrorHandler}
 * rethrows on every callback, so the fallback line is dead code and a Redis
 * outage surfaces to the caller as a 500. Logging and swallowing instead lets
 * execution reach it.
 *
 * <p>Scope note: this only covers reads and writes made through the {@code
 * @Cacheable}/{@code @CacheEvict} abstraction. Direct {@code RedisTemplate}
 * users — {@code OAuth2CodeExchangeService} — are unaffected and still fail
 * loudly, which is correct: a one-time login code has no DB fallback.
 */
@Slf4j
public class ResilientCacheErrorHandler implements CacheErrorHandler {

    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        // Swallowing turns this into a cache miss; the caller falls through to
        // the repository. Also covers deserialization failures, not just
        // connection loss.
        log.warn("Cache GET failed for cache='{}' key='{}' - falling back to source. Cause: {}",
                cache.getName(), key, exception.toString());
    }

    @Override
    public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
        // The value was already computed from the DB and is being returned to
        // the caller regardless. Failing to memoize it is not a request failure.
        log.warn("Cache PUT failed for cache='{}' key='{}' - value served uncached. Cause: {}",
                cache.getName(), key, exception.toString());
    }

    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        // Logged at ERROR: a failed evict can leave a stale entry visible once
        // Redis recovers, which is a correctness risk rather than a slowdown.
        log.error("Cache EVICT failed for cache='{}' key='{}' - entry may be stale until TTL expires. Cause: {}",
                cache.getName(), key, exception.toString());
    }

    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
        log.error("Cache CLEAR failed for cache='{}' - entries may be stale until TTL expires. Cause: {}",
                cache.getName(), exception.toString());
    }
}
