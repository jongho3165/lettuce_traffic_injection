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

    @org.springframework.beans.factory.annotation.Value("${traffic.interval:1000}")
    private long interval;

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

                // 클러스터 노드 정보 로깅 (선택 사항)
                logTargetNode(redisTemplate, key);

                // 키 저장
                redisTemplate.opsForValue().set(key, value);
                logger.info("키 저장 성공: Key={}", key);

                // 키 조회
                String retrievedValue = redisTemplate.opsForValue().get(key);
                logger.info("키 조회 성공: Key={}, Value={}", key, retrievedValue);

                try {
                    TimeUnit.MILLISECONDS.sleep(interval);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }

            } catch (Exception e) {
                logger.error("Redis 작업 실패! Key={}", key);
                logger.error("에러 메시지: {}", e.getMessage());
                logger.error("Stack Trace:", e);
                
                try {
                    TimeUnit.MILLISECONDS.sleep(interval);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * 키가 저장될 슬롯을 계산하고, 해당 슬롯을 담당하는 노드(Master)를 찾아 로깅합니다.
     */
    private void logTargetNode(RedisTemplate<String, String> template, String key) {
        try {
            RedisConnectionFactory factory = template.getConnectionFactory();
            if (factory != null) {
                RedisConnection connection = factory.getConnection();
                try {
                    if (connection instanceof RedisClusterConnection) {
                        RedisClusterConnection clusterConnection = (RedisClusterConnection) connection;
                        
                        int slot = SlotHash.getSlot(key);
                        Iterable<RedisClusterNode> nodes = clusterConnection.clusterGetNodes();
                        boolean nodeFound = false;
                        
                        for (RedisClusterNode node : nodes) {
                            if (node.isMaster() && node.getSlotRange().contains(slot)) {
                                logger.info("Key [{}] (Slot {}) -> Target Node: {}:{}", key, slot, node.getHost(), node.getPort());
                                nodeFound = true;
                                break;
                            }
                        }
                        
                        if (!nodeFound) {
                            logger.warn("Key [{}] (Slot {}) -> 담당 노드를 찾을 수 없습니다.", key, slot);
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
