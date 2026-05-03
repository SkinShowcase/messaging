package com.skinsshowcase.messaging.config;

import com.skinsshowcase.messaging.resolver.CurrentUserArgumentResolver;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class WebConfigTest {

    @Test
    void addsCurrentUserResolver() {
        CurrentUserArgumentResolver resolver = mock(CurrentUserArgumentResolver.class);
        var config = new WebConfig(resolver);
        List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>();
        config.addArgumentResolvers(resolvers);
        assertThat(resolvers).contains(resolver);
    }
}
