package com.eaglebank.service.impl;

import com.eaglebank.entity.UserEntity;
import com.eaglebank.repository.UserRepository;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

import static com.eaglebank.constants.EagleBankApiConstants.EXPIRED_TOKEN;

/**
 * Service implementation for loading user-specific data during authentication.
 *
 * Implements Spring Security's UserDetailsService to load a user by email.
 * Used by the authentication manager to verify credentials during login.
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements UserDetailsService {

    private final UserRepository repository;

    /**
     * Loads a user from the database using the provided email.
     *
     * @param email the email of the user to load
     * @return UserDetails for Spring Security authentication
     * @throws SignatureException if the user is not found (used to indicate token-related failure)
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        final UserEntity user = repository.findByEmail(email)
                .orElseThrow(() -> new SignatureException(EXPIRED_TOKEN));
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority(user.getRole()))
        );
    }

}
