package org.minipiku.pandalhopperv2.Security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.minipiku.pandalhopperv2.DTOs.AuthDTO.LoginResponseDTO;
import org.springframework.beans.factory.annotation.Value;
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
    private final OAuth2CodeExchangeService codeExchangeService;

    /** Was hardcoded to the production Vercel URL; now environment-specific. */
    @Value("${app.frontend.callback-url:http://localhost:5173/auth/callback}")
    private String frontendCallbackUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = oauthToken.getPrincipal();
        String registrationId = oauthToken.getAuthorizedClientRegistrationId();

        ResponseEntity<LoginResponseDTO> loginResponse =
                authService.handleOauth2LoginUser(oAuth2User, registrationId);
        LoginResponseDTO body = loginResponse.getBody();
        if (body == null) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Login failed");
            return;
        }

        // Redirect with a single-use code rather than the JWT. The token itself
        // never enters the URL bar, browser history, or a Referer header.
        String code = codeExchangeService.issueCode(body);
        String redirectUrl = frontendCallbackUrl
                + (frontendCallbackUrl.contains("?") ? "&" : "?")
                + "code=" + URLEncoder.encode(code, StandardCharsets.UTF_8);

        response.sendRedirect(redirectUrl);
    }
}
