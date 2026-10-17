package com.utefit.security;

import com.utefit.entity.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;

    public JwtService(
            @Value("${utefit.jwt.secret-base64}")
            String base64Secret
    ) {
        byte[] decoded =
                Decoders.BASE64.decode(base64Secret);

        if (decoded.length < 32) {
            throw new IllegalArgumentException(
                    "JWT secret phải có tối thiểu 32 bytes"
            );
        }

        this.key = Keys.hmacShaKeyFor(decoded);
    }

    public String generate(User user) {

        Instant now = Instant.now();

        int version = user.getTokenVersion() == null
                ? 0
                : user.getTokenVersion();

        return Jwts.builder()
                .issuer("UTEFit")
                .subject(user.getId().toString())
                .claim("ver", version)
                .issuedAt(Date.from(now))
                .expiration(
                        Date.from(
                                now.plus(Duration.ofHours(2))
                        )
                )
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public Claims parseVerified(String token) {

        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer("UTEFit")
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}