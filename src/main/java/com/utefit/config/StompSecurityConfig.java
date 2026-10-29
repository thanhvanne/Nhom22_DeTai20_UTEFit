package com.utefit.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.messaging.simp.config.ChannelRegistration;
import java.security.Principal;

/** Chặn client gửi trực tiếp vào broker hoặc subscribe user khác. */
@Configuration
public class StompSecurityConfig implements WebSocketMessageBrokerConfigurer {
    @org.springframework.beans.factory.annotation.Autowired private com.utefit.repository.UserRepository users;
    private final java.util.concurrent.ConcurrentMap<String, java.util.Map<String,Object>> sessions = new java.util.concurrent.ConcurrentHashMap<>();
    private void validate(java.util.Map<String,Object> attrs) {
        if (attrs == null || !(attrs.get("authId") instanceof Long id) || !(attrs.get("authExpiry") instanceof Long expiry) || expiry <= System.currentTimeMillis())
            throw new org.springframework.security.access.AccessDeniedException("Phiên chat đã hết hạn.");
        var u = users.findById(id).orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Không có tài khoản."));
        if (u.getStatus() != com.utefit.enums.UserStatus.ACTIVE || !Boolean.TRUE.equals(u.getEnabled())
                || !java.util.Objects.equals(attrs.get("authVersion"), u.getTokenVersion() == null ? 0 : u.getTokenVersion()))
            throw new org.springframework.security.access.AccessDeniedException("Phiên chat đã được thu hồi.");
    }
    @Override public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override public Message<?> preSend(Message<?> message, MessageChannel channel) {
                var h = org.springframework.messaging.simp.SimpMessageHeaderAccessor.wrap(message);
                var attrs = sessions.get(h.getSessionId() == null ? "" : h.getSessionId());
                if (attrs != null) try { validate(attrs); } catch (org.springframework.security.access.AccessDeniedException ex) { return null; }
                return message;
            }
        });
    }
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor h = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (h == null) return message;
                StompCommand cmd = h.getCommand();
                if (cmd == null) return message;
                if (cmd == StompCommand.DISCONNECT && h.getSessionId() != null) sessions.remove(h.getSessionId());
                if (cmd == StompCommand.CONNECT || cmd == StompCommand.SEND
                        || cmd == StompCommand.SUBSCRIBE) {
                    validate(h.getSessionAttributes());
                    if (h.getSessionId() != null) sessions.put(h.getSessionId(), h.getSessionAttributes());
                    Principal user = h.getUser();
                    if (user == null) throw new org.springframework.security.access.AccessDeniedException(
                            "Cần đăng nhập để sử dụng WebSocket.");
                    String destination = h.getDestination();
                    if (cmd == StompCommand.SEND && !"/app/chat.send".equals(destination)) {
                        throw new org.springframework.security.access.AccessDeniedException("STOMP SEND không hợp lệ.");
                    }
                    if (cmd == StompCommand.SUBSCRIBE
                            && !("/user/queue/chat".equals(destination)
                            || "/user/queue/notifications".equals(destination))) {
                        throw new org.springframework.security.access.AccessDeniedException("Không thể subscribe destination này.");
                    }
                }
                return message;
            }
        });
    }
}
