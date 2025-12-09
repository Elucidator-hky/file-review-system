package com.review.mq;

import com.review.config.RabbitConfig;
import com.review.service.FileAsyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 文件复制消费者，监听 file.copy.queue。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileCopyConsumer {

    private final FileAsyncService fileAsyncService;

    @RabbitListener(queues = RabbitConfig.FILE_COPY_QUEUE)
    public void handleMessage(FileCopyMessage message) {
        try {
            fileAsyncService.handleCopy(message);
        } catch (Exception ex) {
            log.error("copy file failed, traceId={}, version={}", message.getTraceId(), message.getNewVersionId(), ex);
            try {
                fileAsyncService.markCopyFailed(message.getNewVersionId());
            } catch (Exception inner) {
                log.error("mark copy failed error, version={}", message.getNewVersionId(), inner);
            }
            throw new AmqpRejectAndDontRequeueException("copy file failed", ex);
        }
    }
}
