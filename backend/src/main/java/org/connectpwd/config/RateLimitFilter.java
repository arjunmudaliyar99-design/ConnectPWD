package org.connectpwd.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;
    private final Environment environment;

    private static final int GENERAL_LIMIT = 100;
    private static final int AUTH_LIMIT = 10;

    /**
     * Skip rate limiting entirely for /api/v1/report/ paths when running in the
     * dev profile — avoids hard dependency on Redis during local development.
     */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        boolean isDev = Arrays.asList(environment.getActiveProfiles()).contains("dev");
        return isDev && request.getRequestURI().startsWith("/api/v1/report/");
    }

    @Override
    @SuppressWarnings("null")
    protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String clientIp = getClientIp(request);
        String path = request.getRequestURI();

        boolean isAuthPath = path.startsWith("/api/v1/auth/");
        int limit = isAuthPath ? AUTH_LIMIT : GENERAL_LIMIT;
        String key = "rate:" + (isAuthPath ? "auth:" : "gen:") + clientIp;

        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, Duration.ofMinutes(1));
            }

            if (count != null && count > limit) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write(
                        "{\"success\":false,\"error\":\"Rate limit exceeded. Max " + limit
                                + " requests per minute.\",\"timestamp\":\"" + java.time.Instant.now() + "\"}");
                return;
            }
        } catch (Exception e) {
            // Redis unavailable — skip rate limiting and allow request through
            log.warn("Rate limiting unavailable (Redis down): {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
