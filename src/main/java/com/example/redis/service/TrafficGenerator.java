package com.example.redis.service;

import io.lettuce.core.cluster.SlotHash;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.connection.RedisClusterConnection;
import org.springframework.data.redis.connection.RedisClusterNode;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Redis 트래픽 생성 서비스 (클러스터 지원 및 노드 로깅)
 */
@Service
public class TrafficGenerator implements CommandLineRunner {

    private static final Logger logger = LogManager.getLogger(TrafficGenerator.class);

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public void run(String... args) throws Exception {
        logger.info("Redis 클러스터 트래픽 생성을 시작합니다.");

        while (true) {
            String key = "";
            try {
                // 시간 기반의 고유 키 생성
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSSSSS"));
                key = "timekey:" + timestamp + ":" + UUID.randomUUID().toString().substring(0, 8);
                String value = "val:" + timestamp;

                // 키가 저장될 노드 정보 확인 및 로깅
                logTargetNode(key);

                // 키 저장 (Set)
                redisTemplate.opsForValue().set(key, value);
                logger.info("키 저장 성공: Key={}", key);

                // 키 조회 (Get)
                String retrievedValue = redisTemplate.opsForValue().get(key);
                logger.info("키 조회 성공: Key={}, Value={}", key, retrievedValue);

                // 너무 빠른 루프 방지 (1초 대기)
                TimeUnit.SECONDS.sleep(1);

            } catch (Exception e) {
                logger.error("Redis 작업 실패! Key={}", key);
                logger.error("에러 메시지: {}", e.getMessage());
                // 상세 스택 트레이스는 필요 시 주석 해제
                // logger.error("Stack Trace:", e);
                
                TimeUnit.SECONDS.sleep(1);
            }
        }
    }

    /**
     * 키가 저장될 슬롯을 계산하고, 해당 슬롯을 담당하는 노드(Master)를 찾아 로깅합니다.
     */
    private void logTargetNode(String key) {
        try {
            RedisConnectionFactory factory = redisTemplate.getConnectionFactory();
            if (factory != null) {
                RedisConnection connection = factory.getConnection();
                try {
                    if (connection instanceof RedisClusterConnection) {
                        RedisClusterConnection clusterConnection = (RedisClusterConnection) connection;
                        
                        // 1. 키의 슬롯 계산 (Lettuce SlotHash 사용)
                        int slot = SlotHash.getSlot(key);
                        
                        // 2. 슬롯을 담당하는 노드 찾기
                        Iterable<RedisClusterNode> nodes = clusterConnection.clusterGetNodes();
                        boolean nodeFound = false;
                        
                        for (RedisClusterNode node : nodes) {
                            // 해당 노드가 마스터이고, 슬롯 범위를 포함하는지 확인
                            if (node.isMaster() && node.getSlotRange().contains(slot)) {
                                logger.info("Key [{}] (Slot {}) -> Target Node: {}:{}", key, slot, node.getHost(), node.getPort());
                                nodeFound = true;
                                break;
                            }
                        }
                        
                        if (!nodeFound) {
                            logger.warn("Key [{}] (Slot {}) -> 담당 노드를 찾을 수 없습니다. (토폴로지 갱신 필요 가능성)", key, slot);
                        }
                    } else {
                        logger.info("클러스터 모드가 아닙니다. 단일 노드에 저장됩니다.");
                    }
                } finally {
                    connection.close();
                }
            }
        } catch (Exception e) {
            logger.warn("노드 정보 확인 중 오류: {}", e.getMessage());
        }
    }
}

