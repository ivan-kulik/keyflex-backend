package com.keyflex.auth.service;

import com.keyflex.auth.entity.EmailVerificationToken;
import com.keyflex.auth.repository.EmailVerificationTokenRepository;
import com.keyflex.common.exception.BadRequestException;
import com.keyflex.common.mail.EmailSender;
import com.keyflex.common.util.HashUtil;
import com.keyflex.user.entity.User;
import com.keyflex.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailSender emailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${app.email-verification.expiration-hours}")
    private long expirationHours;

    @Value("${app.email-verification.base-url}")
    private String baseUrl;

    public void createAndSendVerificationToken(User user) {
        this.tokenRepository.deleteByUser(user);

        String rawToken = UUID.randomUUID().toString();
        String tokenHash = HashUtil.sha256(rawToken);

        EmailVerificationToken token = EmailVerificationToken.builder()
                .tokenHash(tokenHash)
                .user(user)
                .expiryDate(Instant.now().plus(expirationHours, ChronoUnit.HOURS))
                .isUsed(false)
                .build();

        this.tokenRepository.save(token);

        String link = baseUrl + "/api/auth/verify-email?token=" + rawToken;

        Context context = new Context();
        context.setVariable("link", link);
        String htmlBody = this.templateEngine.process("email/verification", context);

        this.emailSender.send(user.getEmail(), "Confirm your email.", htmlBody);

        log.info("Verification email sent to: {}", user.getEmail());
    }

    @Transactional
    public void verifyEmail(String rawToken) {
        String tokenHash = HashUtil.sha256(rawToken);

        EmailVerificationToken token = this.tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new
                        BadRequestException("Invalid or expired verification link.")
                );

        if (token.isUsed()) {
            throw new BadRequestException("Verification link already used.");
        }

        if (token.getExpiryDate().isBefore(Instant.now())) {
            throw new BadRequestException("Verification link expired.");
        }

        User user = token.getUser();
        user.setEmailVerified(true);
        this.userRepository.save(user);

        this.tokenRepository.delete(token);

        log.info("Email verified for user: {}", user.getUsername());
    }

    @Transactional
    public void resendVerificationEmail(String email) {
        User user = this.userRepository.findByEmail(email)
                .orElseThrow(() -> new
                        BadRequestException("User not found.")
                );

        if (user.isEmailVerified()) {
            throw new BadRequestException("Email already verified.");
        }

        createAndSendVerificationToken(user);
    }

    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupExpiredTokens() {
        this.tokenRepository.deleteByExpiryDateBefore(Instant.now());
    }
}
