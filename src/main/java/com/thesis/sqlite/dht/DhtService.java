package com.thesis.sqlite.dht;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.thesis.sqlite.enumerations.EntityTypeE;
import com.thesis.sqlite.kafka.KafkaProducer;
import com.thesis.sqlite.repositories.MetricRepository;
import com.thesis.sqlite.utils.Utils;

import lombok.Getter;
import lombok.NonNull;

@Service
public class DhtService {
    private final Map<EntityTypeE, Set<String>> dataMap;
    private final Map<EntityTypeE, HashMap<String, LocalDateTime>> deletedTypesMap;
    private final Set<EntityTypeE> deletedTypes;
    private final RestTemplate restTemplate;
    private @Getter final Set<String> existingNodes;
    private final KafkaProducer kafkaProducer;
    private final Object deleteLock;
    private final Object syncLock;

    @Autowired
    private DhtService(MetricRepository metricRepository, KafkaProducer kafkaProducer) {
        this.kafkaProducer = kafkaProducer;
        restTemplate = new RestTemplate();
        existingNodes = ConcurrentHashMap.newKeySet();
        deleteLock = new Object();
        syncLock = new Object();
        dataMap = new ConcurrentHashMap<>();
        deletedTypesMap = new ConcurrentHashMap<>();
        deletedTypes = ConcurrentHashMap.newKeySet();
        existingNodes.add(Utils.HOSTNAME);
        existingNodes.add(Utils.DISCOVERY_NODE_NAME);
        final var existingEntityTypes = metricRepository.findDistinctEntityTypes();
        if (!existingEntityTypes.isEmpty()) {
            Utils.LOGGER.info("Detected the following entity types for node '{}': {}", Utils.HOSTNAME,
                    existingEntityTypes);
        }
        for (final var entityType : existingEntityTypes) {
            put(entityType, Collections.singleton(Utils.HOSTNAME), false);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeDHT() {
        if (Utils.HOSTNAME != null) {
            // Perform DHT initialization or send request to the server node
            Utils.LOGGER.info("Application is ready for node '{}''. Sending kafka message for node warm up",
                    Utils.HOSTNAME);

            final var newNodeRequest = BaseRequest.builder().currentNodes(existingNodes).dataMap(dataMap)
                    .nodeName(Utils.HOSTNAME).build();
            kafkaProducer.sendMessage(newNodeRequest);
        }
    }

    // Method to put data into the node's data map
    private void put(@NonNull EntityTypeE key, @NonNull Set<String> value, boolean fromSyncAction) {
        Utils.LOGGER.info("Adding '{}' key with value '{}'", key, String.join(",", value));
        // Make sure that we do not modify the dataMap entity while deletion of an
        // element takes place
        Utils.LOGGER.info("Try delete lock in put");
        synchronized (deleteLock) {
            Utils.LOGGER.info("Achieved delete lock in put");
            final var finalValues = new HashSet<>(value);
            if (fromSyncAction) {
                // If there are delete nodes in the map make sure that we wont re-insert them if
                // they came from sync
                if (deletedTypesMap.containsKey(key)) {
                    final var deleteValues = deletedTypesMap.get(key).keySet();
                    Utils.LOGGER.info(
                            "Skipping add of nodes '{}' to type as they were delete withing the last 10 minutes. In case those types should exist in this node then they will be updated in a later sync");

                    finalValues.removeAll(deleteValues);
                }
            } else {
                // If the request is not from sync action, then we need to update the deleteMap
                // in order to avoid sending wrong details during the sync
                if (deletedTypes.contains(key)) {
                    // If we run this part of code, then if means that we insert an entityType to
                    // this node which was deleted in a previous step
                    deletedTypes.remove(key);
                    if (deletedTypesMap.containsKey(key)) {
                        deletedTypesMap.get(key).remove(Utils.HOSTNAME);
                    }
                }
            }
            if (dataMap.containsKey(key)) {
                dataMap.get(key).addAll(finalValues);
            } else {
                dataMap.put(key, finalValues);
            }
        }
        Utils.LOGGER.info("Unlock delete lock in put");
    }

    // Method to get data from the node's data map
    public Set<String> get(EntityTypeE key) {
        if (dataMap.containsKey(key)) {
            return new HashSet<>(dataMap.get(key));
        }
        return Collections.emptySet();
    }

    public void putDataToNode(EntityTypeE key) {
        // TODO: Maybe we should change this as this might creates performance issues
        // during syncing
        Utils.LOGGER.info("Try sync lock in put");
        synchronized (syncLock) {
            Utils.LOGGER.info("Achieved sync lock in put");
            Utils.LOGGER.info("Adding key '{}' for node '{}'", key, Utils.HOSTNAME);
            put(key, Collections.singleton(Utils.HOSTNAME), false);
        }
        Utils.LOGGER.info("Released sync lock in put");
    }

    public void deleteTypeFromNode(EntityTypeE key) {
        Utils.LOGGER.info("Try delete lock in delete");
        synchronized (deleteLock) {
            Utils.LOGGER.info("Achieved delete lock in delete");
            Utils.LOGGER.info("Removing key '{}' for node '{}'", key, Utils.HOSTNAME);
            dataMap.get(key).remove(Utils.HOSTNAME);
            if (dataMap.get(key).isEmpty()) {
                dataMap.remove(key);
            }
            deletedTypes.add(key);
            deletedTypesMap.computeIfAbsent(key, type -> new HashMap<String, LocalDateTime>()).put(Utils.HOSTNAME,
                    LocalDateTime.now());
        }
        Utils.LOGGER.info("Released delete lock in delete");
    }

    // Method to add node
    public void addNode(BaseRequest newNode) {
        Utils.LOGGER.info("Adding node '{}' to known nodes", newNode);
        final var syncRequest = buildSyncRequest();
        Utils.LOGGER.info("Try sync lock in add");
        synchronized (syncLock) {
            Utils.LOGGER.info("Achieved sync lock in add");
            existingNodes.add(newNode.getNodeName());
            receiveData(newNode);
        }
        Utils.LOGGER.info("Released sync lock in add");
        Utils.LOGGER.info("Send request: {}", syncRequest);
        syncData(newNode.getNodeName(), syncRequest);
        Utils.LOGGER.info("Node '{}' shared it's existence", newNode);
    }

    // Method to sync data from one node to another
    private void syncData(@NonNull String destination, SyncRequest syncRequest) {
        if (destination.equals(Utils.HOSTNAME)) {
            // Do not try to sync with itself
            return;
        }
        CompletableFuture
                .runAsync(() -> restTemplate.put(String.format("http://%s:29000/dht/sync", destination), syncRequest));
    }

    // Execute this job every 5 minutes
    @Scheduled(fixedRate = 5, initialDelay = 5, timeUnit = TimeUnit.MINUTES)
    private void syncScheduledJob() {
        final var startTime = System.nanoTime();
        Utils.LOGGER.info("Now syncing node '{}'", Utils.HOSTNAME);
        final var syncRequest = buildSyncRequest();
        Utils.LOGGER.info("Try delete lock in sync");
        synchronized (deleteLock) {
            Utils.LOGGER.info("Achieved delete lock in sync");
            // Once we have send the info to the other nodes, we need to ensure that we
            // clear this set to avoid erogenous deletions
            deletedTypes.clear();
        }
        Utils.LOGGER.info("Released delete lock in sync");
        Utils.LOGGER.info("Send request: {}", syncRequest);
        for (final var target : syncRequest.getCurrentNodes()) {
            try {
                syncData(target, syncRequest);
            } catch (Exception ex) {
                Utils.LOGGER.error("Could not send request to node '{}'. Adding node to downNodes", target, ex);
            }
        }
        final var endTime = System.nanoTime();
        // Calculate the elapsed time
        final var duration = endTime - startTime; // in nanoseconds
        // Convert to milliseconds if needed
        final var milliseconds = duration / 1_000_000.0;
        // Print the execution time
        Utils.LOGGER.info("Execution time in milliseconds: {}", milliseconds);
    }

    // Execute this job every 1 minutes
    @Scheduled(fixedRate = 1, initialDelay = 6, timeUnit = TimeUnit.MINUTES)
    private void removeNodesFromDeletionMap() {
        Utils.LOGGER.info("Try delete lock in remove");
        synchronized (deleteLock) {
            Utils.LOGGER.info("Achieved delete lock in remove");
            final var now = LocalDateTime.now();
            for (final var entry : deletedTypesMap.entrySet()) {
                final var removeKeys = new ArrayList<String>();
                for (final var node : entry.getValue().entrySet()) {
                    if (node.getValue().plusMinutes(10L).isBefore(now)) {
                        removeKeys.add(node.getKey());
                        Utils.LOGGER.info("Removing '{}'-'{}' from deletion map", entry.getKey(), node.getKey());
                    }
                }
                entry.getValue().keySet().removeAll(removeKeys);
            }
        }
        Utils.LOGGER.info("Released delete lock in remove");
    }

    private SyncRequest buildSyncRequest() {
        // Make a copy to send request only to nodes which were already known
        // In case have new nodes, lets avoid the syncing in the handshake phase to
        // avoid potential errors
        final Set<String> currentExistingNode;
        final Set<EntityTypeE> currentDeletedTypes;
        final Map<EntityTypeE, Set<String>> data = new HashMap<>();
        Utils.LOGGER.info("Try delete lock in build");
        synchronized (deleteLock) {
            Utils.LOGGER.info("Achieved delete lock in build");
            currentDeletedTypes = new HashSet<>(deletedTypes);
        }
        Utils.LOGGER.info("Released delete lock in build");
        Utils.LOGGER.info("Try sync lock in build");
        synchronized (syncLock) {
            Utils.LOGGER.info("Achieved sync lock in build");
            currentExistingNode = new HashSet<>(existingNodes);
            // Potential memory leak if there too many elements. We need to ensure that GC
            // is used properly here
            for (final var entry : dataMap.entrySet()) {
                data.put(entry.getKey(), new HashSet<>(entry.getValue()));
            }

        }
        Utils.LOGGER.info("Released sync lock in build");
        return SyncRequest.builder().currentNodes(currentExistingNode).dataMap(data).nodeName(Utils.HOSTNAME)
                .deletedTypes(currentDeletedTypes).build();
    }

    // Currently supports only add of new data. Updates and deletions are not
    // supported yet
    public void receiveData(BaseRequest request) {
        Utils.LOGGER.info("Try sync lock in receive");
        synchronized (syncLock) {
            Utils.LOGGER.info("Achieved sync lock in receive");
            existingNodes.addAll(request.getCurrentNodes());
            for (final var entry : request.getDataMap().entrySet()) {
                put(entry.getKey(), entry.getValue(), true);
            }
            if (request instanceof SyncRequest syncRequest && !syncRequest.getDeletedTypes().isEmpty()) {
                Utils.LOGGER.info("Try delete lock in receive");
                synchronized (deleteLock) {
                    Utils.LOGGER.info("Achieved delete lock in receive");
                    final var currentTime = LocalDateTime.now();
                    syncRequest.getDeletedTypes()
                            .forEach(type -> {
                                Utils.LOGGER.info("Removing from node '{}' from type '{}'", syncRequest.getNodeName(),
                                        type);
                                if (dataMap.containsKey(type)) {
                                    dataMap.get(type).remove(syncRequest.getNodeName());
                                    if (dataMap.get(type).isEmpty()) {
                                        dataMap.remove(type);
                                    }
                                }
                                deletedTypesMap
                                        .computeIfAbsent(type, key -> new HashMap<String, LocalDateTime>()).put(
                                                syncRequest.getNodeName(),
                                                currentTime);
                            });
                }
                Utils.LOGGER.info("Released delete lock in receive");
            }
        }
        Utils.LOGGER.info("Released sync lock in receive");
    }

    // If one or more nodes are down, remove them from DHT to avoid delays
    public void downNodesAction(Set<String> downNodes) {
        Utils.LOGGER.info("Received request for down nodes {}", downNodes);
        Utils.LOGGER.info("Try sync lock in downNodes");
        synchronized (syncLock) {
            Utils.LOGGER.info("Achieved sync lock in downNodes");
            // If the current node is marked as down make sure that the local info wont be
            // lost
            if (downNodes.contains(Utils.HOSTNAME)) {
                downNodes.remove(Utils.HOSTNAME);
            }
            Utils.LOGGER.info("Removing nodes '{}' as they seems to be down", downNodes);
            existingNodes.removeAll(downNodes);
            for (final var values : dataMap.values()) {
                values.removeAll(downNodes);
            }
        }
        Utils.LOGGER.info("Released sync lock in downNodes");
    }

    // If one or more nodes are down, remove them from DHT to avoid delays
    public void sendDownNodesRequest(Set<String> downNodes) {
        // Send a kafka message which will be caught from every other instance in order
        // to avoid request to every node
        kafkaProducer.sendDownNodesMessage(downNodes);
    }
}
