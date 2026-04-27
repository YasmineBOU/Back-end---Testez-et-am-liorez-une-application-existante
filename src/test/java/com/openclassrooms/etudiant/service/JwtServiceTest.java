package com.openclassrooms.etudiant.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Tag("JwtServiceTest")
@DisplayName("Tests for JwtService")
public class JwtServiceTest {

    private String encodedKey;
    private long expirationMs;

    private JwtService jwtService;

    @BeforeEach
    public void setUp() {
        encodedKey = Base64.getEncoder()
                .encodeToString(Keys.secretKeyFor(io.jsonwebtoken.SignatureAlgorithm.HS256).getEncoded());
        expirationMs = 3600000L;

        jwtService = new JwtService(encodedKey, expirationMs);
    }

    @Nested
    @Tag("generateToken")
    @DisplayName("Tests for generateToken method")
    class GenerateTokenTests {
        @Test
        @DisplayName("Given a UserDetails, when generateToken is called, then a JWT token is generated.")
        public void test_generateToken() {
            // GIVEN
            UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                    .username("LOGIN")
                    .password("PASSWORD")
                    .roles("USER")
                    .build();
            Set<String> userDetailsRoles = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet());

            // WHEN
            String token = jwtService.generateToken(userDetails);
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(Base64.getDecoder().decode(encodedKey)))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            Set<String> tokenRoles = new HashSet<>(claims.get("roles", List.class));

            // THEN
            assertThat(token).isNotNull();
            assertThat(token).isNotEmpty();
            assertThat(claims.getSubject()).isEqualTo(userDetails.getUsername());
            assertThat(claims.getExpiration().getTime()).isCloseTo(claims.getIssuedAt().getTime() + expirationMs,
                    within(1000L));
            assertThat(tokenRoles).isEqualTo(userDetailsRoles);
        }
    }

    @Nested
    @Tag("validateTokenAndGetUsername")
    class ValidateTokenAndGetUsernameTests {

        @Test
        @DisplayName("Given an invalid token, when validateTokenAndGetUsername is called, then an exception is thrown.")
        public void test_validateTokenAndGetUsername_with_invalid_token_throws_exception() {
            // GIVEN
            String invalidToken = "invalid.token.value";

            // WHEN & THEN
            assertThatThrownBy(() -> {
                jwtService.validateTokenAndGetUsername(invalidToken);
            }).isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Invalid JWT token");
        }

        @Test
        @DisplayName("Given a valid token, when validateTokenAndGetUsername is called, then the username is returned.")
        public void test_validateTokenAndGetUsername_with_valid_token_returns_username() {
            // GIVEN
            UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                    .username("LOGIN")
                    .password("PASSWORD")
                    .roles("USER")
                    .build();
            String token = jwtService.generateToken(userDetails);

            // WHEN
            String username = jwtService.validateTokenAndGetUsername(token);

            // THEN
            assertThat(username).isEqualTo(userDetails.getUsername());
        }

    }

}
