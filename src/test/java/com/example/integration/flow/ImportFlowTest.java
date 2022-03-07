package com.example.integration.flow;

import com.example.integration.model.MyDataObject;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.Transformers;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.context.SpringIntegrationTest;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.messaging.MessageHandler;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@SpringIntegrationTest
@SpringJUnitConfig
@EmbeddedKafka(partitions = 1,
        topics={"myImport"})
@SpringBootTest(properties = {"spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ImportFlowTest {

    private final static String IMPORT_TOPIC = "myImport";

    @Autowired
    @Qualifier("importErrorChannel")
    DirectChannel importErrorChannel;

    @Autowired
    @Qualifier("shouldBeReached")
    AbstractEndpoint errorHandlerEndpoint;

    @Autowired
    EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    KafkaTemplate<String, String> template;

    Consumer<String, String> consumer;

    MessageHandler errorMock;

    @BeforeAll
    void setup() {
        errorMock  = FlowUtil.stopAndRegisterMock(errorHandlerEndpoint, importErrorChannel);
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("testGroup", "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        ConsumerFactory<String, String> cf = new DefaultKafkaConsumerFactory<>(consumerProps);
        this.consumer = cf.createConsumer();

        embeddedKafkaBroker.consumeFromEmbeddedTopics(this.consumer,
                true,
                IMPORT_TOPIC);
    }

    @AfterEach
    void tearDown() {
        Mockito.reset(errorMock);
    }

    // This exception will be sent to errorHandler
    @Test
    void thisTestBreaksInErrorFlow() {
        template.send(new ProducerRecord<>(IMPORT_TOPIC, 0, "myKey", "invalid payload", this.getDefaultHeaders()));
        verify(errorMock, timeout(10000).times(1)).handleMessage(any());
    }

    @Test
    void thisTestDoesNotBreak() {
        publishValidDataObject();
        verify(errorMock, timeout(10000).times(1)).handleMessage(any());
    }

    private Headers getDefaultHeaders() {
        Headers headers = new RecordHeaders();
        headers.add(ImportFlow.MY_CORRELATION_HEADER, "abc".getBytes(StandardCharsets.UTF_8));
        return headers;
    }

    private void publishValidDataObject() {
        template.send(
                new ProducerRecord<>(
                        IMPORT_TOPIC,
                        0,
                        "myKey",
                        Transformers.toJson().transform(MessageBuilder
                                .withPayload(MyDataObject.builder()
                                        .prop("a")
                                        .otherProp("b")
                                        .build()
                                ).build()
                        ).getPayload().toString(),
                        this.getDefaultHeaders()
                )
        );
    }

}
