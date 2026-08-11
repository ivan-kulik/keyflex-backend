package com.keyflex.auth;

import com.keyflex.auth.dto.request.LoginRequest;
import com.keyflex.auth.dto.request.RefreshTokenRequest;
import com.keyflex.auth.dto.request.RegisterRequest;
import com.keyflex.auth.dto.response.ApiResponse;
import com.keyflex.auth.dto.response.TokenResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        this.authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "User registered successfully", null)
                );
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        TokenResponse tokens = this.authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(
                "Login successful", tokens)
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(
            @RequestBody RefreshTokenRequest request
    ) {
        TokenResponse tokens = this.authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success(
                "Token refreshed", tokens)
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            Authentication authentication
    ) {
        this.authService.logout(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(
                "Logged out successfully", null)
        );
    }
}
