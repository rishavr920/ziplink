package com.ziplink.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class SnowflakeTest {

    private Snowflake snowflake;

    @BeforeEach
    void setUp() {
        snowflake = new Snowflake();
        // Inject worker ID manually into private field using Spring reflection tools
        ReflectionTestUtils.setField(snowflake, "workerId", 1L);
        snowflake.init();
    }

    @Test
    @DisplayName("Generated Snowflake ID must be positive")
    void positiveIds() {
        long id = snowflake.nextId();
        assertTrue(id > 0, "Snowflake ID must be positive");
    }

    @Test
    @DisplayName("Generated Snowflake IDs must be strictly increasing")
    void strictlyIncreasing() {
        long id1 = snowflake.nextId();
        long id2 = snowflake.nextId();
        long id3 = snowflake.nextId();
        
        assertTrue(id2 > id1, "Successive IDs must be strictly increasing");
        assertTrue(id3 > id2, "Successive IDs must be strictly increasing");
    }

    @Test
    @DisplayName("Concurrent multithreaded generation must produce 100% unique IDs with zero duplicates")
    void multithreadedUniqueness() throws InterruptedException {
        int threadsCount = 50;
        int idsPerThread = 100;
        int totalIdsCount = threadsCount * idsPerThread;

        ExecutorService executor = Executors.newFixedThreadPool(threadsCount);
        CountDownLatch latch = new CountDownLatch(1);
        
        // Use a thread-safe ConcurrentHashMap set to hold results
        Set<Long> generatedIds = Collections.newSetFromMap(new ConcurrentHashMap<>());

        for (int i = 0; i < threadsCount; i++) {
            executor.submit(() -> {
                try {
                    // Block until latch is released to ensure all threads launch simultaneously
                    latch.await();
                    for (int j = 0; j < idsPerThread; j++) {
                        generatedIds.add(snowflake.nextId());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        // Release latch to start all threads concurrently
        latch.countDown();
        
        executor.shutdown();
        boolean finished = executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);
        
        assertTrue(finished, "Executor did not finish in time");
        assertEquals(totalIdsCount, generatedIds.size(), 
                "Duplicates detected! Total generated IDs does not match set size");
    }
}
