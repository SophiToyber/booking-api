package com.booking.config;

import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;


@Slf4j
@Configuration
@EnableCaching
@Profile("!test")
public class RedisConfig implements CachingConfigurer {

  @Bean
  public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
    log.info("Initializing Redis Cache Manager");

    GenericJackson2JsonRedisSerializer serializer =
        new GenericJackson2JsonRedisSerializer();

    RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofMinutes(30))
        .serializeKeysWith(
            RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer())
        )
        .serializeValuesWith(
            RedisSerializationContext.SerializationPair
                .fromSerializer(serializer)
        )
        .disableCachingNullValues();

    log.info("Redis Cache Configuration: TTL=30min, Serializer=GenericJackson2JsonRedisSerializer");

    return RedisCacheManager.builder(connectionFactory)
        .cacheDefaults(config)
        .build();
  }

  @Override
  public CacheErrorHandler errorHandler() {
    return new CacheErrorHandler() {
      @Override
      public void handleCacheGetError(RuntimeException exception,
          org.springframework.cache.Cache cache, Object key) {
        log.error("Cache GET failed for key={} in cache={}: {}",
            key, cache.getName(), exception.getMessage());
      }

      @Override
      public void handleCachePutError(RuntimeException exception,
          org.springframework.cache.Cache cache, Object key, Object value) {
        log.error("Cache PUT failed for key={} in cache={}: {}",
            key, cache.getName(), exception.getMessage());
      }

      @Override
      public void handleCacheEvictError(RuntimeException exception,
          org.springframework.cache.Cache cache, Object key) {
        log.error("Cache EVICT failed for key={} in cache={}: {}",
            key, cache.getName(), exception.getMessage());
      }

      @Override
      public void handleCacheClearError(RuntimeException exception,
          org.springframework.cache.Cache cache) {
        log.error("Cache CLEAR failed in cache={}: {}",
            cache.getName(), exception.getMessage());
      }
    };
  }
}