package com.thesis.sqlite.repositories;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.thesis.sqlite.entities.MetricEntity;
import com.thesis.sqlite.enumerations.EntityTypeE;

public interface MetricRepository extends CrudRepository<MetricEntity, Long> {
    List<MetricEntity> findAll();

    List<MetricEntity> findAllByEntityType(EntityTypeE type);

    long countByEntityType(EntityTypeE type);

    @Query("SELECT DISTINCT m.entityType FROM MetricEntity m")
    Set<EntityTypeE> findDistinctEntityTypes();
}
