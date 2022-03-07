package com.example.integration.flow;

import com.example.integration.flow.handler.MyDataObjectTransformer;
import com.example.integration.model.MyDataObject;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.aggregator.MessageCountReleaseStrategy;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.dsl.StandardIntegrationFlow;
import org.springframework.integration.dsl.Transformers;
import org.springframework.integration.kafka.dsl.Kafka;
import org.springframework.integration.kafka.inbound.KafkaMessageDrivenChannelAdapter.ListenerMode;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.Executors;

@Configuration
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ImportFlow {

    static String MY_CORRELATION_HEADER = "Correlation-Header";

    @Qualifier("errorHandler")
    IntegrationFlow errorHandler;

    KafkaTemplate<String, String> kafkaTemplate;
    ConsumerFactory<String, String> kafkaConsumerFactory;

    MyDataObjectTransformer myDataObjectTransformer;

    @Bean
    public StandardIntegrationFlow startKafkaInbound() {
        return IntegrationFlows.from(Kafka
                .messageDrivenChannelAdapter(
                        kafkaConsumerFactory,
                        ListenerMode.record,
                        "myImport")
                .errorChannel(errorHandler.getInputChannel())
        )
                .channel(nextChannel().getInputChannel())
                .get();
    }

    @Bean
    public IntegrationFlow nextChannel() {
        return IntegrationFlows.from("next")
                .transform(Transformers.fromJson(MyDataObject.class))  // An exception here is sent to errorChannel
                .aggregate(aggregatorSpec ->
                        aggregatorSpec
                                .releaseStrategy(new MessageCountReleaseStrategy(100))
                                .sendPartialResultOnExpiry(true)
                                .groupTimeout(2000L)
                                .expireGroupsUponCompletion(true)
                                .correlationStrategy(message -> message.getHeaders().get(MY_CORRELATION_HEADER))
                )
                .transform(myDataObjectTransformer)  // Exception here is not sent to errorChannel
                .channel(acknowledgeMyObjectFlow().getInputChannel())
                .get();
    }

    @Bean
    public IntegrationFlow acknowledgeMyObjectFlow() {
        return IntegrationFlows.from("ack")
                .log("ack")
                .handle(m -> System.out.println("xxx"))
                .get();
    }
}
