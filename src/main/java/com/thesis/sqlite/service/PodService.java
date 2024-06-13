package com.thesis.sqlite.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.thesis.sqlite.entities.ContainerEntity;
import com.thesis.sqlite.entities.PodEntity;
import com.thesis.sqlite.repositories.PodRepository;

import io.micrometer.common.lang.NonNull;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class PodService {

    private final PodRepository podRepository;

    public PodEntity addPod(@NonNull PodEntity pod) {
        return podRepository.save(pod);
    }

    public PodEntity getOrCreatePod(@NonNull PodEntity pod) {
        return getPodByName(pod.getName()).orElseGet(() -> addPod(pod));
    }

    public Optional<PodEntity> getPodByName(@NonNull String podName) {
        return podRepository.findByName(podName);
    }

    public List<PodEntity> getAllPods() {
        return podRepository.findAll();
    }
}
