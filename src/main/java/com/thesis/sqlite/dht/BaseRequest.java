package com.thesis.sqlite.dht;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.thesis.sqlite.enumerations.EntityTypeE;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@ToString
@Getter
@AllArgsConstructor
public class BaseRequest implements Serializable {
    private String nodeName;
    private Set<String> currentNodes;
    private Map<EntityTypeE, Set<String>> dataMap;
}
