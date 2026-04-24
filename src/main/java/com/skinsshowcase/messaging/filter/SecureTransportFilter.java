package com.skinsshowcase.messaging.filter;

import com.skinsshowcase.messaging.config.TransportSecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class SecureTransportFilter extends OncePerRequestFilter {

    private final TransportSecurityProperties properties;

    public SecureTransportFilter(TransportSecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        var path = request.getRequestURI();
        return path == null || !path.startsWith("/api/chats");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!properties.isRequireSecureTransport()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (isSecureRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpStatus.UPGRADE_REQUIRED.value());
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Secure transport required (HTTPS)\"}");
    }

    private static boolean isSecureRequest(HttpServletRequest request) {
        if (request.isSecure()) {
            return true;
        }
        var proto = request.getHeader("X-Forwarded-Proto");
        return proto != null && "https".equalsIgnoreCase(proto.trim());
    }
}
