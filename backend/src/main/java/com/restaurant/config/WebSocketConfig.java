package com.restaurant.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configures STOMP over WebSocket for real-time order updates.
 *
 * Topics used by the application:
 * - /topic/orders              — staff dashboard subscribes here; receives every order update
 * - /topic/orders/{orderNumber}— individual customers subscribe here; receives updates for their order
 *
 * SockJS fallback is enabled so browsers without native WebSocket support still work.
 * The frontend uses globalThis instead of global (Vite requires this for sockjs-client).
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Configures the in-memory message broker.
     * - /topic and /queue prefixes route messages to subscribed clients
     * - /app prefix routes messages to @MessageMapping controller methods (if any)
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
    }

    /**
     * Registers the /ws endpoint for STOMP connections with SockJS fallback.
     * Only the Vite dev server origin is allowed; update for production.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("http://localhost:5173")
                .withSockJS();
    }
}
