package com.example.integration.flow;

import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;

import static org.mockito.Mockito.mock;

public class FlowUtil {

    public static MessageHandler stopAndRegisterMock(AbstractEndpoint endpoint, SubscribableChannel channel) {
        endpoint.stop();
        return registerMock(channel);
    }

    public static MessageHandler registerMock(SubscribableChannel channel) {

        MessageHandler mockHandler = mock(MessageHandler.class);
        channel.subscribe(mockHandler);
        return mockHandler;
    }
}
