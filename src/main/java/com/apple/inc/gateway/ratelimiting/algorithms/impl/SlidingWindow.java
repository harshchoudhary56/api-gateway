package com.apple.inc.gateway.ratelimiting.algorithms.impl;

import com.apple.inc.gateway.constants.enums.StrategyBeanNames;
import com.apple.inc.gateway.ratelimiting.algorithms.RateLimitStrategy;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

@Component(value = StrategyBeanNames.SLIDING_WINDOW)
public class SlidingWindow implements RateLimitStrategy {

    private final long maxRequests;
    private final long windowSizeInMillis;
    private final Clock clock;
    private final Map<String, Deque<Long>> map = new HashMap<>();

    public SlidingWindow(long maxRequests, long windowSizeInMillis, Clock clock) {
        this.maxRequests = maxRequests;
        this.windowSizeInMillis = windowSizeInMillis;
        this.clock = clock;
    }

    @Override
    public synchronized boolean handleRequest(String userId) {
        long currentTime = clock.millis();
        Deque<Long> window = map.computeIfAbsent(userId, c -> new LinkedList<>());

        while (!window.isEmpty() && currentTime - window.peekFirst() >= windowSizeInMillis) {
            window.pollFirst();
        }

        if (window.size() < maxRequests) {
            window.offerLast(currentTime);
            return true;
        } else {
            return false; // Rate limit exceeded
        }
    }
}
