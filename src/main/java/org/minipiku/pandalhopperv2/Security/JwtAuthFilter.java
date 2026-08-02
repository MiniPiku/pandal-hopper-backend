package org.minipiku.pandalhopperv2.Security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.minipiku.pandalhopperv2.Repository.UserRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final AuthUtil authUtil;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        log.debug("Incoming request: {}", request.getRequestURI());

        String path = request.getServletPath();
        if (path.startsWith("/auth/")) { // Skip auth endpoints
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            // No credentials presented. Anonymous requests are still allowed
            // through; the authorization rules decide whether that is enough.
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7).trim();

        try {
            String username = authUtil.getUserNameFromToken(token);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                userRepository.findByUsername(username).ifPresent(user -> {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                });
            }
        } catch (ExpiredJwtException e) {
            rejectToken(request, response, "Token has expired");
            return;
        } catch (JwtException | IllegalArgumentException e) {
            // Malformed, tampered, or wrongly-signed token. Previously this was
            // swallowed and the request continued as anonymous, so a forged
            // token was indistinguishable from no token at all.
            log.warn("Rejected JWT for {}: {}", request.getRequestURI(), e.getMessage());
            rejectToken(request, response, "Invalid token");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * A presented-but-invalid token is an error, not a downgrade to anonymous.
     * Clearing the context and returning 401 stops the request here rather than
     * letting it reach a permitAll endpoint under a false identity.
     */
    private void rejectToken(HttpServletRequest request,
                             HttpServletResponse response,
                             String message) throws IOException {
        SecurityContextHolder.clearContext();
        request.setAttribute(RestAuthenticationEntryPoint.ERROR_ATTRIBUTE, message);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(
                "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"" + message + "\"}"
        );
    }
}
