package dz.vecopharm.vecoassets.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import dz.vecopharm.vecoassets.exception.ApiError;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Replaces Spring Security's default 401 behaviour (which would otherwise
 * be an empty body / basic-auth challenge) with the standardized
 * {@link ApiError} shape used everywhere else in the API. Fires when a
 * protected endpoint is called with no token, or a token that failed to
 * validate in {@link JwtAuthenticationFilter}.
 */
@Component
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JsonAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiError error = ApiError.of(401, "UNAUTHORIZED", "Authentification requise", request.getRequestURI());
        objectMapper.writeValue(response.getWriter(), error);
    }
}
