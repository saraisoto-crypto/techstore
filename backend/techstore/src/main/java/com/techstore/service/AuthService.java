package com.techstore.service;

import com.techstore.dto.Dtos.*;
import com.techstore.exception.ApiException;
import com.techstore.model.Provider;
import com.techstore.model.User;
import com.techstore.repository.UserRepository;
import com.techstore.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private static final int MAX_FAILED_LOGIN = 3;
    private static final int MAX_MFA_ATTEMPTS = 3;
    private static final int LOCK_MINUTES = 15;

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final MfaService mfa;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt, MfaService mfa) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.mfa = mfa;
    }

    public RegisterResponse register(RegisterRequest r) {
        String email = r.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "El email ya está registrado");
        }
        User u = new User();
        u.setEmail(email);
        u.setPassword(encoder.encode(r.password()));
        u.setFullName(r.fullName().trim());
        u.setStore(r.store().trim());
        u.setMfaSecret(mfa.newSecret());
        users.save(u);
        return new RegisterResponse(
                "Usuario registrado. Escanea el QR con Google Authenticator.",
                mfa.otpauthUri(email, u.getMfaSecret()),
                u.getMfaSecret());
    }

    @Transactional(noRollbackFor = ApiException.class)
    public LoginResponse login(LoginRequest r) {
        String email = r.email().trim().toLowerCase();
        User u = users.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas"));

        if (u.getProvider() != Provider.LOCAL) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Esta cuenta usa inicio de sesión con " + u.getProvider());
        }

        if (u.getLockedUntil() != null) {
            if (LocalDateTime.now().isBefore(u.getLockedUntil())) {
                throw new ApiException(HttpStatus.LOCKED,
                        "Cuenta bloqueada temporalmente. Intenta de nuevo en unos minutos");
            }
            u.setLockedUntil(null);
            u.setFailedAttempts(0);
        }

        if (!encoder.matches(r.password(), u.getPassword())) {
            int n = u.getFailedAttempts() + 1;
            if (n >= MAX_FAILED_LOGIN) {
                u.setFailedAttempts(0);
                u.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
                users.save(u);
                throw new ApiException(HttpStatus.LOCKED,
                        "Cuenta bloqueada por " + MAX_FAILED_LOGIN + " intentos fallidos (" + LOCK_MINUTES + " min)");
            }
            u.setFailedAttempts(n);
            users.save(u);
            throw new ApiException(HttpStatus.UNAUTHORIZED,
                    "Credenciales inválidas. Intentos restantes: " + (MAX_FAILED_LOGIN - n));
        }

        u.setFailedAttempts(0);
        u.setMfaAttempts(0);
        users.save(u);
        return new LoginResponse(true, null, jwt.generatePreAuthToken(u));
    }

    @Transactional(noRollbackFor = ApiException.class)
    public LoginResponse verifyMfa(MfaVerifyRequest r) {
        Claims claims;
        try {
            claims = jwt.parse(r.preAuthToken());
        } catch (JwtException | IllegalArgumentException e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "La verificación expiró. Inicia sesión nuevamente");
        }
        if (!"PRE_AUTH".equals(claims.get("type", String.class))) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Token inválido");
        }
        User u = users.findByEmail(claims.getSubject())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));

        if (u.getLockedUntil() != null && LocalDateTime.now().isBefore(u.getLockedUntil())) {
            throw new ApiException(HttpStatus.LOCKED, "Cuenta bloqueada temporalmente");
        }

        if (!mfa.isValid(u.getMfaSecret(), r.code())) {
            int n = u.getMfaAttempts() + 1;
            if (n >= MAX_MFA_ATTEMPTS) {
                u.setMfaAttempts(0);
                u.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
                users.save(u);
                throw new ApiException(HttpStatus.LOCKED,
                        "Código incorrecto " + MAX_MFA_ATTEMPTS + " veces. Cuenta bloqueada " + LOCK_MINUTES + " min");
            }
            u.setMfaAttempts(n);
            users.save(u);
            throw new ApiException(HttpStatus.UNAUTHORIZED,
                    "Código incorrecto. Intentos restantes: " + (MAX_MFA_ATTEMPTS - n));
        }

        u.setMfaAttempts(0);
        users.save(u);
        return new LoginResponse(false, jwt.generateToken(u), null);
    }

    public User findOrCreateSocialUser(String email, String name, String provider) {
        String e = email.trim().toLowerCase();
        return users.findByEmail(e).orElseGet(() -> {
            User u = new User();
            u.setEmail(e);
            u.setFullName(name);
            u.setStore("Sin asignar");
            u.setProvider(Provider.valueOf(provider));
            return users.save(u);
        });
    }
}