package dz.vecopharm.vecoassets.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import dz.vecopharm.vecoassets.exception.ApiError;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Filter-chain-level fallback for 403s (e.g. a future {@code hasAuthority()}
 * matcher directly in {@code SecurityConfig}). Most permission denials
 * today come from {@code @PreAuthorize} on controller methods, which are
 * caught earlier by {@link dz.vecopharm.vecoassets.exception.GlobalExceptionHandler}
 * - this handler exists so nothing falls through to Spring Security's
 * default (non-JSON) 403 page.
 */
@Component
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public JsonAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiError error = ApiError.of(403, "ACCESS_DENIED", "Permission insuffisante", request.getRequestURI());
        objectMapper.writeValue(response.getWriter(), error);
    }
}
