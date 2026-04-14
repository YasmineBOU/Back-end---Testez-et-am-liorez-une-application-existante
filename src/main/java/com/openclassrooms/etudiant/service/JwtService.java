// package com.openclassrooms.etudiant.service;


// import org.springframework.security.core.userdetails.UserDetails;
// import org.springframework.stereotype.Service;

// @Service
// public class JwtService {

//     public String generateToken(UserDetails userDetails) {
//         return null; // TODO
//     }

// }

package com.openclassrooms.etudiant.service;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Base64;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;



@Service
public class JwtService {

    private final Key encodedKey;
    private final long jwtExpirationMs;

    public JwtService(
        @Value("${com.openclassrooms.etudiant.jwt.secret-key}") String key,
        @Value("${com.openclassrooms.etudiant.jwt.expiration-ms}") long jwtExpirationMs
    ) {
        this.jwtExpirationMs = jwtExpirationMs;
        this.encodedKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(key));
    }


    /**
     * Generate a JWT token containing the user's username and roles.
     * 
     * @param userDetails the user details containing the username and roles
     * @return a JWT token containing the username and roles
     */
    public String generateToken(UserDetails userDetails) {
        return Jwts.builder()
            .setSubject(userDetails.getUsername())
            .claim("roles", userDetails.getAuthorities())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
            .signWith(encodedKey)
            .compact();
    }
    

    // Validate token and return username if valid, else throw exception
    public String validateTokenAndGetUsername(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(encodedKey)
                .build()
                .parseClaimsJws(token)
                .getBody();

            return claims.getSubject();

        } catch (JwtException | IllegalArgumentException e) {
            // Token is invalid, expired, or malformed
            throw new RuntimeException("Invalid JWT token", e);
        }
    }
        
}
