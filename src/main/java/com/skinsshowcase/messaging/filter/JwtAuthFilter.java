package com.skinsshowcase.messaging.filter;

import com.skinsshowcase.messaging.resolver.CurrentUserArgumentResolver;
import com.skinsshowcase.messaging.service.JwtSupportService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Извлекает Bearer JWT из заголовка Authorization, валидирует и кладёт steamId в атрибут запроса.
 * При отсутствии или невалидном токене возвращает 401.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ADMIN_API_PREFIX = "/api/admin/";

    private final JwtSupportService jwtSupportService;

    public JwtAuthFilter(JwtSupportService jwtSupportService) {
        this.jwtSupportService = jwtSupportService;
    }

    /**
     * Админ-эндпоинты защищены {@code X-Admin-Api-Key} на контроллере, без JWT.
     */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        var uri = request.getRequestURI();
        if (uri == null) {
            return false;
        }
        var ctx = request.getContextPath();
        if (ctx != null && !ctx.isEmpty() && uri.startsWith(ctx)) {
            uri = uri.substring(ctx.length());
        }
        return uri.startsWith(ADMIN_API_PREFIX);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                   @NonNull HttpServletResponse response,
                                   @NonNull FilterChain filterChain) throws ServletException, IOException {
        var authHeader = request.getHeader(AUTHORIZATION);
        var token = extractBearerToken(authHeader);

        if (token == null || !jwtSupportService.isValid(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Unauthorized\",\"detail\":\"Missing or invalid token\"}");
            return;
        }

        var steamId = jwtSupportService.parseSubject(token);
        request.setAttribute(CurrentUserArgumentResolver.STEAM_ID_ATTRIBUTE, steamId);
        filterChain.doFilter(request, response);
    }

    private static String extractBearerToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }
        var token = authHeader.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }
}
