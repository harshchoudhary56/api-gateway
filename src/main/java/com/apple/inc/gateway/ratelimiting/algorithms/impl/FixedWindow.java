package com.apple.inc.gateway.ratelimiting.algorithms.impl;

import com.apple.inc.gateway.constants.enums.StrategyBeanNames;
import com.apple.inc.gateway.ratelimiting.algorithms.RateLimitStrategy;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

@Component(value = StrategyBeanNames.FIXED_WINDOW)
public class FixedWindow implements RateLimitStrategy {

        private final long maxRequests;
        private final long windowSizeInMillis;
        private final Clock clock;
        private final Map<String, Window> userFixedWindow = new HashMap<>();

    private record Window(long startTime, long requestCount) {}

        public FixedWindow(long maxRequests, long windowSizeInMillis, Clock clock) {
            this.maxRequests = maxRequests;
            this.windowSizeInMillis = windowSizeInMillis;
            this.clock = clock;
        }

        @Override
        public synchronized boolean handleRequest(String userId) {
            long currentTime = clock.millis();
            Window window = userFixedWindow.get(userId);

            if (window == null || currentTime - window.startTime >= windowSizeInMillis) {
                // Start a new window
                userFixedWindow.put(userId, new Window(currentTime, 1));
                return true;
            } else {
                if (window.requestCount < maxRequests) {
                    userFixedWindow.put(userId, new Window(currentTime, window.requestCount + 1));
                    return true;
                } else {
                    return false; // Rate limit exceeded
                }
            }
        }
}


