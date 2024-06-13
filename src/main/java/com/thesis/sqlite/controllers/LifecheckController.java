package com.thesis.sqlite.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.thesis.sqlite.utils.Utils;

import lombok.AllArgsConstructor;

@RestController
@AllArgsConstructor
@RequestMapping("/lifecheck")
public class LifecheckController {

    @GetMapping("")
    public boolean lifecheck() {
        Utils.LOGGER.warn("Received lifecheck request. Please check the state of the node.");
        return true;
    }
}
