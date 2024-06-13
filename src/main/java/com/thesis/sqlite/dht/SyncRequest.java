package com.thesis.sqlite.dht;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.thesis.sqlite.enumerations.EntityTypeE;

import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@ToString
@Getter
public class SyncRequest extends BaseRequest {
    private Set<EntityTypeE> deletedTypes;

    public SyncRequest(String sourceName, Set<EntityTypeE> deletedTypes, Set<String> currentNodes,
            Map<EntityTypeE, Set<String>> dataMap) {
        super(sourceName, currentNodes, dataMap);
        this.deletedTypes = deletedTypes;
    }

}
