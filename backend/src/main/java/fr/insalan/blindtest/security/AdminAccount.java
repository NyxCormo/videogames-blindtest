package fr.insalan.blindtest.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AdminAccount {

    public static final String USERNAME = "admin";

    private final String password;

    public AdminAccount(@Value("${admin.password}") String password) {
        if (password.isBlank()) {
            throw new IllegalStateException("Mot de passe admin manquant : créer backend/.env à partir de backend/.env.example");
        }
        this.password = password;
    }

    public boolean matches(String username, String candidate) {
        if (!USERNAME.equals(username) || candidate == null) {
            return false;
        }
        return MessageDigest.isEqual(
            password.getBytes(StandardCharsets.UTF_8),
            candidate.getBytes(StandardCharsets.UTF_8)
        );
    }
}
