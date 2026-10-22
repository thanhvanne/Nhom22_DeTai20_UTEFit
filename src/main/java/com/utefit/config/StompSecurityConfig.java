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
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor h = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (h == null) return message;
                StompCommand cmd = h.getCommand();
                if (cmd == null) return message;
                if (cmd == StompCommand.CONNECT || cmd == StompCommand.SEND
                        || cmd == StompCommand.SUBSCRIBE) {
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
