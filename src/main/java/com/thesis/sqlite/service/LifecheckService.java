package com.thesis.sqlite.service;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.thesis.sqlite.dht.DhtService;
import com.thesis.sqlite.utils.Utils;

import lombok.NoArgsConstructor;

@Service
@NoArgsConstructor
public class LifecheckService {

    private DhtService dhtService;

    @EventListener(ContextRefreshedEvent.class)
    public void init(ContextRefreshedEvent context) {
        dhtService = context.getApplicationContext().getBean("dhtService", DhtService.class);
    }

    public void lifeCheck(Set<String> nodes) {
        final var downNodes = new HashSet<String>();
        final var restTemplate = new RestTemplate();
        for (final var node : nodes) {
            try {
                restTemplate.getForObject(
                        String.format("http://%s:29000/lifecheck", node),
                        Boolean.class);
            } catch (Exception ex) {
                // If the request fails then it means that the node is down
                downNodes.add(node);
            }
        }
        // In case we have down nodes, we need to remove them from the DHTs
        // In case a node is just facing temporary issue e.g. network issue
        // Then the node will be re-inserted to the DHTs again during sync period
        if (!downNodes.isEmpty()) {
            Utils.LOGGER.info("Sending request to remove nodes '{}'", downNodes);
            dhtService.sendDownNodesRequest(downNodes);
        }
    }

}
