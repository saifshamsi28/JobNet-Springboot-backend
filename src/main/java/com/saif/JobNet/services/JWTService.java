package com.saif.JobNet.services;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JWTService {

    private final String secretKey;
    private final long accessExpirationMs;
    private final long refreshExpirationMs;

    public JWTService(
            @Value("${app.jwt.secret:JobNetDefaultSecretKeyMustBeAtLeast32Chars!}") String secretKey,
            @Value("${app.jwt.expiration-ms:600000}") long accessExpirationMs,
            @Value("${app.jwt.refresh-expiration-ms:604800000}") long refreshExpirationMs
    ) {
        if (secretKey == null || secretKey.trim().length() < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 characters long");
        }
        this.secretKey = secretKey.trim();
        this.accessExpirationMs = accessExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    public String generateToken(String usernameOrEmail) {
        return generateAccessToken(usernameOrEmail);
    }

    public String generateAccessToken(String usernameOrEmail) {
        return buildToken(usernameOrEmail, "access", accessExpirationMs);
    }

    public String generateRefreshToken(String usernameOrEmail) {
        return buildToken(usernameOrEmail, "refresh", refreshExpirationMs);
    }

    private String buildToken(String usernameOrEmail, String tokenType, long expiryMs) {
        Map<String,Object> claims=new HashMap<>();
        claims.put("tokenType", tokenType);
        return Jwts.builder()
                .claims()
                .add(claims)
                .subject(usernameOrEmail)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiryMs))
                .and()
                .signWith(getKey())
                .compact();
    }

    private SecretKey getKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractUserName(String token) {
        // extract the username from jwt token
        return extractClaim(token, Claims::getSubject);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimResolver) {
        final Claims claims = extractAllClaims(token);
        return claimResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        final String userName = extractUserName(token);
        return isAccessToken(token) && (userName.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    public boolean isRefreshTokenValid(String token) {
        return isRefreshToken(token) && !isTokenExpired(token);
    }

    public boolean isAccessToken(String token) {
        return "access".equalsIgnoreCase(extractTokenType(token));
    }

    public boolean isRefreshToken(String token) {
        return "refresh".equalsIgnoreCase(extractTokenType(token));
    }

    private String extractTokenType(String token) {
        try {
            Claims claims = extractAllClaims(token);
            Object tokenType = claims.get("tokenType");
            return tokenType == null ? "" : tokenType.toString();
        } catch (Exception ignored) {
            return "";
        }
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
}
