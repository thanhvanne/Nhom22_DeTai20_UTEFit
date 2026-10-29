package com.utefit.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Value("${utefit.websocket.allowed-origins:http://localhost:8080}")
    private String allowedOrigins;
    @org.springframework.beans.factory.annotation.Autowired private com.utefit.security.JwtService jwt;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .addInterceptors(new org.springframework.web.socket.server.HandshakeInterceptor() {
                    public boolean beforeHandshake(org.springframework.http.server.ServerHttpRequest request, org.springframework.http.server.ServerHttpResponse response, org.springframework.web.socket.WebSocketHandler handler, java.util.Map<String,Object> attributes) {
                        if (!(request instanceof org.springframework.http.server.ServletServerHttpRequest servlet)) return false;
                        var cookies = servlet.getServletRequest().getCookies();
                        if (cookies != null) for (var cookie : cookies) if ("UTEFIT_AUTH".equals(cookie.getName())) {
                            try {
                                var claims = jwt.parseVerified(cookie.getValue());
                                attributes.put("authId", Long.parseLong(claims.getSubject()));
                                attributes.put("authVersion", claims.get("ver", Integer.class));
                                attributes.put("authExpiry", claims.getExpiration().getTime());
                                return true;
                            } catch (RuntimeException ex) { return false; }
                        }
                        return false;
                    }
                    public void afterHandshake(org.springframework.http.server.ServerHttpRequest request, org.springframework.http.server.ServerHttpResponse response, org.springframework.web.socket.WebSocketHandler handler, Exception exception) {}
                })
                .setAllowedOriginPatterns(allowedOrigins.split(","))   // dev: cho phép mọi origin, nếu cần sau này siết lại
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Broker đơn giản cho /topic, /queue, /user
        registry.enableSimpleBroker("/topic", "/queue", "/user");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }
}
