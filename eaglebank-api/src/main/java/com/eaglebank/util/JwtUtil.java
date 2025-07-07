package com.eaglebank.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

/**
 * Utility class for handling JSON Web Tokens (JWT) operations such as
 * generating tokens, extracting usernames from tokens, and validating tokens.
 *
 * Uses a symmetric secret key with HS256 signing algorithm.
 */
@Component
public class JwtUtil {

    // Secret key used for signing and verifying JWT tokens.
    // Must be kept secure and sufficiently long for HS256 algorithm.
    @Value(("${eagle-bank-api.secret-key}"))
    private String secret;

    /**
     * Generates a JWT token for the given username.
     * The token contains the username as the subject, issued time,
     * and expiration time (30 minutes from issue).
     *
     * @param username the username for which the token is generated
     * @return a signed JWT token string
     */
    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 30))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extracts the username (subject) from a given JWT token.
     *
     * @param token the JWT token string
     * @return the username contained in the token
     * @throws JwtException if token parsing fails (e.g. invalid token)
     */
    public String extractUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    /**
     * Validates the JWT token by verifying its signature and checking
     * that the username extracted from the token matches the provided UserDetails.
     *
     * @param token the JWT token string to validate
     * @param userDetails the user details to match username against
     * @return true if token is valid and username matches; false otherwise
     */
    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            String username = extractUsername(token);
            return username.equals(userDetails.getUsername());
        } catch (JwtException e) {
            return false;
        }
    }

    /**
     * Returns the SecretKey used to sign and verify JWT tokens.
     *
     * @return the HMAC SHA key generated from the secret string
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}