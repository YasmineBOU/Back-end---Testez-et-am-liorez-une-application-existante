package com.openclassrooms.etudiant.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclassrooms.etudiant.dto.RegisterDTO;
import com.openclassrooms.etudiant.dto.UserBasicInfoDTO;
import com.openclassrooms.etudiant.entities.User;
import com.openclassrooms.etudiant.entities.UserRoleEnum;
import com.openclassrooms.etudiant.repository.UserRepository;
import com.openclassrooms.etudiant.service.JwtService;
import com.openclassrooms.etudiant.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:etudiant_integrationdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;NON_KEYWORDS=USER",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.globally_quoted_identifiers=true",
        "spring.jpa.show-sql=false",
        "com.openclassrooms.etudiant.jwt.secret-key=test-secret-key-for-integration-tests",
        "com.openclassrooms.etudiant.jwt.expiration-ms=3600000"
})
@AutoConfigureMockMvc
@Tag("SecurityIntegrationTest")
@DisplayName("Integration tests for security and key user flows")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        User admin = new User();
        admin.setFirstName("Admin");
        admin.setLastName("User");
        admin.setLogin("admin");
        admin.setPassword("encoded-admin-password");
        admin.setRole(UserRoleEnum.ADMIN);
        userRepository.saveAndFlush(admin);
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    @Nested
    @Tag("publicRoutes")
    @DisplayName("Tests for public routes")
    class PublicRoutesTests {

        @Test
        @DisplayName("Given a register request without authentication, when /api/register is called, then the request succeeds.")
        void test_register_is_public_and_returnsCreated() throws Exception {
            // GIVEN
            RegisterDTO registerDTO = new RegisterDTO();
            registerDTO.setFirstName("Jane");
            registerDTO.setLastName("Doe");
            registerDTO.setLogin("jane");
            registerDTO.setPassword("password");

            // WHEN / THEN
            mockMvc.perform(post("/api/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(registerDTO)))
                    .andExpect(status().isCreated());
        }
    }

    @Nested
    @Tag("protectedRoutes")
    @DisplayName("Tests for protected routes")
    class ProtectedRoutesTests {

        @Test
        @DisplayName("Given no token, when /api/read-user is called, then access is denied.")
        void test_readUser_withoutAuthentication_returnsUnauthorized() throws Exception {
            // WHEN / THEN
            mockMvc.perform(get("/api/read-user"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Given a valid admin JWT, when /api/read-user is called, then users are returned.")
        void test_readUser_withAdminJwt_returnsUsers() throws Exception {
            // GIVEN
            when(jwtService.validateTokenAndGetUsername("valid.jwt.token")).thenReturn("admin");
            when(userService.getUsers(any(User.class))).thenReturn(List.of(
                    new UserBasicInfoDTO(1L, "Admin", "User", UserRoleEnum.ADMIN)));

            // WHEN / THEN
            mockMvc.perform(get("/api/read-user")
                    .header("Authorization", "Bearer valid.jwt.token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].firstName").value("Admin"))
                    .andExpect(jsonPath("$[0].role").value("ADMIN"));
        }
    }
}
