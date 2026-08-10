package com.keyflex.common.security;

import com.keyflex.user.entity.User;
import com.keyflex.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String login) throws
            UsernameNotFoundException {
        User user = this.userRepository.findByUsername(login)
                .or(() -> this.userRepository.findByEmail(login))
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found: " + login
                ));

        if (!user.isEmailVerified()) {
            throw new DisabledException("Email not verified. Please check your inbox.");
        }

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPasswordHash(),
                new ArrayList<>()
        );
    }
}
