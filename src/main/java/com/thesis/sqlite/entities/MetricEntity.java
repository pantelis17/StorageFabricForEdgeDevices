package com.thesis.sqlite.entities;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.thesis.sqlite.enumerations.EntityTypeE;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor
@Entity
@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MetricEntity {

    private enum Unit {
        PERCENTAGE, PLAIN;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(nullable = false)
    private EntityTypeE entityType;
    private Unit unit = Unit.PLAIN;
    private Double minVal;
    private Double maxVal;
    private boolean higherIsBetter;
    @Column(nullable = false)
    private Double val;
    @Column(nullable = false)
    private Long timestamp;
    @ManyToOne
    @JoinColumn(name = "pod_id")
    private PodEntity pod;
    @ManyToOne
    @JoinColumn(name = "container_id")
    private ContainerEntity container;
}
