package com.techstore.controller;

import com.techstore.exception.ApiException;
import com.techstore.model.Role;
import com.techstore.model.User;
import com.techstore.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class UserController {

    private final UserRepository users;

    public UserController(UserRepository users) {
        this.users = users;
    }

    private Map<String, Object> toMap(User u) {
        boolean locked = u.getLockedUntil() != null && LocalDateTime.now().isBefore(u.getLockedUntil());
        return Map.of(
                "id", u.getId(),
                "email", u.getEmail(),
                "fullName", u.getFullName(),
                "store", u.getStore() == null ? "" : u.getStore(),
                "role", u.getRole().name(),
                "provider", u.getProvider().name(),
                "locked", locked);
    }

    private User find(Long id) {
        return users.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    @GetMapping("/me")
    public Map<String, Object> me(Authentication auth) {
        User u = users.findByEmail(auth.getName())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        return toMap(u);
    }

    @GetMapping("/reportes")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','AUDITOR')")
    public Map<String, String> reportes() {
        return Map.of("message", "Reporte de inventario generado correctamente");
    }

    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Map<String, Object>> listUsers() {
        return users.findAll().stream().map(this::toMap).toList();
    }

    @PutMapping("/admin/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody Map<String, String> body,
                                      Authentication auth) {
        User u = find(id);
        String role = body.get("role");
        if (role != null) {
            try {
                Role r = Role.valueOf(role);
                if (u.getEmail().equals(auth.getName()) && r != Role.ADMIN) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "No puedes quitarte el rol de administrador");
                }
                u.setRole(r);
            } catch (IllegalArgumentException e) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Rol inválido");
            }
        }
        String store = body.get("store");
        if (store != null && !store.isBlank()) u.setStore(store.trim());
        return toMap(users.save(u));
    }

    @PostMapping("/admin/users/{id}/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> unlock(@PathVariable Long id) {
        User u = find(id);
        u.setLockedUntil(null);
        u.setFailedAttempts(0);
        u.setMfaAttempts(0);
        return toMap(users.save(u));
    }

    @DeleteMapping("/admin/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, String> delete(@PathVariable Long id, Authentication auth) {
        User u = find(id);
        if (u.getEmail().equals(auth.getName())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No puedes eliminar tu propia cuenta");
        }
        users.delete(u);
        return Map.of("message", "Usuario eliminado");
    }
}