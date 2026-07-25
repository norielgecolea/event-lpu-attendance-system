package org.nors.dev.codes.lpu.config;

import java.util.List;
import org.nors.dev.codes.lpu.security.JwtAuthEntryPoint;
import org.nors.dev.codes.lpu.security.JwtAuthFilter;
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
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final JwtAuthEntryPoint jwtAuthEntryPoint;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, JwtAuthEntryPoint jwtAuthEntryPoint) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.jwtAuthEntryPoint = jwtAuthEntryPoint;
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
                        // Students & employees come from the gate DB — read-only here.
                        .requestMatchers(HttpMethod.GET, "/api/students/**")
                        .hasAnyRole("SUPERADMIN", "ADMIN")
                        .requestMatchers("/api/students/**").denyAll()
                        .requestMatchers(HttpMethod.GET, "/api/employees/**")
                        .hasAnyRole("SUPERADMIN", "ADMIN")
                        .requestMatchers("/api/employees/**").denyAll()
                        .requestMatchers("/api/users/**").hasAnyRole("SUPERADMIN", "ADMIN")
                        .requestMatchers("/api/event-tones/**")
                        .hasAnyRole("SUPERADMIN", "ADMIN", "OSAS")
                        // Permanent event deletion: superadmin only
                        .requestMatchers(HttpMethod.DELETE, "/api/events/**").hasRole("SUPERADMIN")
                        .requestMatchers("/api/events/**").hasAnyRole("SUPERADMIN", "ADMIN", "OSAS")
                        .requestMatchers("/api/dashboard/**")
                        .hasAnyRole("SUPERADMIN", "ADMIN", "OSAS", "SCANNER")
                        .requestMatchers("/api/event-attendance/**")
                        .hasAnyRole("SUPERADMIN", "ADMIN", "OSAS", "SCANNER")
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
        configuration.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "https://rfidattendance.lpulaguna.com",
                "https://*.lpulaguna.com"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
