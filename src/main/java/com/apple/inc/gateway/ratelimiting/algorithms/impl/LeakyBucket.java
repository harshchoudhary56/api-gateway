package com.apple.inc.gateway.ratelimiting.algorithms.impl;

import com.apple.inc.gateway.constants.enums.StrategyBeanNames;
import com.apple.inc.gateway.ratelimiting.algorithms.RateLimitStrategy;
import org.springframework.stereotype.Component;

@Component(value = StrategyBeanNames.LEAKY_BUCKET)
public class LeakyBucket implements RateLimitStrategy {


    @Override
    public boolean handleRequest(String userId) {
        return false;
    }
}
