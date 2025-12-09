package com.review.mq;

import com.review.common.BusinessException;
import com.review.config.RabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 文件复制消息生产者。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileCopyProducer {

    private final RabbitTemplate rabbitTemplate;

    public void send(FileCopyMessage message) {
        try {
            rabbitTemplate.convertAndSend(RabbitConfig.REVIEW_EXCHANGE,
                    RabbitConfig.FILE_COPY_ROUTING_KEY,
                    message);
            log.info("file-copy message sent, traceId={}, taskId={}, newVersion={}", message.getTraceId(),
                    message.getTaskId(), message.getNewVersionId());
        } catch (Exception e) {
            log.error("send file-copy message failed, traceId={}", message.getTraceId(), e);
            throw new BusinessException("触发文件复制失败，请稍后重试");
        }
    }
}
