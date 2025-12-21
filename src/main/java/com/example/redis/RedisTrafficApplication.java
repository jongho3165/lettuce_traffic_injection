package com.example.redis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Redis 트래픽 생성 애플리케이션 메인 클래스
 */
@SpringBootApplication
public class RedisTrafficApplication {

    public static void main(String[] args) {
        SpringApplication.run(RedisTrafficApplication.class, args);
    }

}

