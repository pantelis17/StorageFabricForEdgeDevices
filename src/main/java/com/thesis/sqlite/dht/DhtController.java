package com.thesis.sqlite.dht;

import java.util.Set;

import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/dht")
@AllArgsConstructor
public class DhtController {

    private DhtService dhtService;

    @PutMapping("/init")
    public void initializeNode(@RequestBody BaseRequest newNode) {
        dhtService.addNode(newNode);
    }

    @PutMapping("/sync")
    public void syncNode(@RequestBody SyncRequest request) {
        dhtService.receiveData(request);
    }

    @PutMapping("/down")
    public void downNodes(@RequestBody Set<String> downNodes) {
        dhtService.downNodesAction(downNodes);
    }
}
