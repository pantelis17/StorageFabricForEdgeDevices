package com.thesis.sqlite.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {
    public static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);
    public static final String DISCOVERY_NODE_NAME = System.getenv("DISCOVERY");
    public static final String HOSTNAME = System.getenv("HOSTNAME");
}
