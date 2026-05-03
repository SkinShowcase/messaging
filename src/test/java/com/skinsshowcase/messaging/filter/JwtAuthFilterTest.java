package com.skinsshowcase.messaging.filter;

import com.skinsshowcase.messaging.resolver.CurrentUserArgumentResolver;
import com.skinsshowcase.messaging.service.JwtSupportService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    JwtSupportService jwtSupportService;

    @Mock
    FilterChain filterChain;

    JwtAuthFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthFilter(jwtSupportService);
    }

    @Test
    void adminPath_skipsJwt() throws Exception {
        var req = new MockHttpServletRequest();
        req.setRequestURI("/api/admin/messaging/support/incoming");
        var res = new MockHttpServletResponse();

        filter.doFilter(req, res, filterChain);

        verify(filterChain).doFilter(req, res);
    }

    @Test
    void missingToken_401() throws Exception {
        var req = new MockHttpServletRequest();
        req.setRequestURI("/api/chats");
        var res = new MockHttpServletResponse();

        filter.doFilter(req, res, filterChain);

        assertThat(res.getStatus()).isEqualTo(401);
    }

    @Test
    void validToken_setsAttribute() throws Exception {
        when(jwtSupportService.isValid("tok")).thenReturn(true);
        when(jwtSupportService.parseSubject("tok")).thenReturn("76561198000000001");

        var req = new MockHttpServletRequest();
        req.setRequestURI("/api/chats");
        req.addHeader("Authorization", "Bearer tok");
        var res = new MockHttpServletResponse();

        filter.doFilter(req, res, filterChain);

        assertThat(req.getAttribute(CurrentUserArgumentResolver.STEAM_ID_ATTRIBUTE))
                .isEqualTo("76561198000000001");
        verify(filterChain).doFilter(eq(req), eq(res));
    }
}
