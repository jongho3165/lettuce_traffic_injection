package com.example.redis.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.Arrays;
import java.util.List;

/**
 * Redis 클러스터 설정 클래스
 */
@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.cluster.nodes}")
    private String clusterNodes;

    @Value("${spring.data.redis.cluster.max-redirects}")
    private Integer maxRedirects;

    @Value("${spring.data.redis.password}")
    private String password;

    /**
     * LettuceConnectionFactory 빈 생성 (클러스터 모드)
     * application.properties의 클러스터 노드 정보를 사용합니다.
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        // 쉼표로 구분된 노드 문자열을 리스트로 변환
        List<String> nodes = Arrays.asList(clusterNodes.split(","));
        
        RedisClusterConfiguration clusterConfiguration = new RedisClusterConfiguration(nodes);
        
        if (maxRedirects != null) {
            clusterConfiguration.setMaxRedirects(maxRedirects);
        }
        
        if (password != null && !password.isBlank()) {
            clusterConfiguration.setPassword(RedisPassword.of(password));
        }
        
        return new LettuceConnectionFactory(clusterConfiguration);
    }

    /**
     * RedisTemplate 빈 생성
     * 키와 값을 문자열로 직렬화하여 사용합니다.
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate() {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory());
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        return redisTemplate;
    }
}

