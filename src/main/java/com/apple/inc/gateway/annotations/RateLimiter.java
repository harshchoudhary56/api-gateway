package com.apple.inc.gateway.annotations;

import com.apple.inc.gateway.constants.enums.StrategyType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimiter {

    StrategyType strategy() default StrategyType.TOKEN_BUCKET;
}
