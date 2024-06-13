package com.thesis.sqlite.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.thesis.sqlite.entities.ContainerEntity;
import com.thesis.sqlite.repositories.ContainerRepository;

import io.micrometer.common.lang.NonNull;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ContainerService {

    private final ContainerRepository containerRepository;

    public ContainerEntity addContainer(@NonNull ContainerEntity container) {
        return containerRepository.save(container);
    }

    public ContainerEntity getOrCreateContainer(@NonNull ContainerEntity container) {
        return getContainerByName(container.getName()).orElseGet(() -> addContainer(container));
    }

    public Optional<ContainerEntity> getContainerByName(@NonNull String containerName) {
        return containerRepository.findByName(containerName);
    }

    public List<ContainerEntity> getAllContainers() {
        return containerRepository.findAll();
    }
}
