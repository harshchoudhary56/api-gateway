package com.apple.inc.gateway.apirecord.filter;

import com.apple.inc.gateway.apirecord.entity.ApiRecordEntity;
import com.apple.inc.gateway.apirecord.repository.ApiRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

/**
 * Global Gateway filter that logs every request/response to MongoDB.
 *
 * <p>Execution order:
 * <ol>
 *   <li>Captures request metadata (method, URI, headers, client IP)</li>
 *   <li>Decorates the response to capture the response body + status code</li>
 *   <li>Persists the full audit record to MongoDB asynchronously</li>
 * </ol>
 *
 * <p>Ordered at {@code Ordered.HIGHEST_PRECEDENCE + 1} so it runs early
 * (after the gateway routing filter but before custom filters).</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiRecordGlobalFilter implements GlobalFilter, Ordered {

    private final ApiRecordRepository apiRecordRepository;

    @Value("${spring.profiles.active:unknown}")
    private String environment;

    @Override
    public int getOrder() {
        // Run early to capture everything, but after routing is determined
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();
        ServerHttpRequest request = exchange.getRequest();

        // Generate correlation ID
        String crid = request.getHeaders().getFirst("X-Correlation-Id");
        if (crid == null || crid.isBlank()) {
            crid = UUID.randomUUID().toString();
        }

        // Build the entity with request info
        ApiRecordEntity record = new ApiRecordEntity();
        record.setCrid(crid);
        record.setHttpMethod(request.getMethod().name());
        record.setRequestUri(request.getURI().getPath());
        record.setQueryParams(request.getURI().getQuery());
        record.setClientIp(extractClientIp(request));
        record.setEnvironment(environment);
        record.setCreatedAt(Instant.now());

        // Extract target service name from the route (if available)
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        if (route != null) {
            record.setTargetService(route.getId());
        }

        // Capture selected request headers (avoid logging Authorization tokens fully)
        record.setRequestHeaders(sanitizeHeaders(request));

        // Inject correlation ID into the downstream request
        String finalCrid = crid;
        ServerHttpRequest mutatedRequest = request.mutate()
                .header("X-Correlation-Id", finalCrid)
                .build();

        // Decorate response to capture body + status
        ServerHttpResponse originalResponse = exchange.getResponse();
        DataBufferFactory bufferFactory = originalResponse.bufferFactory();

        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                if (body instanceof Flux<? extends DataBuffer> fluxBody) {
                    return super.writeWith(
                            DataBufferUtils.join(fluxBody).map(dataBuffer -> {
                                // Read response body bytes
                                byte[] content = new byte[dataBuffer.readableByteCount()];
                                dataBuffer.read(content);
                                DataBufferUtils.release(dataBuffer);

                                String responseBody = new String(content, StandardCharsets.UTF_8);

                                // Complete the record
                                record.setResponseBody(truncate(responseBody, 5000));
                                record.setHttpStatusCode(
                                        originalResponse.getStatusCode() != null
                                                ? originalResponse.getStatusCode().value()
                                                : 0
                                );
                                record.setLatencyMs(System.currentTimeMillis() - startTime);
                                record.setUpdatedAt(Instant.now());

                                // Persist asynchronously — don't block the response
                                apiRecordRepository.save(record)
                                        .subscribeOn(Schedulers.boundedElastic())
                                        .doOnSuccess(saved -> log.debug("API record saved: crid={}", saved.getCrid()))
                                        .doOnError(e -> log.error("Failed to save API record: crid={}", finalCrid, e))
                                        .subscribe();

                                // Return the original response bytes untouched
                                return bufferFactory.wrap(content);
                            })
                    );
                }
                return super.writeWith(body);
            }
        };

        return chain.filter(
                exchange.mutate()
                        .request(mutatedRequest)
                        .response(decoratedResponse)
                        .build()
        );
    }

    /**
     * Extract client IP, checking X-Forwarded-For header first.
     */
    private String extractClientIp(ServerHttpRequest request) {
        String xff = request.getHeaders().getFirst("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddress() != null
                ? request.getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
    }

    /**
     * Log only safe headers (exclude Authorization, Cookie, etc.)
     */
    private String sanitizeHeaders(ServerHttpRequest request) {
        StringBuilder sb = new StringBuilder();
        request.getHeaders().forEach((name, values) -> {
            String lowerName = name.toLowerCase();
            if (!lowerName.equals("authorization") && !lowerName.equals("cookie")) {
                sb.append(name).append(": ").append(String.join(", ", values)).append("; ");
            }
        });
        return sb.toString();
    }

    /**
     * Truncate large bodies to avoid storing huge payloads in MongoDB.
     */
    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() > maxLength
                ? value.substring(0, maxLength) + "...[TRUNCATED]"
                : value;
    }
}

