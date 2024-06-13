package com.thesis.sqlite.kafka;

import java.util.Set;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thesis.sqlite.dht.BaseRequest;
import com.thesis.sqlite.dht.DhtService;
import com.thesis.sqlite.utils.Utils;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class KafkaConsumer {

    private final DhtService dhtService;
    private final ObjectMapper mapper;

    @KafkaListener(topics = "init-topic", groupId = "group_id")
    public void consumeInitNodeMessage(ConsumerRecord<String, String> message) {
        final var key = message.key();

        // Check if the producer ID matches the ID of the producer service
        if (key != null && !Utils.HOSTNAME.equals(key)) {
            try {
                // Process the message
                final var newNodeRequest = message.value();
                Utils.LOGGER.info("Message received: {}", newNodeRequest);
                final var object = mapper.readValue(newNodeRequest, BaseRequest.class);
                dhtService.addNode(object);
            } catch (JsonProcessingException e) {
                Utils.LOGGER.error(e.getMessage(), e);
            }
        } else {
            Utils.LOGGER.warn("Skipping message produced by the producer service");
        }
    }

    @KafkaListener(id = "downNodes", topics = "downnodes-topic", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeDownNodesMessage(String message) {
        try {
            Utils.LOGGER.info("Message received: {}", message);
            final var object = mapper.readValue(message, new TypeReference<Set<String>>() {
            });
            dhtService.downNodesAction(object);
        } catch (JsonProcessingException e) {
            Utils.LOGGER.error(e.getMessage(), e);
        }
    }
}