package com.openclassrooms.etudiant.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclassrooms.etudiant.configuration.security.CustomUserDetailService;
import com.openclassrooms.etudiant.configuration.security.JwtAuthenticationFilter;
import com.openclassrooms.etudiant.dto.AddUserRequestDTO;
import com.openclassrooms.etudiant.dto.LoginRequestDTO;
import com.openclassrooms.etudiant.dto.RegisterDTO;
import com.openclassrooms.etudiant.entities.User;
import com.openclassrooms.etudiant.entities.UserRoleEnum;
import com.openclassrooms.etudiant.handler.RestExceptionHandler;
import com.openclassrooms.etudiant.mapper.UserDtoMapper;
import com.openclassrooms.etudiant.service.JwtService;
import com.openclassrooms.etudiant.service.UserService;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.mockito.Mockito.withSettings;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.securityContext;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.test.context.TestSecurityContextHolder;

import java.util.List;
import java.util.Map;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(RestExceptionHandler.class)
@ContextConfiguration(classes = { UserController.class, RestExceptionHandler.class })
public class UserControllerTest {

    private static final Map<String, String> URLS_BY_METHOD = Map.of(
            "register", "/api/register",
            "login", "/api/login",
            "addUser", "/api/add-user",
            "readUser", "/api/read-user",
            "readUserById", "/api/read-user/{id}",
            "deleteUserById", "/api/delete-user/{id}",
            "updateUser", "/api/update-user/{id}");
    private String URL;
    private static final String FIRST_NAME = "John";
    private static final String LAST_NAME = "Doe";
    private static final String LOGIN = "login";
    private static final String PASSWORD = "password";
    private static final UserRoleEnum ROLE = UserRoleEnum.USER;

    @MockBean
    private UserService userService;

    @MockBean
    private UserDtoMapper userDtoMapper;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private CustomUserDetailService customUserDetailService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    // private UserController userController;

    @BeforeAll
    public static void setUpAll() {
        ObjectMapper objectMapper = new ObjectMapper();
    }

    private Map getResponseBodyAsMap(MvcResult mvcResult) throws Exception {
        String responseContent = mvcResult.getResponse().getContentAsString();
        return objectMapper.readValue(responseContent, new TypeReference<Map>() {
        });
    }

    @Nested
    @Tag("userIsAuthenticated")
    @DisplayName("Tests for userIsAuthenticated method")
    class UserIsAuthenticatedTests {
        @Test
        @DisplayName("Given no authenticated user, when userIsAuthenticated is called, then UNAUTHORIZED is returned.")
        public void test_userIsAuthenticated_WhenUserIsNull_ShouldReturnUnauthorized() throws Exception {
            UserController userController = new UserController(userService, userDtoMapper);
            // WHEN
            ResponseEntity<?> response = userController.userIsAuthenticated(null);

            // THEN
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertEquals("Authentication required", ((Map<?, ?>) response.getBody()).get("error"));
        }

        @Test
        @WithMockUser(username = "user", roles = { "USER" }) // mock an authenticated user without admin role
        @DisplayName("Given an authenticated user without admin role, when userIsAuthenticated is called, then FORBIDDEN is returned.")
        public void test_userIsAuthenticated_WhenUserIsNotAdmin_ShouldReturnForbidden() throws Exception {
            // GIVEN
            UserController userController = new UserController(userService, userDtoMapper);
            User authenticatedUser = new User();
            authenticatedUser.setLogin("any_user");
            authenticatedUser.setRole(UserRoleEnum.USER); // Non-admin role

            // WHEN
            ResponseEntity<?> response = userController.userIsAuthenticated(authenticatedUser);

            // THEN
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertEquals("Admin privileges required", ((Map<?, ?>) response.getBody()).get("error"));
        }

