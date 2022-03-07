package com.example.integration.flow;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ErrorFlow {

    KafkaTemplate<String, String> kafkaTemplate;

    @Bean
    IntegrationFlow errorHandler() {
        return IntegrationFlows.from("importErrorChannel")
                .log("error")
                .handle(m -> m.getPayload(), e -> e.id("shouldBeReached"))
                .get();
    }


}