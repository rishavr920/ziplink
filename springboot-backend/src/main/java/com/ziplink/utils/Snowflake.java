package com.ziplink.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Snowflake ID Generator.
 * 
 * Replicates Twitter's Snowflake algorithm to generate 64-bit, time-sortable unique IDs
 * in distributed environments without central coordination.
 * 
 * Bit Allocation:
 * - 1 sign bit (always 0)
 * - 41 bits: Time in milliseconds since a custom epoch
 * - 10 bits: Worker ID (supports up to 1024 servers/containers)
 * - 12 bits: Sequence counter (up to 4096 unique IDs per millisecond per worker)
 */
@Component
public class Snowflake {

    private static final Logger log = LoggerFactory.getLogger(Snowflake.class);

    // Custom epoch: 2021-01-01 00:00:00 UTC
    private static final long EPOCH = 1609459200000L;

    private static final long WORKER_ID_BITS = 10L;
    private static final long SEQUENCE_BITS = 12L;

    private static final long MAX_WORKER_ID = -1L ^ (-1L << WORKER_ID_BITS); // 1023
    private static final long MAX_SEQUENCE = -1L ^ (-1L << SEQUENCE_BITS); // 4095

    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS; // 12
    private static final long TIMESTAMP_SHIFT = WORKER_ID_BITS + SEQUENCE_BITS; // 22

    @Value("${ziplink.worker-id:0}")
    private long workerId;

    private long sequence = 0L;
    private long lastTimestamp = -1L;

    @PostConstruct
    public void init() {
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException(String.format("Worker ID must be between 0 and %d", MAX_WORKER_ID));
        }
        log.info("Snowflake ID Generator initialized with workerId: {}, max workerId: {}, max sequence: {}", 
                workerId, MAX_WORKER_ID, MAX_SEQUENCE);
    }

    /**
     * Generates a unique 64-bit ID in a thread-safe manner.
     */
    public synchronized long nextId() {
        long timestamp = getCurrentTimestamp();

        if (timestamp < lastTimestamp) {
            long offset = lastTimestamp - timestamp;
            log.error("NTP Clock moved backwards by {} ms. Refusing to generate ID.", offset);
            throw new RuntimeException(String.format("Clock moved backwards. Refusing to generate ID for %d milliseconds", offset));
        }

        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                timestamp = waitNextMillisecond(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT) |
               (workerId << WORKER_ID_SHIFT) |
               sequence;
    }

    private long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }

    private long waitNextMillisecond(long lastTimestamp) {
        long timestamp = getCurrentTimestamp();
        while (timestamp <= lastTimestamp) {
            timestamp = getCurrentTimestamp();
        }
        return timestamp;
    }
}
