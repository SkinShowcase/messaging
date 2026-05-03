package com.skinsshowcase.messaging.resolver;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class CurrentUserArgumentResolverTest {

    private final CurrentUserArgumentResolver resolver = new CurrentUserArgumentResolver();

    @Test
    void supportsParameter_currentUserStringOnly() throws Exception {
        var m = getClass().getDeclaredMethod("handler", String.class, String.class);
        assertThat(resolver.supportsParameter(MethodParameter.forExecutable(m, 0))).isTrue();
        assertThat(resolver.supportsParameter(MethodParameter.forExecutable(m, 1))).isFalse();
    }

    @Test
    void resolveArgument_readsRequestAttribute() throws Exception {
        var m = getClass().getDeclaredMethod("handler", String.class, String.class);
        var mp = MethodParameter.forExecutable(m, 0);
        var req = new MockHttpServletRequest();
        req.setAttribute(CurrentUserArgumentResolver.STEAM_ID_ATTRIBUTE, "76561198000000001");
        var out = resolver.resolveArgument(
                mp,
                new ModelAndViewContainer(),
                new ServletWebRequest(req),
                null);

        assertThat(out).isEqualTo("76561198000000001");
    }

    @SuppressWarnings("unused")
    private void handler(@CurrentUser String user, String other) {
    }
}
