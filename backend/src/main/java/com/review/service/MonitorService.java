package com.review.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.review.config.RabbitConfig;
import com.review.dto.CacheMonitorResponse;
import com.review.dto.QueueMonitorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * 监控相关服务：队列与缓存状态。
 */
@Service
@RequiredArgsConstructor
public class MonitorService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisConnectionFactory redisConnectionFactory;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${monitor.rabbitmq.api-url:http://localhost:15672/api}")
    private String rabbitApiUrl;

    @Value("${monitor.rabbitmq.username:${spring.rabbitmq.username}}")
    private String rabbitApiUsername;

    @Value("${monitor.rabbitmq.password:${spring.rabbitmq.password}}")
    private String rabbitApiPassword;

    @Value("${spring.rabbitmq.virtual-host:/}")
    private String rabbitVirtualHost;

    public QueueMonitorResponse loadQueueMetrics() {
        QueueMonitorResponse response = new QueueMonitorResponse();
        response.setMainQueue(fetchQueueInfo(RabbitConfig.FILE_COPY_QUEUE));
        response.setDlxQueue(fetchQueueInfo(RabbitConfig.FILE_COPY_DLX_QUEUE));
        response.setGeneratedAt(LocalDateTime.now());
        response.setAlertLevel(resolveAlertLevel(response));
        response.setAlertMessage(buildAlertMessage(response));
        return response;
    }

    public CacheMonitorResponse loadCacheMetrics() {
        CacheMonitorResponse response = new CacheMonitorResponse();
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            Properties info = connection.info();
            long hits = parseLong(info.getProperty("keyspace_hits"));
            long misses = parseLong(info.getProperty("keyspace_misses"));
            long total = hits + misses;
            response.setHits(hits);
            response.setMisses(misses);
            response.setHitRate(total == 0 ? 1.0 : ((double) hits / total));
            response.setConnectedClients(parseLong(info.getProperty("connected_clients")));
            response.setUsedMemoryHuman(info.getProperty("used_memory_human", "0B"));
            response.setUsedMemoryBytes(parseLong(info.getProperty("used_memory")));
            response.setTotalKeys(connection.dbSize());
        }
        response.setHotKeys(sampleHotKeys());
        response.setGeneratedAt(LocalDateTime.now());
        return response;
    }

    private QueueMonitorResponse.QueueInfo fetchQueueInfo(String queueName) {
        QueueMonitorResponse.QueueInfo info = new QueueMonitorResponse.QueueInfo();
        info.setQueueName(queueName);
        try {
            String url = rabbitApiUrl + "/queues/" + encode(rabbitVirtualHost) + "/" + encode(queueName);
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.setBasicAuth(rabbitApiUsername, rabbitApiPassword, StandardCharsets.UTF_8);
            ResponseEntity<String> entity = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET,
                    new HttpEntity<>(headers), String.class);
            JsonNode node = objectMapper.readTree(entity.getBody());
            info.setReady(node.path("messages_ready").asLong());
            info.setUnacked(node.path("messages_unacknowledged").asLong());
            info.setTotal(node.path("messages").asLong());
            info.setConsumers(node.path("consumers").asLong());
            info.setPublishRate(node.path("message_stats").path("publish_details").path("rate").asDouble(0.0));
            info.setDeliverRate(node.path("message_stats").path("deliver_details").path("rate").asDouble(0.0));
            info.setStatusLabel(info.getReady() > 0 ? "有待处理" : "空闲");
        } catch (Exception ex) {
            info.setStatusLabel("连接失败");
        }
        return info;
    }

    private String resolveAlertLevel(QueueMonitorResponse response) {
        long backlog = 0;
        if (response.getMainQueue() != null) {
            backlog += response.getMainQueue().getReady();
            backlog += response.getMainQueue().getUnacked();
        }
        if (backlog > 500) {
            return "danger";
        }
        if (backlog > 100) {
            return "warning";
        }
        return "normal";
    }

    private String buildAlertMessage(QueueMonitorResponse response) {
        String level = response.getAlertLevel();
        if ("danger".equals(level)) {
            return "文件复制队列堆积严重，请立即扩容或检查消费者";
        }
        if ("warning".equals(level)) {
            return "队列存在堆积，请关注消费速率";
        }
        return "队列运行稳定";
    }

    private List<CacheMonitorResponse.CacheKeySnapshot> sampleHotKeys() {
        if (redisTemplate == null) {
            return Collections.emptyList();
        }
        List<CacheMonitorResponse.CacheKeySnapshot> snapshots = new ArrayList<>();
        for (String pattern : Arrays.asList("stat:*", "options:reviewers:*")) {
            Set<String> keys = redisTemplate.keys(pattern);
            if (CollectionUtils.isEmpty(keys)) {
                continue;
            }
            for (String key : keys) {
                CacheMonitorResponse.CacheKeySnapshot snapshot = new CacheMonitorResponse.CacheKeySnapshot();
                snapshot.setKey(key);
                snapshot.setTtlSeconds(redisTemplate.getExpire(key));
                snapshots.add(snapshot);
                if (snapshots.size() >= 8) {
                    return snapshots;
                }
            }
        }
        return snapshots;
    }

    private long parseLong(String value) {
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private String encode(String value) {
        try {
            return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException ex) {
            throw new IllegalStateException("UTF-8 编码不可用", ex);
        }
    }
}
