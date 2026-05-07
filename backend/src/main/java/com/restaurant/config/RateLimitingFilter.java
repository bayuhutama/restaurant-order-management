package com.restaurant.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting filter for public endpoints to prevent abuse.
 * Limits each IP to 100 requests per minute on auth, menu, and order endpoints.
 *
 * The bucket map is cleared every 10 minutes to prevent unbounded memory growth
 * from unique IPs. A cleared bucket resets the counter for that IP, which is
 * acceptable — the window is short enough that legitimate users are unaffected.
 */
@Component
public class RateLimitingFilter implements Filter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private Bucket createNewBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(100)
                .refillGreedy(100, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    /** Evicts all buckets every 10 minutes to prevent the map growing unboundedly. */
    @Scheduled(fixedDelay = 600_000)
    public void evictBuckets() {
        buckets.clear();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
            
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String path = httpRequest.getRequestURI();
        
        // Only apply rate limiting to public endpoints that could be subject to abuse
        if (path.startsWith("/api/auth/") || path.startsWith("/api/menu") || path.startsWith("/api/orders/public")) {
            String ip = httpRequest.getRemoteAddr();
            Bucket bucket = buckets.computeIfAbsent(ip, k -> createNewBucket());

            if (bucket.tryConsume(1)) {
                chain.doFilter(request, response);
            } else {
                HttpServletResponse httpResponse = (HttpServletResponse) response;
                httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                httpResponse.setContentType("application/json");
                httpResponse.getWriter().write("{\"message\": \"Too many requests. Please try again later.\"}");
            }
        } else {
            // Unrestricted endpoints or authenticated endpoints
            chain.doFilter(request, response);
        }
    }
}
