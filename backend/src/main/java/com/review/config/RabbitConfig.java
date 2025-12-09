package com.review.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 基础配置，定义文件复制相关的交换机/队列/绑定。
 */
@Configuration
@EnableRabbit
public class RabbitConfig {

    public static final String REVIEW_EXCHANGE = "review.exchange";
    public static final String FILE_COPY_QUEUE = "file.copy.queue";
    public static final String FILE_COPY_DLX_QUEUE = "file.copy.dlx";
    public static final String FILE_COPY_ROUTING_KEY = "file.copy";
    public static final String FILE_COPY_DLX_ROUTING_KEY = "file.copy.dlx";

    @Bean
    public TopicExchange reviewExchange() {
        return new TopicExchange(REVIEW_EXCHANGE, true, false);
    }

    @Bean
    public Queue fileCopyQueue() {
        return QueueBuilder.durable(FILE_COPY_QUEUE)
                .withArgument("x-dead-letter-exchange", REVIEW_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", FILE_COPY_DLX_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue fileCopyDlxQueue() {
        return QueueBuilder.durable(FILE_COPY_DLX_QUEUE).build();
    }

    @Bean
    public Binding fileCopyBinding(Queue fileCopyQueue, TopicExchange reviewExchange) {
        return BindingBuilder.bind(fileCopyQueue)
                .to(reviewExchange)
                .with(FILE_COPY_ROUTING_KEY);
    }

    @Bean
    public Binding fileCopyDlxBinding(Queue fileCopyDlxQueue, TopicExchange reviewExchange) {
        return BindingBuilder.bind(fileCopyDlxQueue)
                .to(reviewExchange)
                .with(FILE_COPY_DLX_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
