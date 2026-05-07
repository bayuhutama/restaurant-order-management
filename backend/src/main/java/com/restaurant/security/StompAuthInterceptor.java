package com.restaurant.security;

import com.restaurant.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    io.jsonwebtoken.Claims claims = jwtUtil.parseToken(token);
                    String username = claims.getSubject();
                    
                    if (username != null) {
                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                        
                        boolean versionOk = true;
                        if (userDetails instanceof User user) {
                            Object v = claims.get(JwtUtil.CLAIM_TOKEN_VERSION);
                            Long claimVersion = (v instanceof Number n) ? n.longValue() : null;
                            versionOk = Objects.equals(claimVersion, user.getTokenVersion());
                        }

                        boolean valid = username.equals(userDetails.getUsername()) && !claims.getExpiration().before(new java.util.Date());
                        
                        if (versionOk && valid) {
                            UsernamePasswordAuthenticationToken auth = 
                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                            accessor.setUser(auth);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Invalid JWT token over STOMP: {}", e.getMessage());
                }
            }
        }
        return message;
    }
}
