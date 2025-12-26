package com.example.redis.config;

import io.lettuce.core.SslOptions;
import io.lettuce.core.cluster.ClusterClientOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * Redis 클러스터 설정 클래스 (단일 타겟, 마이그레이션 테스트용)
 * application.properties 변경을 통해 다음 단계들을 지원합니다:
 * 1. 인증/TLS 없는 Redis
 * 2. 인증/TLS 없는 ElastiCache
 * 3. TLS만 있는 ElastiCache
 * 4. 인증 + TLS 있는 ElastiCache
 */
@Configuration
public class RedisConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);

    @Value("${redis.cluster.nodes}")
    private String clusterNodes;

    @Value("${redis.cluster.max-redirects:3}")
    private Integer maxRedirects;

    @Value("${redis.username:}")
    private String username;

    @Value("${redis.password:}")
    private String password;

    @Value("${redis.ssl.enabled:false}")
    private boolean sslEnabled;

    @Value("${redis.ssl.trust-store:}")
    private String trustStore;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        log.info("Redis 연결 설정을 초기화합니다: Nodes={}, SSL={}, User={}", clusterNodes, sslEnabled, username);

        // 1. 클러스터 설정
        List<String> nodes = Arrays.asList(clusterNodes.split(","));
        RedisClusterConfiguration clusterConfiguration = new RedisClusterConfiguration(nodes);

        if (maxRedirects != null) {
            clusterConfiguration.setMaxRedirects(maxRedirects);
        }

        if (username != null && !username.isBlank()) {
            clusterConfiguration.setUsername(username);
        }

        if (password != null && !password.isBlank()) {
            clusterConfiguration.setPassword(RedisPassword.of(password));
        }

        // 2. 클라이언트/SSL 설정
        LettuceClientConfiguration.LettuceClientConfigurationBuilder clientConfigBuilder = LettuceClientConfiguration.builder();

        if (sslEnabled) {
            clientConfigBuilder.useSsl();
            if (trustStore != null && !trustStore.isBlank()) {
                SslOptions sslOptions = SslOptions.builder()
                        .trustManager(new File(trustStore))
                        .build();

                ClusterClientOptions clientOptions = ClusterClientOptions.builder()
                        .sslOptions(sslOptions)
                        .build();

                clientConfigBuilder.clientOptions(clientOptions);
            }
        }

        LettuceClientConfiguration clientConfiguration = clientConfigBuilder.build();
        return new LettuceConnectionFactory(clusterConfiguration, clientConfiguration);
    }

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        return redisTemplate;
    }
}
