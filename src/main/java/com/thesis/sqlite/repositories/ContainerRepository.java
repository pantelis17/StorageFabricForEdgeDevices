package com.thesis.sqlite.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import com.thesis.sqlite.entities.ContainerEntity;

public interface ContainerRepository extends CrudRepository<ContainerEntity, Long> {
    List<ContainerEntity> findAll();

    Optional<ContainerEntity> findByName(String name);
}
