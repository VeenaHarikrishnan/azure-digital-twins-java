/**
 * Copyright (c) Microsoft Corporation. Licensed under the MIT License.
 */
package com.microsoft.twins.reflector;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import org.slf4j.MDC;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.Message;
import com.microsoft.twins.reflector.model.FeedbackMessage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ListenToIngressSampler {
  private final Set<TestMessage> receivedDeviceMessages =
      Collections.synchronizedSet(new HashSet<>());

  private final Set<FeedbackMessage> receivedFeedbackMessages =
      Collections.synchronizedSet(new HashSet<>());

  private final AmqpDeserializer amqpDeserializer = new AmqpDeserializer();

  @StreamListener(TestDeviceMessageSink.INPUT)
  void getEvent(final Message<String> event) {
    final String hardwareId = amqpDeserializer
        .deserializeString((byte[]) event.getHeaders().get("DigitalTwins-SensorHardwareId"));
    final String deviceId = amqpDeserializer
        .deserializeString((byte[]) event.getHeaders().get("iothub-connection-device-id"));
    final String source = amqpDeserializer
        .deserializeString((byte[]) event.getHeaders().get("iothub-message-source"));
    final String adtIsTelemetry = amqpDeserializer
        .deserializeString((byte[]) event.getHeaders().get("DigitalTwins-Telemetry"));


    log.info(
        "Got testevent {} for sensor {} through device {} by source {} and marked by ADT as telemetry: {}",
        event.getPayload(), hardwareId, deviceId, source, adtIsTelemetry);

    receivedDeviceMessages
        .add(new TestMessage(hardwareId, event.getPayload(), UUID.fromString(deviceId)));
  }

  @StreamListener(TestFeedbackSink.FEEDBACK)
  void getIngressFeedback(final FeedbackMessage feedback) {
    MDC.put("correlationId", feedback.getCorrelationId().toString());
    try {
      log.info("Got ingress feedback [{}]", feedback);
      receivedFeedbackMessages.add(feedback);
    } finally {
      MDC.clear();
    }
  }

  public boolean receivedDeviceMessagesContainsAll(final Collection<TestMessage> c) {
    synchronized (receivedDeviceMessages) {
      return receivedDeviceMessages.containsAll(c);
    }
  }

  public boolean anyMatchOnReceivedFeedbackMessages(final Predicate<FeedbackMessage> predicate) {
    synchronized (receivedFeedbackMessages) {
      return receivedFeedbackMessages.stream().anyMatch(predicate);
    }
  }

  public void clearReceivedDeviceMessages() {
    synchronized (receivedDeviceMessages) {
      receivedDeviceMessages.clear();
    }

  }

  public void clearReceivedFeedbackMessages() {
    synchronized (receivedFeedbackMessages) {
      receivedFeedbackMessages.clear();
    }
  }



}
