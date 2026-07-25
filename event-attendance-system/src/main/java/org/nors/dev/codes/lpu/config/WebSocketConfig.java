package org.nors.dev.codes.lpu.config;

import org.nors.dev.codes.lpu.websocket.AuthWebSocketHandler;
import org.nors.dev.codes.lpu.websocket.EventKioskHandshakeInterceptor;
import org.nors.dev.codes.lpu.websocket.EventKioskWebSocketHandler;
import org.nors.dev.codes.lpu.websocket.JwtHandshakeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final AuthWebSocketHandler authWebSocketHandler;
    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;
    private final EventKioskWebSocketHandler eventKioskWebSocketHandler;
    private final EventKioskHandshakeInterceptor eventKioskHandshakeInterceptor;

    public WebSocketConfig(
            AuthWebSocketHandler authWebSocketHandler,
            JwtHandshakeInterceptor jwtHandshakeInterceptor,
            EventKioskWebSocketHandler eventKioskWebSocketHandler,
            EventKioskHandshakeInterceptor eventKioskHandshakeInterceptor
    ) {
        this.authWebSocketHandler = authWebSocketHandler;
        this.jwtHandshakeInterceptor = jwtHandshakeInterceptor;
        this.eventKioskWebSocketHandler = eventKioskWebSocketHandler;
        this.eventKioskHandshakeInterceptor = eventKioskHandshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(authWebSocketHandler, "/ws/notifications")
                .addInterceptors(jwtHandshakeInterceptor)
                .setAllowedOriginPatterns("*");

        registry.addHandler(eventKioskWebSocketHandler, "/ws/event-kiosk")
                .addInterceptors(eventKioskHandshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
