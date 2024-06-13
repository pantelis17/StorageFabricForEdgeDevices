package com.thesis.sqlite.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import com.thesis.sqlite.entities.PodEntity;

public interface PodRepository extends CrudRepository<PodEntity, Long> {
    List<PodEntity> findAll();

    Optional<PodEntity> findByName(String name);
}
