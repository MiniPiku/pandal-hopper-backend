package org.minipiku.pandalhopperv2.Security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.minipiku.pandalhopperv2.DTOs.AuthDTO.LoginResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;
    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = oauthToken.getPrincipal();
        String registrationId = oauthToken.getAuthorizedClientRegistrationId();

        // Create/lookup user and generate JWT
        ResponseEntity<LoginResponseDTO> loginResponse =
                authService.handleOauth2LoginUser(oAuth2User, registrationId);
        LoginResponseDTO body = loginResponse.getBody();
        if (body == null) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Login failed");
            return;
        }

        // Build redirect URL to your frontend callback page
        String redirectUrl = String.format(
                "http://localhost:3000/auth/callback?token=%s&userId=%s",
                URLEncoder.encode(body.getJwt(), StandardCharsets.UTF_8),
                URLEncoder.encode(String.valueOf(body.getUserId()), StandardCharsets.UTF_8)
        );

        // Redirect the browser
        response.sendRedirect(redirectUrl);
    }
}
