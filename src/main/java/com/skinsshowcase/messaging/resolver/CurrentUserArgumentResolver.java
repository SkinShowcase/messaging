package com.skinsshowcase.messaging.resolver;

import org.springframework.core.MethodParameter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Разрешает параметр, аннотированный {@link CurrentUser}, в SteamID64 из атрибута запроса.
 */
@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    public static final String STEAM_ID_ATTRIBUTE = "steamId";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterAnnotation(CurrentUser.class) != null
                && parameter.getParameterType().equals(String.class);
    }

    @Override
    public Object resolveArgument(@NonNull MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  @NonNull NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        return webRequest.getAttribute(STEAM_ID_ATTRIBUTE, 0);
    }
}
