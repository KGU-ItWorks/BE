package com.streamly.streamly.global.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class RabbitMQConfig {

    // Queue 이름
    public static final String VIDEO_ENCODING_QUEUE = "video.encoding.queue";
    
    // Exchange 이름
    public static final String VIDEO_ENCODING_EXCHANGE = "video.encoding.exchange";
    
    // Routing Key
    public static final String VIDEO_ENCODING_ROUTING_KEY = "video.encoding";

    /**
     * 인코딩 작업 큐
     */
    @Bean
    public Queue videoEncodingQueue() {
        return QueueBuilder.durable(VIDEO_ENCODING_QUEUE)
                .build();
    }

    /**
     * Direct Exchange
     */
    @Bean
    public DirectExchange videoEncodingExchange() {
        return new DirectExchange(VIDEO_ENCODING_EXCHANGE);
    }

    /**
     * Queue와 Exchange 바인딩
     */
    @Bean
    public Binding videoEncodingBinding(Queue videoEncodingQueue, DirectExchange videoEncodingExchange) {
        return BindingBuilder
                .bind(videoEncodingQueue)
                .to(videoEncodingExchange)
                .with(VIDEO_ENCODING_ROUTING_KEY);
    }

    /**
     * JSON 메시지 컨버터
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate 설정
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }
}