        @Test
        @WithMockUser(username = "admin", roles = { "ADMIN" }) // mock an authenticated user with admin role
        @DisplayName("Given an authenticated user with admin role, when userIsAuthenticated is called, then null is returned.")
        public void test_userIsAuthenticated_WhenUserIsAdmin_ShouldReturnNull() throws Exception {
            // GIVEN
            UserController userController = new UserController(userService, userDtoMapper);
            User authenticatedUser = new User();
            authenticatedUser.setLogin("admin");
            authenticatedUser.setRole(UserRoleEnum.ADMIN);

            // WHEN
            ResponseEntity<?> response = userController.userIsAuthenticated(authenticatedUser);

            // THEN
            assertThat(response).isNull();
        }

    }

    @Nested
    @Tag("register")
    @DisplayName("Tests for register method")
    class RegisterTests {

        @BeforeEach
        public void setUp() {
            URL = URLS_BY_METHOD.get("register");
        }

        @Test
        @DisplayName("Given a null user, when register is called, then IllegalArgumentException is thrown.")
        public void test_register_UserWithoutRequiredData_ShouldThrowIllegalArgumentException() throws Exception {
            // GIVEN
            RegisterDTO registerDTO = new RegisterDTO();

            // WHEN
            mockMvc.perform(MockMvcRequestBuilders.post(URL)
                    .content(objectMapper.writeValueAsString(registerDTO))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(MockMvcResultMatchers.status().isBadRequest());
        }

        @Test
        @DisplayName("Given an existing user, when register is called, then IllegalArgumentException is thrown.")
        public void test_register_AlreadyExistUser_ShouldThrowIllegalArgumentException() throws Exception {
            // GIVEN
            User user = new User();
            user.setFirstName(FIRST_NAME);
            user.setLastName(LAST_NAME);
            user.setLogin(LOGIN);
            user.setPassword(PASSWORD);

            when(userDtoMapper.toEntity(any(RegisterDTO.class))).thenReturn(user);
            doThrow(new IllegalArgumentException("User with login " + LOGIN + " already exists"))
                    .when(userService).register(any(User.class));

            RegisterDTO registerDTO = new RegisterDTO();
            registerDTO.setFirstName(FIRST_NAME);
            registerDTO.setLastName(LAST_NAME);
            registerDTO.setLogin(LOGIN);
            registerDTO.setPassword(PASSWORD);

            // WHEN
            mockMvc.perform(MockMvcRequestBuilders.post(URL)
                    .content(objectMapper.writeValueAsString(registerDTO))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(MockMvcResultMatchers.status().isBadRequest());
        }

        @Test
        @DisplayName("Given a valid user, when register is called, then User is created.")
        public void test_register_UserSuccessful_ShouldCreateUser() throws Exception {
            // GIVEN
            RegisterDTO registerDTO = new RegisterDTO();
            registerDTO.setFirstName(FIRST_NAME);
            registerDTO.setLastName(LAST_NAME);
            registerDTO.setLogin(LOGIN);
            registerDTO.setPassword(PASSWORD);

            User user = new User();
            user.setFirstName(FIRST_NAME);
            user.setLastName(LAST_NAME);
            user.setLogin(LOGIN);
            user.setPassword(PASSWORD);
            when(userDtoMapper.toEntity(any(RegisterDTO.class))).thenReturn(user);

            // WHEN
            mockMvc.perform(MockMvcRequestBuilders.post(URL)
                    .content(objectMapper.writeValueAsString(registerDTO))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(MockMvcResultMatchers.status().isCreated());

            verify(userService, times(1)).register(any(User.class));
        }
    }

    @Nested
    @Tag("login")
    @DisplayName("Tests for login method")
    class LoginTests {

        @BeforeEach
        public void setUp() {
            URL = URLS_BY_METHOD.get("login");
        }

        @Test
        @DisplayName("Given null login and password, when login is called, then BadRequest is returned.")
        public void test_login_NullLoginAndPassword_ShouldReturnBadRequest() throws Exception {

            // WHEN
            mockMvc.perform(MockMvcRequestBuilders.post(URL)
                    // .content(objectMapper.writeValueAsString(loginRequestDTO))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(MockMvcResultMatchers.status().isBadRequest());
        }

        @Test
        @DisplayName("Given invalid credentials, when login is called, then BadCredentialsException is returned.")
        public void test_login_InvalidCredentials_ShouldReturnUnauthorized() throws Exception {
            // GIVEN
            LoginRequestDTO loginRequestDTO = new LoginRequestDTO();
            loginRequestDTO.setLogin("BAD_LOGIN");
            loginRequestDTO.setPassword("BAS_PASSWORD");

            when(userService.login(
                    loginRequestDTO.getLogin(), loginRequestDTO.getPassword()))
                    .thenThrow(new BadCredentialsException("Invalid credentials"));

            // WHEN
            mockMvc.perform(MockMvcRequestBuilders.post(URL)
                    .content(objectMapper.writeValueAsString(loginRequestDTO))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(MockMvcResultMatchers.status().isUnauthorized());

        }

        @Test
        @DisplayName("Given valid credentials, when login is called, then JWT token is returned.")
        public void test_login_validCredentials_ShouldReturnJwtToken() throws Exception {
            // GIVEN
            LoginRequestDTO loginRequestDTO = new LoginRequestDTO();
            loginRequestDTO.setLogin(LOGIN);
            loginRequestDTO.setPassword(PASSWORD);

            when(userService.login(LOGIN, PASSWORD)).thenReturn("valid.jwt.token");

            // WHEN
            MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.post(URL)
                    .content(objectMapper.writeValueAsString(loginRequestDTO))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andReturn();

            Map responseBody = getResponseBodyAsMap(mvcResult);

            // THEN
            assertThat(responseBody.get("token")).isEqualTo("valid.jwt.token");
            assertThat(responseBody.get("message")).isEqualTo("Logged successfully !");

        }
    }

    @Nested
    @Tag("addUser")
    @DisplayName("Tests for add-user method")
    class AddUserTests {

        @BeforeEach
        public void setUp() {
            URL = URLS_BY_METHOD.get("addUser");
        }

        @Test
        @DisplayName("Given an authenticated admin user, when add-user is called, then Created is returned.")
        public void test_addUser_WhenUserIsAdmin_ShouldReturnCreated() throws Exception {
            // GIVEN
            User authenticatedUser = new User();
            authenticatedUser.setLogin("admin");
            authenticatedUser.setRole(UserRoleEnum.ADMIN);

            Authentication auth = new UsernamePasswordAuthenticationToken(
                    authenticatedUser,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

            SecurityContext securityContext = new SecurityContextImpl();
            securityContext.setAuthentication(auth);
            TestSecurityContextHolder.setContext(securityContext);

            // Set new user details
            AddUserRequestDTO addUserRequestDTO = new AddUserRequestDTO();
            addUserRequestDTO.setLogin(LOGIN);
            addUserRequestDTO.setPassword(PASSWORD);
            addUserRequestDTO.setFirstName(FIRST_NAME);
            addUserRequestDTO.setLastName(LAST_NAME);
            addUserRequestDTO.setRole(ROLE);

            // Mock the mapping from DTO to entity
            User newUser = new User();
            newUser.setId(1L);
            newUser.setLogin(LOGIN);
            newUser.setPassword(PASSWORD);
            newUser.setFirstName(FIRST_NAME);
            newUser.setLastName(LAST_NAME);
            newUser.setRole(ROLE);

            when(userDtoMapper.toEntity(addUserRequestDTO)).thenReturn(newUser);
            when(userService.addUser(any(User.class), any(User.class))).thenReturn(newUser);

            // WHEN
            MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.post(URL)
                    .with(securityContext(securityContext))
                    .content(objectMapper.writeValueAsString(addUserRequestDTO))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(MockMvcResultMatchers.status().isCreated())
                    .andReturn();

            Map responseBody = getResponseBodyAsMap(mvcResult);

            // THEN
            assertThat(responseBody.get("message")).isEqualTo("User created successfully");
            assertThat(Long.valueOf(responseBody.get("userId").toString())).isEqualTo(newUser.getId());
            assertThat(responseBody.get("login")).isEqualTo(addUserRequestDTO.getLogin());
            // Clean up
            TestSecurityContextHolder.clearContext();

        }
    }

}
