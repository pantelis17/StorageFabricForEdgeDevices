package com.thesis.sqlite.controllers;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.thesis.sqlite.entities.MetricEntity;
import com.thesis.sqlite.enumerations.EntityTypeE;
import com.thesis.sqlite.service.MetricService;

import lombok.AllArgsConstructor;

@RestController
@AllArgsConstructor
@RequestMapping("/metrics")
public class MetricController {

    private final MetricService metricService;

    @PutMapping("/add")
    public void addPod(@RequestBody MetricEntity metric) {
        metricService.addMetric(metric);
    }

    @GetMapping("/get")
    public List<MetricEntity> getMetrics(@RequestParam(name = "type", required = false) EntityTypeE type,
            @RequestParam(name = "node", required = false) String node) {
        return metricService.getMetrics(type, Optional.ofNullable(node));
    }

    @GetMapping("/getByEntityType")
    public Map<String, List<MetricEntity>> getMetricsByEntityType(EntityTypeE type) {
        return metricService.getAllMetricsByEntityType(type);
    }

    @DeleteMapping("/delete")
    public void deleteMetricById(@RequestParam(name = "id", required = true) Long id) {
        metricService.deleteMetric(id);
    }

}
