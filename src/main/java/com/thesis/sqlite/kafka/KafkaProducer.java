package com.thesis.sqlite.kafka;

import java.util.Set;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thesis.sqlite.dht.BaseRequest;
import com.thesis.sqlite.utils.Utils;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class KafkaProducer {

    private static final String INIT_TOPIC = "init-topic";
    private static final String DOWNNODES_TOPIC = "downnodes-topic";

    private KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper mapper;

    public void sendMessage(BaseRequest newNodeRequest) {
        try {
            final var request = mapper.writeValueAsString(newNodeRequest);
            kafkaTemplate.send(INIT_TOPIC, Utils.HOSTNAME, request);
            Utils.LOGGER.info("Message sent: {}", request);
        } catch (JsonProcessingException e) {
            Utils.LOGGER.error(e.getMessage(), e);
        }
    }

    public void sendDownNodesMessage(Set<String> downNodes) {
        try {
            final var request = mapper.writeValueAsString(downNodes);
            kafkaTemplate.send(DOWNNODES_TOPIC, request);
            Utils.LOGGER.info("Message sent: {}", request);
        } catch (JsonProcessingException e) {
            Utils.LOGGER.error(e.getMessage(), e);
        }
    }
}
