package org.nors.dev.codes.lpu.config;

import java.util.List;
import org.nors.dev.codes.lpu.security.JwtAuthEntryPoint;
import org.nors.dev.codes.lpu.security.JwtAuthFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(CorsProperties.class)
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final JwtAuthEntryPoint jwtAuthEntryPoint;
    private final CorsProperties corsProperties;

    public SecurityConfig(
            JwtAuthFilter jwtAuthFilter,
            JwtAuthEntryPoint jwtAuthEntryPoint,
            CorsProperties corsProperties
    ) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.jwtAuthEntryPoint = jwtAuthEntryPoint;
        this.corsProperties = corsProperties;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/pictures/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/tones/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/events/today").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/events/*/public").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/event-attendance/public-tap").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/event-attendance/kiosk-status").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/event-tones").permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/api/auth/**").authenticated()
                        // Students from gate DB — read-only; Superadmin + OSAS
                        .requestMatchers(HttpMethod.GET, "/api/students/**")
                        .hasAnyRole("SUPERADMIN", "OSAS")
                        .requestMatchers("/api/students/**").denyAll()
                        // Employees — Superadmin only
                        .requestMatchers(HttpMethod.GET, "/api/employees/**")
                        .hasRole("SUPERADMIN")
                        .requestMatchers("/api/employees/**").denyAll()
                        .requestMatchers("/api/users/**").hasRole("SUPERADMIN")
                        .requestMatchers("/api/event-tones/**").hasRole("SUPERADMIN")
                        // Event mutations by role
                        .requestMatchers(HttpMethod.DELETE, "/api/events/**").hasRole("SUPERADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/events/**")
                        .hasAnyRole("SUPERADMIN", "OSAS")
                        .requestMatchers(HttpMethod.PATCH, "/api/events/**")
                        .hasAnyRole("SUPERADMIN", "OSAS")
                        .requestMatchers(HttpMethod.POST, "/api/events/**")
                        .hasAnyRole("SUPERADMIN", "OSAS", "EVENT_MAKER")
                        .requestMatchers("/api/events/**")
                        .hasAnyRole("SUPERADMIN", "OSAS", "EVENT_MAKER")
                        .requestMatchers("/api/dashboard/**")
                        .hasAnyRole("SUPERADMIN", "OSAS", "EVENT_MAKER")
                        .requestMatchers("/api/event-attendance/**")
                        .hasAnyRole("SUPERADMIN", "OSAS", "EVENT_MAKER")
                        .requestMatchers(HttpMethod.GET, "/api/tap-errors/**")
                        .hasAnyRole("SUPERADMIN", "OSAS")
                        .requestMatchers("/api/tap-errors/**")
                        .hasAnyRole("SUPERADMIN", "OSAS")
                        .requestMatchers("/api/**").hasRole("SUPERADMIN")
                        .anyRequest().permitAll()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Origin has no path — trailing /* would reject browser CORS and return 403
        List<String> patterns = corsProperties.resolvedPatterns();
        if (patterns.isEmpty()) {
            patterns = List.of("*");
        }
        configuration.setAllowedOriginPatterns(patterns);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
