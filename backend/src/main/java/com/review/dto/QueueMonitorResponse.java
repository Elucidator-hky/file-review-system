package com.review.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 队列监控响应。
 */
@Data
public class QueueMonitorResponse {

    private QueueInfo mainQueue;
    private QueueInfo dlxQueue;
    private String alertLevel;
    private String alertMessage;
    private LocalDateTime generatedAt;

    @Data
    public static class QueueInfo {
        private String queueName;
        private long ready;
        private long unacked;
        private long total;
        private long consumers;
        private double publishRate;
        private double deliverRate;
        private String statusLabel;
    }
}
