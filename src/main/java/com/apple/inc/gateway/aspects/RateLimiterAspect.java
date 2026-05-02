package com.apple.inc.gateway.aspects;

import com.apple.inc.gateway.annotations.RateLimiter;
import com.apple.inc.gateway.ratelimiting.algorithms.RateLimitStrategy;
import com.apple.inc.gateway.constants.enums.StrategyType;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import java.util.Map;

@Aspect
@Component
@RequiredArgsConstructor
public class RateLimiterAspect {

    private final ApplicationContext context;

    private final Map<StrategyType, RateLimitStrategy> strategies = Map.of(
            StrategyType.TOKEN_BUCKET, context.getBean(StrategyType.TOKEN_BUCKET.getValue(), RateLimitStrategy.class),
            StrategyType.LEAKY_BUCKET, context.getBean(StrategyType.LEAKY_BUCKET.getValue(), RateLimitStrategy.class),
            StrategyType.FIXED_WINDOW, context.getBean(StrategyType.FIXED_WINDOW.getValue(), RateLimitStrategy.class),
            StrategyType.SLIDING_WINDOW, context.getBean(StrategyType.SLIDING_WINDOW.getValue(), RateLimitStrategy.class)
    );

    @Before("@annotation(rateLimiter) && args(.., exchange)")
    public void applyRateLimiting(JoinPoint joinPoint, RateLimiter rateLimiter, ServerWebExchange exchange) {
        String userId = getUserId(exchange);
        StrategyType strategyType = rateLimiter.strategy();
        RateLimitStrategy strategy = strategies.get(strategyType);
        if (strategy == null) {
            throw new RuntimeException("Rate limit strategy not found: " + strategyType);
        }
        boolean allowed = strategy.handleRequest(userId);
        if (!allowed) {
            throw new RuntimeException("Rate limit exceeded");
        }
    }

    private String getUserId(ServerWebExchange exchange) {
        String ip = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (ip == null) {
            ip = exchange.getRequest().getRemoteAddress() != null ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() : "unknown";
        }
        return ip;
    }
}