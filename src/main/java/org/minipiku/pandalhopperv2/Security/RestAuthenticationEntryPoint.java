package org.minipiku.pandalhopperv2.Security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * Returns a JSON 401 instead of the servlet container's HTML error page or a
 * redirect to a login form, which is what an API client expects.
 *
 * <p>{@link JwtAuthFilter} stashes the reason a token was rejected on the
 * request so the response can distinguish "no credentials supplied" from
 * "credentials supplied but invalid" without leaking parser internals.
 */
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    static final String ERROR_ATTRIBUTE = "jwt.auth.error";

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        Object reason = request.getAttribute(ERROR_ATTRIBUTE);
        String message = (reason instanceof String s) ? s : "Authentication required";

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), Map.of(
                "status", 401,
                "error", "Unauthorized",
                "message", message,
                "path", request.getRequestURI()
        ));
    }
}
