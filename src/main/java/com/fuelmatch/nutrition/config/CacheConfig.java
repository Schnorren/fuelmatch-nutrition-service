package com.fuelmatch.nutrition.config;

import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Configuração de cache Redis com TTLs diferenciados por tipo de dado.
 *
 * <p>Estratégia de TTL:
 * <ul>
 *   <li>{@code food-detail} — 24h: detalhes de um alimento mudam raramente</li>
 *   <li>{@code food-barcode} — 12h: barcode lookup com fallback OFF</li>
 *   <li>{@code food-search} — 5min: resultados de busca (mais dinâmico)</li>
 * </ul>
 */
@Configuration
@EnableCaching
public class CacheConfig {

    private static final GenericJackson2JsonRedisSerializer JSON_SERIALIZER =
            new GenericJackson2JsonRedisSerializer();

    @Bean
    public RedisCacheManagerBuilderCustomizer cacheManagerCustomizer() {
        return builder -> builder
                .withCacheConfiguration("food-detail",
                        defaultConfig().entryTtl(Duration.ofHours(24)))
                .withCacheConfiguration("food-barcode",
                        defaultConfig().entryTtl(Duration.ofHours(12)))
                .withCacheConfiguration("food-search",
                        defaultConfig().entryTtl(Duration.ofMinutes(5)));
    }

    private RedisCacheConfiguration defaultConfig() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(JSON_SERIALIZER));
    }
}
