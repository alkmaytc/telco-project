package com.telco.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer; // 🎯 En güncel üst interface
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
public class RedisConfig {

    /**
     * 🎯 MADDE 9: Spring Boot 4+ mimarisine %100 uyumlu, sıfır "deprecated" uyarısı veren CacheManager ✅
     */
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {

        // 🎯 EN GÜNCEL STANDART: new Jackson2JsonRedisSerializer() yerine doğrudan kütüphanenin sunduğu
        // hazır ve optimize edilmiş yerleşik JSON serileştirici motorunu çağırıyoruz. Kırmızılıklar bitti! ✅
        RedisSerializer<Object> jsonSerializer = RedisSerializer.json();

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(2)) // Önbellek ömrü: 2 Saat ⏳
                .disableCachingNullValues()    // Boş değerlerin gereksiz yer kaplamasını önler
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }
}