package com.keyflex.auth.service;

import com.keyflex.auth.dto.request.LoginRequest;
import com.keyflex.auth.dto.request.RefreshTokenRequest;
import com.keyflex.auth.dto.request.RegisterRequest;
import com.keyflex.auth.dto.response.TokenResponse;
import com.keyflex.auth.entity.RefreshToken;
import com.keyflex.auth.exception.TokenRefreshException;
import com.keyflex.auth.repository.RefreshTokenRepository;
import com.keyflex.common.exception.BadRequestException;
import com.keyflex.common.security.JwtTokenManager;
import com.keyflex.common.util.HashUtil;
import com.keyflex.user.entity.User;
import com.keyflex.user.exception.UserAlreadyExistsException;
import com.keyflex.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenManager jwtTokenManager;
    private final AuthenticationManager authManager;
    private final UserDetailsService userDetailsService;

    @Value("${jwt.access-token-expiration}")
    private Long accessTokenExpirationMs;

    @Value("${jwt.refresh-token-expiration}")
    private Long refreshTokenExpirationMs;

    @Value("${jwt.token-type}")
    private String tokenType;

    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new UserAlreadyExistsException(
                    "Username already taken: " + request.username());
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException(
                    "Email already registered: " + request.email());
        }

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .build();
        this.userRepository.save(user);
        log.info("User registered successfully: {}", request.username());
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        Authentication authentication = this.authManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.login(),
                        request.password()
                )
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        User user = this.userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("User disappeared"));

        if (!user.isEmailVerified()) {
            throw new BadRequestException("Email not verified. Please check your inbox.");
        }

        this.refreshTokenRepository.deleteByUser(user);

        String accessToken = this.jwtTokenManager.generateAccessToken(userDetails);
        String refreshToken = createRefreshToken(userDetails);

        return new TokenResponse(
                accessToken,
                refreshToken,
                this.tokenType,
                this.accessTokenExpirationMs / 1_000
        );
    }

    @Transactional
    public TokenResponse refreshToken(RefreshTokenRequest request) {
        String rawRefreshToken = request.refreshToken();

        UserDetails userDetails = this.userDetailsService.loadUserByUsername(
                this.jwtTokenManager.extractUsername(rawRefreshToken)
        );

        if (!jwtTokenManager.isRefreshToken(rawRefreshToken)) {
            throw new TokenRefreshException("Invalid token type");
        }

        if (!jwtTokenManager.isTokenValid(rawRefreshToken, userDetails)) {
            throw new TokenRefreshException("Invalid refresh token");
        }

        String tokenHash = HashUtil.sha256(rawRefreshToken);
        RefreshToken refreshToken = this.refreshTokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(() ->
                        new TokenRefreshException("Refresh token revoked")
                );

        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            this.refreshTokenRepository.delete(refreshToken);
            throw new TokenRefreshException("Refresh token expired. Please login again.");
        }

        this.refreshTokenRepository.delete(refreshToken);

        String newAccessToken = this.jwtTokenManager.generateAccessToken(userDetails);
        String newRefreshToken = createRefreshToken(userDetails);

        return new TokenResponse(
                newAccessToken,
                newRefreshToken,
                this.tokenType,
                this.refreshTokenExpirationMs / 1_000
        );
    }

    @Transactional
    public void logout(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new BadRequestException("User not found")
                );

        refreshTokenRepository.deleteByUser(user);
        log.info("User logged out: {}", username);
    }

    private String createRefreshToken(UserDetails userDetails) {
        String rawToken = this.jwtTokenManager.generateRefreshToken(userDetails);
        String tokenHash = HashUtil.sha256(rawToken);

        User user = this.userRepository
                .findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user not found in database: " + userDetails.getUsername()
                ));

        RefreshToken refreshToken = RefreshToken.builder()
                .tokenHash(tokenHash)
                .user(user)
                .expiryDate(Instant.now().plusMillis(89))
                .build();
        this.refreshTokenRepository.save(refreshToken);
        return rawToken;
    }
}
