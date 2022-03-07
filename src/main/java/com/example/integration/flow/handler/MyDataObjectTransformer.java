package com.example.integration.flow.handler;

import com.example.integration.model.MyDataObject;
import lombok.RequiredArgsConstructor;
import org.springframework.integration.transformer.GenericTransformer;
import org.springframework.integration.transformer.MessageTransformationException;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MyDataObjectTransformer implements GenericTransformer<Message<List<MyDataObject>>, List<MyDataObject>> {

    @Override
    public List<MyDataObject> transform(Message<List<MyDataObject>> source) {
        throw new MessageTransformationException(source, "test");
    }
}
