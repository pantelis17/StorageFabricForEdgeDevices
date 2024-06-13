package com.thesis.sqlite.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.thesis.sqlite.dht.DhtService;
import com.thesis.sqlite.entities.MetricEntity;
import com.thesis.sqlite.enumerations.EntityTypeE;
import com.thesis.sqlite.repositories.MetricRepository;
import com.thesis.sqlite.utils.Utils;

import io.micrometer.common.lang.NonNull;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class MetricService {
    private static final RestTemplate REST_TEMPLATE = new RestTemplate();
    private final MetricRepository metricRepository;
    private final PodService podService;
    private final ContainerService containerService;
    private final LifecheckService lifecheckService;
    private DhtService dhtService;

    @EventListener(ContextRefreshedEvent.class)
    public void init(ContextRefreshedEvent context) {
        dhtService = context.getApplicationContext().getBean("dhtService", DhtService.class);
    }

    public MetricEntity addMetric(@NonNull MetricEntity metric) {
        if (metric.getPod() != null) {
            metric.setPod(podService.getOrCreatePod(metric.getPod()));
        }
        if (metric.getContainer() != null) {
            metric.setContainer(containerService.getOrCreateContainer(metric.getContainer()));
        }
        final var savedMetric = metricRepository.save(metric);
        dhtService.putDataToNode(savedMetric.getEntityType());
        return savedMetric;
    }

    public void deleteMetric(@NonNull Long id) {
        Utils.LOGGER.info("Deleting metric with id '{}'", id);
        final var metric = metricRepository.findById(id);
        if (metric.isEmpty()) {
            Utils.LOGGER.warn("Could not find metric with id '{}'", id);
            return;
        }
        final var entityType = metric.get().getEntityType();
        metricRepository.deleteById(id);
        final var count = metricRepository.countByEntityType(entityType);
        if (count == 0) {
            dhtService.deleteTypeFromNode(entityType);
        }
    }

    @SuppressWarnings("unchecked")
    public List<MetricEntity> getMetrics(EntityTypeE type, Optional<String> node) {
        if (node.isPresent() && !node.get().equals(Utils.HOSTNAME)) {
            // If node is not present in the existingNodes list then return empty list
            if (!dhtService.getExistingNodes().contains(node.get())) {
                Utils.LOGGER.error("Could not find node '{}' in the existing nodes", node.get());
                return Collections.emptyList();
            }
            if (type != null) {
                // If based on the current information, the target node does not contain metrics
                // of the requested entityType then return empty list to avoid request
                final var targetNodes = dhtService.get(type);
                if (!targetNodes.contains(node.get())) {
                    return Collections.emptyList();
                }
            }
            // In different case send the request to fetch the data
            try {
                final var typeRequest = type != null ? String.format("?type=%s", type) : "";
                return REST_TEMPLATE.getForObject(
                        String.format("http://%s:29000/metrics/get%s", node.get(), typeRequest),
                        List.class);
            } catch (Exception e) {
                Utils.LOGGER.error("Exception during request to node '{}'", node.get(), e);
                return Collections.emptyList();
            }
        }
        if (type != null) {
            return metricRepository.findAllByEntityType(type);
        } else {
            return metricRepository.findAll();
        }
    }

    public Map<String, List<MetricEntity>> getAllMetricsByEntityType(EntityTypeE type) {
        final var data = new HashMap<String, List<MetricEntity>>();
        final var targetNodes = dhtService.get(type);
        if (targetNodes != null && !targetNodes.isEmpty()) {
            final var threadsMap = new HashMap<CompletableFuture<Boolean>, String>();
            // List to store nodes that didn't complete within the timeout
            final var timedOutNodes = new HashSet<String>();
            // List to store nodes that threw exceptions
            final var exceptionNodes = new HashSet<String>();
            // List of CompletableFuture
            List<CompletableFuture<Boolean>> futures = targetNodes.stream()
                    .map(node -> {
                        final var nodeName = node == null ? "localhost" : node;
                        final var future = CompletableFuture.supplyAsync(() -> {
                            Thread.currentThread().setName(nodeName);
                            try {
                                final List<MetricEntity> resp = REST_TEMPLATE.getForObject(
                                        String.format("http://%s:29000/metrics/get?type=%s", nodeName, type),
                                        List.class);
                                // There is a chance that some data have been deleted from a node and this node
                                // is not updated yet. As a result, we will get an empty list. To avoid this,
                                // add a guard here
                                if (resp != null && !resp.isEmpty()) {
                                    data.put(nodeName, resp);
                                }
                                return true;
                            } catch (Exception e) {
                                // Handle exception
                                Utils.LOGGER.warn("Exception during request to node '{}'", nodeName, e);
                                exceptionNodes.add(nodeName);
                                return false;
                            }
                        });
                        threadsMap.put(future, nodeName);
                        return future;
                    })
                    .peek(future -> future.orTimeout(4, TimeUnit.SECONDS) // Add timeout to CompletableFuture
                            .thenAccept(result -> {
                                Utils.LOGGER.info("Result is '{}'", result);
                                if (result == null) {
                                    // The future timed out
                                    final var nodeName = threadsMap.get(future);
                                    timedOutNodes.add(nodeName);
                                    Utils.LOGGER.warn("Timeout on node '{}'. Ignoring it's data", nodeName);
                                }
                            }))
                    .map(future -> future.exceptionally(ex -> {
                        // Handle exception
                        final var nodeName = threadsMap.get(future);
                        exceptionNodes.add(nodeName);
                        Utils.LOGGER.warn("Exception during waiting for node '{}'", nodeName, ex);
                        return null; // Handle the exception and return null to continue execution
                    }))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            // Wait for all CompletableFutures to complete
            final CompletableFuture<Void> allFutures = CompletableFuture
                    .allOf(futures.toArray(new CompletableFuture[0]));

            // Join all the futures
            try {
                Utils.LOGGER.info("Waiting for response");
                allFutures.join();
            } catch (CompletionException e) {
                // Handle completion exception if any
            }

            // Handle timed out nodes
            // Ignore their handling for now
            // timedOutNodes.forEach(node -> {});

            // Handle nodes with exceptions
            if (!exceptionNodes.isEmpty()) {
                Utils.LOGGER.info("Handling exception nodes '{}'", exceptionNodes);
                CompletableFuture.runAsync(() -> lifecheckService.lifeCheck(exceptionNodes));
            }
        }
        Utils.LOGGER.info("Sending data to user");
        return data;
    }
}
