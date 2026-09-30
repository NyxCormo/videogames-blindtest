package fr.insalan.blindtest.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import fr.insalan.blindtest.dto.LoginRequest;
import fr.insalan.blindtest.dto.LoginResponse;
import fr.insalan.blindtest.security.AdminAccount;
import fr.insalan.blindtest.security.TokenStore;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AdminAccount adminAccount;
    private final TokenStore tokenStore;

    public AuthController(AdminAccount adminAccount, TokenStore tokenStore) {
        this.adminAccount = adminAccount;
        this.tokenStore = tokenStore;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        if (!adminAccount.matches(request.username(), request.password())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Identifiant ou mot de passe incorrect");
        }
        return new LoginResponse(tokenStore.create(request.username()));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestHeader(name = "Authorization", required = false) String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            tokenStore.revoke(authorization.substring("Bearer ".length()));
        }
    }
}
