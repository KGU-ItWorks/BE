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

    // ── 인코딩 큐 ──────────────────────────────────────
    public static final String VIDEO_ENCODING_QUEUE = "video.encoding.queue";
    public static final String VIDEO_ENCODING_EXCHANGE = "video.encoding.exchange";
    public static final String VIDEO_ENCODING_ROUTING_KEY = "video.encoding";

    // ── 광고 누끼 처리 큐 ────────────────────────────────
    public static final String AD_NUKI_QUEUE = "ad.nuki.queue";
    public static final String AD_NUKI_EXCHANGE = "ad.nuki.exchange";
    public static final String AD_NUKI_ROUTING_KEY = "ad.nuki";

    // ── AI 영상 fetch 큐 ─────────────────────────────────
    public static final String VIDEO_FETCH_QUEUE = "video.fetch.queue";
    public static final String VIDEO_FETCH_EXCHANGE = "video.fetch.exchange";
    public static final String VIDEO_FETCH_ROUTING_KEY = "video.fetch";

    /**
     * 인코딩 작업 큐
     */
    @Bean
    public Queue videoEncodingQueue() {
        return QueueBuilder.durable(VIDEO_ENCODING_QUEUE).build();
    }

    @Bean
    public DirectExchange videoEncodingExchange() {
        return new DirectExchange(VIDEO_ENCODING_EXCHANGE);
    }

    @Bean
    public Binding videoEncodingBinding(Queue videoEncodingQueue, DirectExchange videoEncodingExchange) {
        return BindingBuilder.bind(videoEncodingQueue).to(videoEncodingExchange).with(VIDEO_ENCODING_ROUTING_KEY);
    }

    /**
     * 광고 누끼 처리 큐
     */
    @Bean
    public Queue adNukiQueue() {
        return QueueBuilder.durable(AD_NUKI_QUEUE).build();
    }

    @Bean
    public DirectExchange adNukiExchange() {
        return new DirectExchange(AD_NUKI_EXCHANGE);
    }

    @Bean
    public Binding adNukiBinding(Queue adNukiQueue, DirectExchange adNukiExchange) {
        return BindingBuilder.bind(adNukiQueue).to(adNukiExchange).with(AD_NUKI_ROUTING_KEY);
    }

    /**
     * AI 영상 fetch 큐
     */
    @Bean
    public Queue videoFetchQueue() {
        return QueueBuilder.durable(VIDEO_FETCH_QUEUE).build();
    }

    @Bean
    public DirectExchange videoFetchExchange() {
        return new DirectExchange(VIDEO_FETCH_EXCHANGE);
    }

    @Bean
    public Binding videoFetchBinding(Queue videoFetchQueue, DirectExchange videoFetchExchange) {
        return BindingBuilder.bind(videoFetchQueue).to(videoFetchExchange).with(VIDEO_FETCH_ROUTING_KEY);
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
