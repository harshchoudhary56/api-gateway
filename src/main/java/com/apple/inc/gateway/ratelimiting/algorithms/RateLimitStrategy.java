package com.apple.inc.gateway.ratelimiting.algorithms;

public interface RateLimitStrategy {

    boolean handleRequest(String userId);
}
