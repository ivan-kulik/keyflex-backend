package com.keyflex.auth;

import com.keyflex.auth.dto.request.LoginRequest;
import com.keyflex.auth.dto.request.RefreshTokenRequest;
import com.keyflex.auth.dto.request.RegisterRequest;
import com.keyflex.auth.dto.response.ApiResponse;
import com.keyflex.auth.dto.response.TokenResponse;
import com.keyflex.auth.service.AuthService;
import com.keyflex.auth.service.EmailVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        this.authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "User registered successfully.", null)
                );
    }

    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @RequestParam String token
    ) {
        emailVerificationService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.success(
                "Email verified successfully.", null)
        );
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerification(
            @RequestParam String email
    ) {
        emailVerificationService.resendVerificationEmail(email);
        return ResponseEntity.ok(ApiResponse.success(
                "Verification email sent.", null)
        );
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        TokenResponse tokens = this.authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(
                "Login successful.", tokens)
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(
            @RequestBody RefreshTokenRequest request
    ) {
        TokenResponse tokens = this.authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success(
                "Token refreshed.", tokens)
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
