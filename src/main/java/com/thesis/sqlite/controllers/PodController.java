package com.thesis.sqlite.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.thesis.sqlite.entities.PodEntity;
import com.thesis.sqlite.service.PodService;

import lombok.AllArgsConstructor;

@RestController
@AllArgsConstructor
@RequestMapping("/pods")
public class PodController {

    private final PodService podService;

    @PostMapping("/add")
    public void addPod(@RequestBody PodEntity pod) {
        podService.addPod(pod);
    }

    @GetMapping("/get")
    public List<PodEntity> getPods() {
        return podService.getAllPods();
    }
}
