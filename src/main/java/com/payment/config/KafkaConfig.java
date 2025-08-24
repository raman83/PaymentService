package com.payment.config;

import java.util.HashMap;
import java.util.Map;

import com.rtr.dto.Pacs002Response;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;

import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

@EnableKafka
@Configuration
public class KafkaConfig {

    private final ProducerFactory<String, Object> producerFactory;

    public KafkaConfig(ProducerFactory<String, Object> producerFactory) {
        this.producerFactory = producerFactory;
    }

    // ---------- Producer side ----------
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory);
    }

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrap;

    // =====================================================================
    // RTR listener: Pacs002Response  (bean name kept as "kafkaListenerContainerFactory")
    // Your existing @KafkaListener on rtr.payment.status references this name.
    // =====================================================================
    @Bean
    public ConsumerFactory<String, Pacs002Response> rtrConsumerFactory() {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        // We provide the JsonDeserializer instance directly (no spring.json.* properties to avoid conflicts)
        JsonDeserializer<Pacs002Response> valueDeserializer =
                new JsonDeserializer<Pacs002Response>(Pacs002Response.class);
        valueDeserializer.addTrustedPackages("*");
        valueDeserializer.setUseTypeMapperForKey(false);
        valueDeserializer.ignoreTypeHeaders(); // don't require type headers from producer
        return new DefaultKafkaConsumerFactory<String, Pacs002Response>(
                props,
                new StringDeserializer(),
                valueDeserializer
        );
    }

    @Bean // IMPORTANT: name must match your listener's containerFactory reference
    public ConcurrentKafkaListenerContainerFactory<String, Pacs002Response> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Pacs002Response> factory =
                new ConcurrentKafkaListenerContainerFactory<String, Pacs002Response>();
        factory.setConsumerFactory(rtrConsumerFactory());
        return factory;
    }

    // =====================================================================
    // Bill status listener: String -> String (we parse JSON manually in the listener)
    // =====================================================================
    @Bean
    public ConsumerFactory<String, String> stringConsumerFactory() {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new DefaultKafkaConsumerFactory<String, String>(props);
    }

    @Bean(name = "stringKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, String> stringKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<String, String>();
        factory.setConsumerFactory(stringConsumerFactory());
        return factory;
    }
}
