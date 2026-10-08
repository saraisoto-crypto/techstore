package com.techstore.security;

import com.techstore.model.User;
import com.techstore.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;
    private final JwtService jwtService;
    private final String frontendUrl;

    public OAuth2SuccessHandler(AuthService authService, JwtService jwtService,
                                @Value("${app.frontend-url}") String frontendUrl) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        String provider = token.getAuthorizedClientRegistrationId().toUpperCase();
        OAuth2User principal = token.getPrincipal();

        String email = principal.getAttribute("email");
        String name = principal.getAttribute("name");
        if (email == null) {
            String login = principal.getAttribute("login");
            email = login + "@users.noreply.github.com";
        }
        if (name == null) {
            name = email;
        }

        User user = authService.findOrCreateSocialUser(email, name, provider);
        String jwt = jwtService.generateToken(user);
        response.sendRedirect(frontendUrl + "/auth/callback?token="
                + URLEncoder.encode(jwt, StandardCharsets.UTF_8));
    }
}