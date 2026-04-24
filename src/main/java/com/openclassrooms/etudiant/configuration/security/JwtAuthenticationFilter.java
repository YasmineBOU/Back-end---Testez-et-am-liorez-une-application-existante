package com.openclassrooms.etudiant.configuration.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.openclassrooms.etudiant.service.JwtService;
import com.openclassrooms.etudiant.repository.UserRepository;
import com.openclassrooms.etudiant.entities.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(@org.springframework.lang.NonNull HttpServletRequest request,
            @org.springframework.lang.NonNull HttpServletResponse response,
            @org.springframework.lang.NonNull FilterChain filterChain) throws ServletException, IOException {

        log.info("JwtAuthenticationFilter executing for: {} {}", request.getMethod(), request.getRequestURI());

        final String authHeader = request.getHeader("Authorization");
        final String tokenPrefix = "Bearer ";

        String usernameTemp = null;
        String jwtToken = null;

        // Check if Authorization header is present and starts with Bearer
        if (authHeader != null && authHeader.startsWith(tokenPrefix)) {
            jwtToken = authHeader.substring(tokenPrefix.length());
            log.info("Found JWT token in request");
            try {
                usernameTemp = jwtService.validateTokenAndGetUsername(jwtToken);
                log.info("Token validated for user: {}", usernameTemp);
            } catch (RuntimeException e) {
                log.warn("Invalid JWT token: {}", e.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Invalid or expired JWT token");
                return;
            }
        }

        // If username extracted and authentication is absent or anonymous, authenticate
        // user
        final String username = usernameTemp;
        var currentAuth = SecurityContextHolder.getContext().getAuthentication();
        if (username != null && (currentAuth == null || currentAuth instanceof AnonymousAuthenticationToken)) {

            log.debug("Authenticating user: {}", username);
            User user = userRepository.findByLogin(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));

            List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_" + user.getRole().name()) // Exemple : "ROLE_ADMIN" ou "ROLE_USER"
            );
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                    user,
                    null,
                    authorities);

            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // Set authenticated user in security context
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            log.info("User {} authenticated successfully", username);
        }
        filterChain.doFilter(request, response);
    }
}
