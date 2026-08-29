package br.ufpr.sept.so2.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.time.Duration

/**
 * Redis cache manager configuration.
 *
 * Active when `spring.cache.type=redis` (set via CACHE_TYPE env var in Docker Compose).
 * Falls back to the Spring Boot `simple` (in-memory) cache manager in local dev without Redis.
 *
 * TTL policy:
 * - `bff-dashboard`: 60 s — dashboard aggregates query several tables; short TTL keeps data fresh
 *   without hammering the DB on every page load.
 *
 * NOTE: The Redis instance is configured with `maxmemory-policy noeviction` so auth session keys
 * are never silently dropped under memory pressure. Cache keys all have TTLs, so they are
 * bounded in size. If the 256 MB limit is reached, writes will fail (logged as errors) rather
 * than silently evicting auth data.
 */
@Configuration
@ConditionalOnProperty(name = ["spring.cache.type"], havingValue = "redis")
class CacheConfig {
    private val defaultTtl = Duration.ofSeconds(60)

    private val cacheTtls: Map<String, Duration> = mapOf(
        "bff-dashboard" to Duration.ofSeconds(60),
    )

    @Bean
    fun redisCacheManager(connectionFactory: RedisConnectionFactory): RedisCacheManager {
        val defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(defaultTtl)
            .disableCachingNullValues()
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer()),
            )
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    GenericJackson2JsonRedisSerializer(),
                ),
            )

        val perCacheConfig = cacheTtls.mapValues { (_, ttl) ->
            defaultConfig.entryTtl(ttl)
        }

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(perCacheConfig)
            .build()
    }
}
