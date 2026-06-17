package com.telco.backend.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String QUEUE_NAME = "telco.order.queue";
    public static final String EXCHANGE_NAME = "telco.order.exchange";
    public static final String ROUTING_KEY = "telco.order.routingKey";

    // 1. Kuyruğu Tanımlıyoruz
    @Bean
    public Queue orderQueue() {
        return new Queue(QUEUE_NAME, true); // durable = true
    }

    // 2. Exchange Tanımlıyoruz
    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    // 3. Kuyruğu Exchange'e Bağlıyoruz
    @Bean
    public Binding orderBinding(Queue orderQueue, DirectExchange orderExchange) {
        return BindingBuilder.bind(orderQueue).to(orderExchange).with(ROUTING_KEY);
    }
}