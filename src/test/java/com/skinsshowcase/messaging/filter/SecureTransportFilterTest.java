package com.skinsshowcase.messaging.filter;

import com.skinsshowcase.messaging.config.TransportSecurityProperties;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SecureTransportFilterTest {

    @Mock
    FilterChain filterChain;

    @Test
    void nonChatPath_skipped() throws Exception {
        var props = new TransportSecurityProperties();
        props.setRequireSecureTransport(true);
        var filter = new SecureTransportFilter(props);
        var req = new MockHttpServletRequest();
        req.setRequestURI("/actuator/health");
        var res = new MockHttpServletResponse();

        filter.doFilter(req, res, filterChain);

        verify(filterChain).doFilter(req, res);
    }

    @Test
    void requireSecure_insecureRejected() throws Exception {
        var props = new TransportSecurityProperties();
        props.setRequireSecureTransport(true);
        var filter = new SecureTransportFilter(props);
        var req = new MockHttpServletRequest();
        req.setRequestURI("/api/chats");
        req.setSecure(false);
        var res = new MockHttpServletResponse();

        filter.doFilter(req, res, filterChain);

        assertThat(res.getStatus()).isEqualTo(426);
    }

    @Test
    void forwardedProtoHttps_allowed() throws Exception {
        var props = new TransportSecurityProperties();
        props.setRequireSecureTransport(true);
        var filter = new SecureTransportFilter(props);
        var req = new MockHttpServletRequest();
        req.setRequestURI("/api/chats");
        req.setSecure(false);
        req.addHeader("X-Forwarded-Proto", "https");
        var res = new MockHttpServletResponse();

        filter.doFilter(req, res, filterChain);

        verify(filterChain).doFilter(req, res);
    }
}
