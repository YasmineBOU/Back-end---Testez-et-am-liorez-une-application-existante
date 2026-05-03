package com.openclassrooms.etudiant.handler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.nio.file.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("RestExceptionHandlerTest")
@DisplayName("Tests for RestExceptionHandler")
class RestExceptionHandlerTest {

    private final RestExceptionHandler restExceptionHandler = new RestExceptionHandler();

    private WebRequest buildRequest(String uri) {
        return new ServletWebRequest(new org.springframework.mock.web.MockHttpServletRequest("GET", uri));
    }

    @Nested
    @Tag("exceptionHandlers")
    @DisplayName("Tests for mapped exception handlers")
    class ExceptionHandlerMethods {

        @Test
        @DisplayName("Given IllegalArgumentException, when handleConflict is called, then 400 with ErrorDetails is returned.")
        void test_handleConflict_withIllegalArgumentException_returnsBadRequest() {
            // GIVEN
            IllegalArgumentException exception = new IllegalArgumentException("Invalid argument");

            // WHEN
            ResponseEntity<Object> response = restExceptionHandler.handleConflict(exception,
                    buildRequest("/api/conflict"));

            // THEN
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            ErrorDetails body = assertInstanceOf(ErrorDetails.class, response.getBody());
            assertEquals("Invalid argument", body.getMessage());
            assertEquals("uri=/api/conflict", body.getDetails());
            assertNotNull(body.getTimestamp());
        }

        @Test
        @DisplayName("Given BadCredentialsException, when handleBadCredentialsException is called, then 401 with ErrorDetails is returned.")
        void test_handleBadCredentialsException_returnsUnauthorized() {
            // GIVEN
            BadCredentialsException exception = new BadCredentialsException("Invalid credentials");

            // WHEN
            ResponseEntity<Object> response = restExceptionHandler.handleBadCredentialsException(exception,
                    buildRequest("/api/login"));

            // THEN
            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
            ErrorDetails body = assertInstanceOf(ErrorDetails.class, response.getBody());
            assertEquals("Invalid credentials", body.getMessage());
            assertEquals("uri=/api/login", body.getDetails());
            assertNotNull(body.getTimestamp());
        }

        @Test
        @DisplayName("Given AccessDeniedException, when handleForbiddenException is called, then 403 with ErrorDetails is returned.")
        void test_handleForbiddenException_returnsForbidden() throws Exception {
            // GIVEN
            AccessDeniedException exception = new AccessDeniedException("forbidden resource");

            // WHEN
            ResponseEntity<Object> response = restExceptionHandler.handleForbiddenException(exception,
                    buildRequest("/api/secure"));

            // THEN
            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
            ErrorDetails body = assertInstanceOf(ErrorDetails.class, response.getBody());
            assertEquals("forbidden resource", body.getMessage());
            assertEquals("uri=/api/secure", body.getDetails());
            assertNotNull(body.getTimestamp());
        }

        @Test
        @DisplayName("Given RuntimeException, when handleException is called, then 500 with generic message is returned.")
        void test_handleException_returnsInternalServerError() {
            // GIVEN
            RuntimeException exception = new RuntimeException("unexpected");

            // WHEN
            ResponseEntity<Object> response = restExceptionHandler.handleException(exception,
                    buildRequest("/api/unexpected"));

            // THEN
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals("Internal Server error", response.getBody());
        }
    }
}
