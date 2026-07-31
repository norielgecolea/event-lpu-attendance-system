package org.nors.dev.codes.lpu.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    /**
     * Comma-separated origin patterns. Override with APP_CORS_ALLOWED_ORIGIN_PATTERNS.
     * Patterns support Spring's {@code *} wildcards (e.g. {@code https://*.example.com}).
     */
    private String allowedOriginPatterns =
            String.join(
                    ",",
                    "http://localhost:*",
                    "http://127.0.0.1:*",
                    "https://localhost:*",
                    "https://127.0.0.1:*",
                    "https://eventattendance.lpulaguna.com",
                    "http://eventattendance.lpulaguna.com",
                    "https://*.lpulaguna.com",
                    "http://*.lpulaguna.com",
                    "https://eventattendance.lpu-laguna.edu.ph",
                    "http://eventattendance.lpu-laguna.edu.ph",
                    "https://*.lpu-laguna.edu.ph",
                    "http://*.lpu-laguna.edu.ph"
            );

    public String getAllowedOriginPatterns() {
        return allowedOriginPatterns;
    }

    public void setAllowedOriginPatterns(String allowedOriginPatterns) {
        this.allowedOriginPatterns = allowedOriginPatterns;
    }

    public List<String> resolvedPatterns() {
        if (allowedOriginPatterns == null || allowedOriginPatterns.isBlank()) {
            return List.of();
        }
        List<String> patterns = new ArrayList<>();
        Arrays.stream(allowedOriginPatterns.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(patterns::add);
        return patterns;
    }
}
