package com.openclassrooms.etudiant.configuration.security;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import com.openclassrooms.etudiant.entities.User;
import com.openclassrooms.etudiant.entities.UserRoleEnum;
import com.openclassrooms.etudiant.repository.UserRepository;
import com.openclassrooms.etudiant.service.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.servlet.FilterChain;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("JwtAuthenticationFilterTest")
public class JwtAuthentificationFilterTest {

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain filterChain;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = mock(FilterChain.class);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @Tag("doFilterInternal")
    @DisplayName("Tests for doFilterInternal method")
    class DoFilterInternalTests {

        @Test
        @DisplayName("Given valid JWT authenticates user and continues chain")
        public void test_doFilterInternal_validJwt_authenticatesAndContinues() throws Exception {
            // GIVEN
            request.addHeader("Authorization", "Bearer valid.jwt.token");

            when(jwtService.validateTokenAndGetUsername("valid.jwt.token")).thenReturn("validUser");
            User user = new User();
            user.setLogin("validUser");
            user.setRole(UserRoleEnum.USER);
            when(userRepository.findByLogin("validUser")).thenReturn(Optional.of(user));

            // WHEN
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // THEN
            assertNotNull(SecurityContextHolder.getContext().getAuthentication(),
                    "Authentication should be set in security context");
            assertEquals(user, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
            verify(filterChain, times(1)).doFilter(request, response);
        }

        @Test
        @DisplayName("Given invalid JWT returns 401 and does not continue chain")
        public void test_doFilterInternal_invalidJwt_returns401() throws Exception {
            // GIVEN
            request.addHeader("Authorization", "Bearer bad.jwt.token");

            when(jwtService.validateTokenAndGetUsername("bad.jwt.token"))
                    .thenThrow(new RuntimeException("Invalid or expired JWT token"));

            // WHEN
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // THEN
            assertEquals(HttpStatus.SC_UNAUTHORIZED, response.getStatus());
            assertTrue(response.getContentAsString().contains("Invalid or expired JWT token"));
            verify(filterChain, never()).doFilter(any(), any());
        }

        @Test
        @DisplayName("Given valid JWT but user not found returns exception and does not continue chain")
        public void test_doFilterInternal_validJwtButUserNotFound_returnsException() throws Exception {
            // GIVEN
            request.addHeader("Authorization", "Bearer valid.jwt.token");

            when(jwtService.validateTokenAndGetUsername("valid.jwt.token")).thenReturn("nonExistentUser");
            when(userRepository.findByLogin("nonExistentUser"))
                    .thenReturn(Optional.empty());

            // WHEN
            assertThrows(RuntimeException.class,
                    () -> jwtAuthenticationFilter.doFilterInternal(request, response, filterChain));

            // THEN
            verify(filterChain, never()).doFilter(any(), any());
        }

        @Test
        @DisplayName("Given Authorization header without Bearer prefix continues chain without authentication")
        public void test_doFilterInternal_noBearerPrefix_continuesChain() throws Exception {

            // GIVEN
            request.addHeader("Authorization", "SomeOtherHeader value");

            // WHEN
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // THEN
            assertNull(SecurityContextHolder.getContext().getAuthentication(),
                    "Authentication should not be set in security context");
            verify(filterChain, times(1)).doFilter(request, response);
        }
    }

}
