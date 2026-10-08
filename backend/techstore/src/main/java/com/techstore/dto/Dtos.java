package com.techstore.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class Dtos {

    public record RegisterRequest(
            @NotBlank(message = "El email es obligatorio")
            @Email(message = "Email inválido")
            String email,

            @NotBlank(message = "La contraseña es obligatoria")
            @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$",
                    message = "La contraseña debe tener mínimo 8 caracteres, una mayúscula, un número y un carácter especial")
            String password,

            @NotBlank(message = "El nombre completo es obligatorio")
            String fullName,

            @NotBlank(message = "La tienda asignada es obligatoria")
            String store) {}

    public record LoginRequest(
            @NotBlank(message = "El email es obligatorio") String email,
            @NotBlank(message = "La contraseña es obligatoria") String password) {}

    public record MfaVerifyRequest(
            @NotBlank(message = "Falta el token de verificación") String preAuthToken,
            @NotBlank(message = "Ingresa el código")
            @Pattern(regexp = "\\d{6}", message = "El código debe tener 6 dígitos")
            String code) {}

    public record RegisterResponse(String message, String otpauthUri, String mfaSecret) {}

    public record LoginResponse(boolean mfaRequired, String token, String preAuthToken) {}
}