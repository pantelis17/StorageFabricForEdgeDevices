package com.thesis.sqlite.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.thesis.sqlite.entities.ContainerEntity;
import com.thesis.sqlite.service.ContainerService;

import lombok.AllArgsConstructor;

@RestController
@AllArgsConstructor
@RequestMapping("/containers")
public class ContainerController {
    private final ContainerService containerService;

    @PostMapping("/add")
    public void addContainer(@RequestBody ContainerEntity container) {
        containerService.addContainer(container);
    }

    @GetMapping("/get")
    public List<ContainerEntity> getContainers() {
        return containerService.getAllContainers();
    }
}
