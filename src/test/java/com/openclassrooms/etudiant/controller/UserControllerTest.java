package com.openclassrooms.etudiant.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclassrooms.etudiant.configuration.security.CustomUserDetailService;
import com.openclassrooms.etudiant.configuration.security.JwtAuthenticationFilter;
import com.openclassrooms.etudiant.controller.UserControllerTest.AddUserTests.MvcResultAndAuthenticatedUser;
import com.openclassrooms.etudiant.dto.AddUserRequestDTO;
import com.openclassrooms.etudiant.dto.LoginRequestDTO;
import com.openclassrooms.etudiant.dto.RegisterDTO;
import com.openclassrooms.etudiant.dto.UpdateRequestDTO;
import com.openclassrooms.etudiant.dto.UserBasicInfoDTO;
import com.openclassrooms.etudiant.dto.UserSummaryDTO;
import com.openclassrooms.etudiant.entities.User;
import com.openclassrooms.etudiant.entities.UserRoleEnum;
import com.openclassrooms.etudiant.handler.RestExceptionHandler;
import com.openclassrooms.etudiant.mapper.UserDtoMapper;
import com.openclassrooms.etudiant.service.JwtService;
import com.openclassrooms.etudiant.service.UserService;

import org.junit.jupiter.api.AfterEach;
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
import org.springframework.cglib.core.Local;
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
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.mockito.Mockito.withSettings;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.securityContext;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.test.context.TestSecurityContextHolder;

import java.time.LocalDateTime;
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
    @DisplayName("Tests for addUser method")
    class AddUserTests {

        private AddUserRequestDTO addUserRequestDTO;

        public record MvcResultAndAuthenticatedUser(User authenticatedUser, Map response) {
        }

        @BeforeEach
        public void setUp() {
            // Get the URL for addUser endpoint
            URL = URLS_BY_METHOD.get("addUser");
            // Set new user details
            addUserRequestDTO = new AddUserRequestDTO();
            addUserRequestDTO.setLogin(LOGIN);
            addUserRequestDTO.setPassword(PASSWORD);
            addUserRequestDTO.setFirstName(FIRST_NAME);
            addUserRequestDTO.setLastName(LAST_NAME);
            addUserRequestDTO.setRole(ROLE);
        }

        @AfterEach
        public void tearDown() {
            TestSecurityContextHolder.clearContext();
        }

        public MvcResultAndAuthenticatedUser getMvcResultWithAuthenticatedUserSetInContext(String login,
                UserRoleEnum role,
                AddUserRequestDTO addUserRequestDTO, ResultMatcher expectedStatus)
                throws Exception {
            // GIVEN
            User authenticatedUser = new User();
            authenticatedUser.setLogin(login);
            authenticatedUser.setRole(role);
            // Create an Authentication object with the authenticated user and their role
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    authenticatedUser,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));

            // Set the authentication in the security context
            SecurityContext securityContext = new SecurityContextImpl();
            securityContext.setAuthentication(auth);
            TestSecurityContextHolder.setContext(securityContext);

            // WHEN
            MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.post(URL)
                    .with(securityContext(securityContext))
                    .content(objectMapper.writeValueAsString(addUserRequestDTO))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(expectedStatus)
                    .andExpect(result -> {
                        String contentType = result.getResponse().getContentType();
                        if (contentType == null) {
                            throw new AssertionError("Content-Type est null");
                        } else if (!(contentType.startsWith("application/json") ||
                                contentType.startsWith("application/problem+json"))) {
                            throw new AssertionError(
                                    "The content type must be 'application/json' or 'application/problem+json', but was: "
                                            + contentType);
                        }
                    })
                    .andReturn();

            return new MvcResultAndAuthenticatedUser(authenticatedUser, getResponseBodyAsMap(mvcResult));
        }

        public Map getMvcResultWithUnauthenticatedUser(String login,
                UserRoleEnum role,
                AddUserRequestDTO addUserRequestDTO, ResultMatcher expectedStatus)
                throws Exception {
            // WHEN
            MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.post(URL)
                    .content(objectMapper.writeValueAsString(addUserRequestDTO))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(expectedStatus)
                    .andExpect(result -> {
                        String contentType = result.getResponse().getContentType();
                        if (contentType == null) {
                            throw new AssertionError("Content-Type est null");
                        } else if (!(contentType.startsWith("application/json") ||
                                contentType.startsWith("application/problem+json"))) {
                            throw new AssertionError(
                                    "The content type must be 'application/json' or 'application/problem+json', but was: "
                                            + contentType);
                        }
                    })
                    .andReturn();

            return getResponseBodyAsMap(mvcResult);
        }

        @Test
        @DisplayName("Given an authenticated 'ADMIN' user, when addUser is called, then Created is returned.")
        public void test_addUser_WhenUserIsAdmin_ShouldReturnCreated() throws Exception {
            // GIVEN
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
            MvcResultAndAuthenticatedUser result = getMvcResultWithAuthenticatedUserSetInContext(
                    "admin",
                    UserRoleEnum.ADMIN,
                    addUserRequestDTO,
                    MockMvcResultMatchers.status().isCreated());

            // THEN
            verify(userDtoMapper, times(1)).toEntity(addUserRequestDTO);
            verify(userService, times(1)).addUser(newUser, result.authenticatedUser());
            assertThat(result.response().get("message")).isEqualTo("User created successfully");
            assertThat(Long.valueOf(result.response().get("userId").toString())).isEqualTo(newUser.getId());
            assertThat(result.response().get("login")).isEqualTo(addUserRequestDTO.getLogin());
            assertThat(result.response()).doesNotContainKeys("password", "passwordHash", "passwordSalt");
            assertThat(result.response()).doesNotContainValue(PASSWORD);

        }

        @Test
        @DisplayName("Given an authenticated 'USER' lambda user, when addUser is called, then FORBIDDEN is returned.")
        public void test_addUser_WhenUserIsUser_ShouldReturnForbidden() throws Exception {
            // GIVEN
            // WHEN
            MvcResultAndAuthenticatedUser result = getMvcResultWithAuthenticatedUserSetInContext(
                    "user",
                    UserRoleEnum.USER,
                    addUserRequestDTO,
                    MockMvcResultMatchers.status().isForbidden());

            // THEN
            verifyNoInteractions(userDtoMapper);
            verifyNoInteractions(userService);
            assertThat(result.response().get("error")).isEqualTo("Admin privileges required");
        }

        @Test
        @DisplayName("Given an existing user added by an admin, when addUser is called, then BadRequest is returned.")
        public void test_addUser_WhenUserExists_ShouldReturnBadRequest() throws Exception {
            // GIVEN
            User existingUser = new User();
            when(userDtoMapper.toEntity(addUserRequestDTO)).thenReturn(existingUser);
            when(userService.addUser(any(User.class), any(User.class))).thenThrow(
                    new IllegalArgumentException("User with login '" + LOGIN + "' already exists"));

            // WHEN
            MvcResultAndAuthenticatedUser result = getMvcResultWithAuthenticatedUserSetInContext(
                    "admin",
                    UserRoleEnum.ADMIN,
                    addUserRequestDTO,
                    MockMvcResultMatchers.status().isBadRequest());

            // THEN
            verify(userDtoMapper, times(1)).toEntity(addUserRequestDTO);
            verify(userService, times(1)).addUser(existingUser, result.authenticatedUser());
            assertThat(result.response().get("error")).isNotNull();

        }

        @Test
        @DisplayName("Given an unauthenticated user, when addUser is called, then UNAUTHORIZED is returned.")
        public void test_addUser_WhenUserIsUnauthenticated_ShouldReturnUnauthorized() throws Exception {
            // GIVEN
            // Mock the mapping from DTO to entity
            User newUser = new User();

            when(userService.addUser(any(User.class), any(User.class))).thenReturn(newUser);

            // WHEN
            Map response = getMvcResultWithUnauthenticatedUser(
                    null,
                    UserRoleEnum.USER,
                    addUserRequestDTO,
                    MockMvcResultMatchers.status().isUnauthorized());

            // THEN
            verifyNoInteractions(userDtoMapper);
            verifyNoInteractions(userService);
            assertThat(response.get("error")).isEqualTo("Authentication required");
        }

        @Nested
        @Tag("addUser - Request Validation")
        @DisplayName("Tests for addUser method - Request Validation")
        class AddUserRequestValidationTests {
            @Test
            @DisplayName("Given an authenticated 'ADMIN' user, when addUser is called with empty request body, then BadRequest is returned.")
            public void test_addUser_WhenRequestIsEmpty_ShouldReturnBadRequest() throws Exception {
                // GIVEN
                // WHEN
                getMvcResultWithAuthenticatedUserSetInContext(
                        "admin",
                        UserRoleEnum.ADMIN,
                        new AddUserRequestDTO(), // empty request body
                        MockMvcResultMatchers.status().isBadRequest());

                // THEN
                verifyNoInteractions(userDtoMapper);
                verifyNoInteractions(userService);
            }

            @Test
            @DisplayName("Given an authenticated 'ADMIN' user, when addUser is called with invalid request firstName, then BadRequest is returned.")
            public void test_addUser_WhenRequestFirstNameIsInvalid_ShouldReturnBadRequest() throws Exception {
                // GIVEN
                addUserRequestDTO.setFirstName(""); // Invalid first name

                // WHEN
                getMvcResultWithAuthenticatedUserSetInContext(
                        "admin",
                        UserRoleEnum.ADMIN,
                        addUserRequestDTO,
                        MockMvcResultMatchers.status().isBadRequest());

                // THEN
                verifyNoInteractions(userDtoMapper);
                verifyNoInteractions(userService);
            }

            @Test
            @DisplayName("Given an authenticated 'ADMIN' user, when addUser is called with invalid request lastName, then BadRequest is returned.")
            public void test_addUser_WhenRequestLastNameIsInvalid_ShouldReturnBadRequest() throws Exception {
                // GIVEN
                addUserRequestDTO.setLastName(""); // Invalid last name

                // WHEN
                getMvcResultWithAuthenticatedUserSetInContext(
                        "admin",
                        UserRoleEnum.ADMIN,
                        addUserRequestDTO,
                        MockMvcResultMatchers.status().isBadRequest());

                // THEN
                verifyNoInteractions(userDtoMapper);
                verifyNoInteractions(userService);
            }

            @Test
            @DisplayName("Given an authenticated 'ADMIN' user, when addUser is called with invalid request login, then BadRequest is returned.")
            public void test_addUser_WhenRequestLoginIsInvalid_ShouldReturnBadRequest() throws Exception {
                // GIVEN
                addUserRequestDTO.setLogin(""); // Invalid login

                // WHEN
                getMvcResultWithAuthenticatedUserSetInContext(
                        "admin",
                        UserRoleEnum.ADMIN,
                        addUserRequestDTO,
                        MockMvcResultMatchers.status().isBadRequest());

                // THEN
                verifyNoInteractions(userDtoMapper);
                verifyNoInteractions(userService);
            }

            @Test
            @DisplayName("Given an authenticated 'ADMIN' user, when addUser is called with invalid request password, then BadRequest is returned.")
            public void test_addUser_WhenRequestPasswordIsInvalid_ShouldReturnBadRequest() throws Exception {
                // GIVEN
                addUserRequestDTO.setPassword(""); // Invalid password

                // WHEN
                getMvcResultWithAuthenticatedUserSetInContext(
                        "admin",
                        UserRoleEnum.ADMIN,
                        addUserRequestDTO,
                        MockMvcResultMatchers.status().isBadRequest());

                // THEN
                verifyNoInteractions(userDtoMapper);
                verifyNoInteractions(userService);
            }

            @Test
            @DisplayName("Given an authenticated 'ADMIN' user, when addUser is called with invalid request role, then BadRequest is returned.")
            public void test_addUser_WhenRequestRoleIsInvalid_ShouldReturnBadRequest() throws Exception {
                // GIVEN
                addUserRequestDTO.setRole(null); // Invalid role

                // WHEN
                getMvcResultWithAuthenticatedUserSetInContext(
                        "admin",
                        UserRoleEnum.ADMIN,
                        addUserRequestDTO,
                        MockMvcResultMatchers.status().isBadRequest());

                // THEN
                verifyNoInteractions(userDtoMapper);
                verifyNoInteractions(userService);
            }
        }

    }

    @Nested
    @Tag("readUser")
    @DisplayName("Tests for readUser method")
    class ReadUserTests {

        private List<UserBasicInfoDTO> users;

        public record MvcResultAndAuthenticatedUser(User authenticatedUser, MvcResult mvcResult) {
        }

        @BeforeEach
        public void setUp() {
            // Get the URL for readUser endpoint
            URL = URLS_BY_METHOD.get("readUser");
            // Set users details
            users = List.of(
                    new UserBasicInfoDTO(),
                    new UserBasicInfoDTO(),
                    new UserBasicInfoDTO());
            // User 1
            users.get(1).setId(1L);
            users.get(1).setFirstName(FIRST_NAME);
            users.get(1).setLastName(LAST_NAME);
            users.get(1).setRole(ROLE);
            // User 2
            users.get(0).setId(2L);
            users.get(0).setFirstName("Jane");
            users.get(0).setLastName("Smith");
            users.get(0).setRole(UserRoleEnum.USER);
            // User 3
            users.get(2).setId(3L);
            users.get(2).setFirstName("Admin");
            users.get(2).setLastName("User");
            users.get(2).setRole(UserRoleEnum.ADMIN);

        }

        @AfterEach
        public void tearDown() {
            TestSecurityContextHolder.clearContext();
        }

        private List<UserBasicInfoDTO> getResponseBodyAsUserBasicInfoDTO(MvcResult mvcResult) throws Exception {
            String responseContent = mvcResult.getResponse().getContentAsString();
            return objectMapper.readValue(
                    responseContent, new TypeReference<List<UserBasicInfoDTO>>() {
                    });
        }

        public MvcResultAndAuthenticatedUser getMvcResultWithAuthenticatedUserSetInContext(String login,
                UserRoleEnum role,
                ResultMatcher expectedStatus)
                throws Exception {
            // GIVEN
            User authenticatedUser = new User();
            authenticatedUser.setLogin(login);
            authenticatedUser.setRole(role);
            // Create an Authentication object with the authenticated user and their role
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    authenticatedUser,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));

            // Set the authentication in the security context
            SecurityContext securityContext = new SecurityContextImpl();
            securityContext.setAuthentication(auth);
            TestSecurityContextHolder.setContext(securityContext);

            // WHEN
            MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get(URL)
                    .with(securityContext(securityContext))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(expectedStatus)
                    .andExpect(result -> {
                        String contentType = result.getResponse().getContentType();
                        if (contentType == null) {
                            throw new AssertionError("Content-Type est null");
                        } else if (!(contentType.startsWith("application/json") ||
                                contentType.startsWith("application/problem+json"))) {
                            throw new AssertionError(
                                    "The content type must be 'application/json' or 'application/problem+json', but was: "
                                            + contentType);
                        }
                    })
                    .andReturn();

            return new MvcResultAndAuthenticatedUser(
                    authenticatedUser,
                    mvcResult);
        }

        public Map getMvcResultWithUnauthenticatedUser(String login,
                UserRoleEnum role,
                ResultMatcher expectedStatus)
                throws Exception {
            // WHEN
            MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(expectedStatus)
                    .andExpect(result -> {
                        String contentType = result.getResponse().getContentType();
                        if (contentType == null) {
                            throw new AssertionError("Content-Type est null");
                        } else if (!(contentType.startsWith("application/json") ||
                                contentType.startsWith("application/problem+json"))) {
                            throw new AssertionError(
                                    "The content type must be 'application/json' or 'application/problem+json', but was: "
                                            + contentType);
                        }
                    })
                    .andReturn();

            return getResponseBodyAsMap(mvcResult);
        }

        @Test
        @DisplayName("Given an authenticated 'ADMIN' user and users list is not empty, when readUser is called, then OK is returned.")
        public void test_readUser_WhenUserIsAdminAndUserListIsNotEmpty_ShouldReturnOk() throws Exception {
            // GIVEN
            when(userService.getUsers(any(User.class))).thenReturn(users);

            // WHEN
            MvcResultAndAuthenticatedUser result = getMvcResultWithAuthenticatedUserSetInContext(
                    "admin",
                    UserRoleEnum.ADMIN,
                    MockMvcResultMatchers.status().isOk());

            List<UserBasicInfoDTO> responseUsers = getResponseBodyAsUserBasicInfoDTO(result.mvcResult());

            // THEN
            verify(userService, times(1)).getUsers(result.authenticatedUser());
            assertThat(responseUsers).usingRecursiveComparison().isEqualTo(users);
        }

        @Test
        @DisplayName("Given an authenticated 'ADMIN' user and users list is empty, when readUser is called, then OK is returned.")
        public void test_readUser_WhenUserIsAdminAndUserListIsEmpty_ShouldReturnOk() throws Exception {
            // GIVEN
            List<UserBasicInfoDTO> emptyList = List.of();
            when(userService.getUsers(any(User.class))).thenReturn(emptyList);

            // WHEN
            MvcResultAndAuthenticatedUser result = getMvcResultWithAuthenticatedUserSetInContext(
                    "admin",
                    UserRoleEnum.ADMIN,
                    MockMvcResultMatchers.status().isOk());

            List<UserBasicInfoDTO> responseUsers = getResponseBodyAsUserBasicInfoDTO(result.mvcResult());

            // THEN
            verify(userService, times(1)).getUsers(result.authenticatedUser());
            assertThat(responseUsers).usingRecursiveComparison().isEqualTo(emptyList);
        }

        @Test
        @DisplayName("Given an authenticated 'USER' lambda user, when readUser is called, then FORBIDDEN is returned.")
        public void test_readUser_WhenUserIsUser_ShouldReturnForbidden() throws Exception {
            // GIVEN
            // WHEN
            MvcResultAndAuthenticatedUser result = getMvcResultWithAuthenticatedUserSetInContext(
                    "user",
                    UserRoleEnum.USER,
                    MockMvcResultMatchers.status().isForbidden());

            Map response = getResponseBodyAsMap(result.mvcResult());
            // THEN
            verifyNoInteractions(userService);
            assertThat(response.get("error")).isEqualTo("Admin privileges required");
        }

        @Test
        @DisplayName("Given an unauthenticated user, when readUser is called, then UNAUTHORIZED is returned.")
        public void test_readUser_WhenUserIsUnauthenticated_ShouldReturnUnauthorized() throws Exception {
            // GIVEN

            // WHEN
            Map response = getMvcResultWithUnauthenticatedUser(
                    null,
                    UserRoleEnum.USER,
                    MockMvcResultMatchers.status().isUnauthorized());

            // THEN
            verifyNoInteractions(userService);
            assertThat(response.get("error")).isEqualTo("Authentication required");
        }
    }

    @Nested
    @Tag("readUserById")
    @DisplayName("Tests for readUserById method")
    class ReadUserByIdTests {

        private UserSummaryDTO user;
        private static final Long USER_ID = 1L;

        public record MvcResultAndAuthenticatedUser(User authenticatedUser, MvcResult mvcResult) {
        }

        @BeforeEach
        public void setUp() {
            // Get the URL for readUserById endpoint
            URL = URLS_BY_METHOD.get("readUserById");
            // Set user details
            user = new UserSummaryDTO();
            user.setId(USER_ID);
            user.setFirstName(FIRST_NAME);
            user.setLastName(LAST_NAME);
            user.setLogin(LOGIN);
            user.setRole(ROLE);
            user.setCreated_at(LocalDateTime.now());
            user.setUpdated_at(LocalDateTime.now());

        }

        @AfterEach
        public void tearDown() {
            TestSecurityContextHolder.clearContext();
        }

        private UserSummaryDTO getResponseBodyAsUserSummaryDTO(MvcResult mvcResult) throws Exception {
            String responseContent = mvcResult.getResponse().getContentAsString();
            if (responseContent == null || responseContent.isEmpty()) {
                return null;
            }
            return objectMapper.readValue(
                    responseContent, new TypeReference<UserSummaryDTO>() {
                    });
        }

        public MvcResultAndAuthenticatedUser getMvcResultWithAuthenticatedUserSetInContext(String login,
                UserRoleEnum role,
                Long userId,
                ResultMatcher expectedStatus)
                throws Exception {
            // GIVEN
            User authenticatedUser = new User();
            authenticatedUser.setLogin(login);
            authenticatedUser.setRole(role);
            // Create an Authentication object with the authenticated user and their role
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    authenticatedUser,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));

            // Set the authentication in the security context
            SecurityContext securityContext = new SecurityContextImpl();
            securityContext.setAuthentication(auth);
            TestSecurityContextHolder.setContext(securityContext);

            // WHEN
            MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get(URL, userId)
                    .with(securityContext(securityContext))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(expectedStatus)
                    .andReturn();

            return new MvcResultAndAuthenticatedUser(
                    authenticatedUser,
                    mvcResult);
        }

        public Map getMvcResultWithUnauthenticatedUser(String login,
                UserRoleEnum role,
                Long userId,
                ResultMatcher expectedStatus)
                throws Exception {
            // WHEN
            MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get(URL, userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(expectedStatus)
                    .andExpect(result -> {
                        String contentType = result.getResponse().getContentType();
                        if (contentType == null) {
                            throw new AssertionError("Content-Type est null");
                        } else if (!(contentType.startsWith("application/json") ||
                                contentType.startsWith("application/problem+json"))) {
                            throw new AssertionError(
                                    "The content type must be 'application/json' or 'application/problem+json', but was: "
                                            + contentType);
                        }
                    })
                    .andReturn();

            return getResponseBodyAsMap(mvcResult);
        }

        @Test
        @DisplayName("Given an authenticated 'ADMIN' user and returned user exists, when readUser is called, then OK is returned.")
        public void test_readUserById_WhenUserIsAdminAndReturnedUserExists_ShouldReturnOk() throws Exception {
            // GIVEN
            when(userService.getUserById(any(User.class), eq(USER_ID))).thenReturn(user);
            System.out.println("\n\n\n******Mocked user: " + user);
            // WHEN
            MvcResultAndAuthenticatedUser result = getMvcResultWithAuthenticatedUserSetInContext(
                    "admin",
                    UserRoleEnum.ADMIN,
                    USER_ID,
                    MockMvcResultMatchers.status().isOk());

            UserSummaryDTO responseUser = getResponseBodyAsUserSummaryDTO(result.mvcResult());

            // THEN
            verify(userService, times(1)).getUserById(result.authenticatedUser(), USER_ID);
            assertThat(responseUser).isEqualTo(user);
        }

        @Test
        @DisplayName("Given an authenticated 'ADMIN' user and returned user does not exist, when readUser is called, then OK is returned.")
        public void test_readUserById_WhenUserIsAdminAndReturnedUserDoesNotExist_ShouldReturnOk() throws Exception {
            // GIVEN
            when(userService.getUserById(any(User.class), eq(USER_ID))).thenReturn(null);

            // WHEN
            MvcResultAndAuthenticatedUser result = getMvcResultWithAuthenticatedUserSetInContext(
                    "admin",
                    UserRoleEnum.ADMIN,
                    USER_ID,
                    MockMvcResultMatchers.status().isOk());

            UserSummaryDTO responseUser = getResponseBodyAsUserSummaryDTO(result.mvcResult());

            // THEN
            verify(userService, times(1)).getUserById(result.authenticatedUser(), USER_ID);
            assertThat(responseUser).isNull();
        }

        @Test
        @DisplayName("Given an authenticated 'USER' lambda user, when readUserById is called, then FORBIDDEN is returned.")
        public void test_readUserById_WhenUserIsUser_ShouldReturnForbidden() throws Exception {
            // GIVEN
            // WHEN
            MvcResultAndAuthenticatedUser result = getMvcResultWithAuthenticatedUserSetInContext(
                    "user",
                    UserRoleEnum.USER,
                    USER_ID,
                    MockMvcResultMatchers.status().isForbidden());

            Map response = getResponseBodyAsMap(result.mvcResult());
            // THEN
            verifyNoInteractions(userService);
            assertThat(response.get("error")).isEqualTo("Admin privileges required");
        }

        @Test
        @DisplayName("Given an unauthenticated user, when readUserById is called, then UNAUTHORIZED is returned.")
        public void test_readUserById_WhenUserIsUnauthenticated_ShouldReturnUnauthorized() throws Exception {
            // GIVEN

            // WHEN
            Map response = getMvcResultWithUnauthenticatedUser(
                    null,
                    UserRoleEnum.USER,
                    USER_ID,
                    MockMvcResultMatchers.status().isUnauthorized());

            // THEN
            verifyNoInteractions(userService);
            assertThat(response.get("error")).isEqualTo("Authentication required");
        }
    }

    @Nested
    @Tag("deleteUserById")
    @DisplayName("Tests for deleteUserById method")
    class DeleteUserByIdTests {

        private static final Long USER_ID = 1L;

        public record MvcResultAndAuthenticatedUser(User authenticatedUser, Map response) {
        }

        @BeforeEach
        public void setUp() {
            // Get the URL for deleteUserById endpoint
            URL = URLS_BY_METHOD.get("deleteUserById");
        }

        @AfterEach
        public void tearDown() {
            TestSecurityContextHolder.clearContext();
        }

        public MvcResultAndAuthenticatedUser getMvcResultWithAuthenticatedUserSetInContext(String login,
                UserRoleEnum role,
                Long userId,
                ResultMatcher expectedStatus)
                throws Exception {
            // GIVEN
            User authenticatedUser = new User();
            authenticatedUser.setLogin(login);
            authenticatedUser.setRole(role);
            // Create an Authentication object with the authenticated user and their role
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    authenticatedUser,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));

            // Set the authentication in the security context
            SecurityContext securityContext = new SecurityContextImpl();
            securityContext.setAuthentication(auth);
            TestSecurityContextHolder.setContext(securityContext);

            // WHEN
            MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.delete(URL, userId)
                    .with(securityContext(securityContext))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(expectedStatus)
                    .andReturn();

            return new MvcResultAndAuthenticatedUser(
                    authenticatedUser,
                    getResponseBodyAsMap(mvcResult));
        }

        public Map getMvcResultWithUnauthenticatedUser(String login,
                UserRoleEnum role,
                Long userId,
                ResultMatcher expectedStatus)
                throws Exception {
            // WHEN
            MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.delete(URL, userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(expectedStatus)
                    .andExpect(result -> {
                        String contentType = result.getResponse().getContentType();
                        if (contentType == null) {
                            throw new AssertionError("Content-Type est null");
                        } else if (!(contentType.startsWith("application/json") ||
                                contentType.startsWith("application/problem+json"))) {
                            throw new AssertionError(
                                    "The content type must be 'application/json' or 'application/problem+json', but was: "
                                            + contentType);
                        }
                    })
                    .andReturn();

            return getResponseBodyAsMap(mvcResult);
        }

        @Test
        @DisplayName("Given an authenticated 'ADMIN' user and deleted user exists, when deleteUser is called, then OK is returned.")
        public void test_deleteUser_WhenUserIsAdminAndDeletedUserExists_ShouldReturnOk() throws Exception {
            // GIVEN
            // WHEN
            MvcResultAndAuthenticatedUser result = getMvcResultWithAuthenticatedUserSetInContext(
                    "admin",
                    UserRoleEnum.ADMIN,
                    USER_ID,
                    MockMvcResultMatchers.status().isOk());

            // THEN
            verify(userService, times(1)).deleteUser(result.authenticatedUser(), USER_ID);
            assertThat((String) result.response().get("message")).contains("deleted", "successfully");
        }

        @Test
        @DisplayName("Given an authenticated 'ADMIN' user and deleted user does not exist, when deleteUser is called, then BadRequest is returned.")
        public void test_deleteUser_WhenUserIsAdminAndDeletedUserDoesNotExist_ShouldReturnBadRequest()
                throws Exception {
            // GIVEN
            doThrow(new IllegalStateException()).when(userService).deleteUser(any(User.class), eq(USER_ID));

            // WHEN
            MvcResultAndAuthenticatedUser result = getMvcResultWithAuthenticatedUserSetInContext(
                    "admin",
                    UserRoleEnum.ADMIN,
                    USER_ID,
                    MockMvcResultMatchers.status().isBadRequest());

            // THEN
            verify(userService, times(1)).deleteUser(result.authenticatedUser(), USER_ID);

            // assertThat(responseUser).isNull();
        }

        @Test
        @DisplayName("Given an authenticated 'USER' lambda user, when deleteUser is called, then FORBIDDEN is returned.")
        public void test_deleteUser_WhenUserIsUser_ShouldReturnForbidden() throws Exception {
            // GIVEN
            // WHEN
            MvcResultAndAuthenticatedUser result = getMvcResultWithAuthenticatedUserSetInContext(
                    "user",
                    UserRoleEnum.USER,
                    USER_ID,
                    MockMvcResultMatchers.status().isForbidden());

            // THEN
            verifyNoInteractions(userService);
            assertThat(result.response().get("error")).isEqualTo("Admin privileges required");
        }

        @Test
        @DisplayName("Given an unauthenticated user, when deleteUser is called, then UNAUTHORIZED is returned.")
        public void test_deleteUser_WhenUserIsUnauthenticated_ShouldReturnUnauthorized() throws Exception {
            // GIVEN

            // WHEN
            Map response = getMvcResultWithUnauthenticatedUser(
                    null,
                    UserRoleEnum.USER,
                    USER_ID,
                    MockMvcResultMatchers.status().isUnauthorized());

            // THEN
            verifyNoInteractions(userService);
            assertThat(response.get("error")).isEqualTo("Authentication required");
        }
    }

    @Nested
    @Tag("updateUser")
    @DisplayName("Tests for updateUser method")
    class UpdateUserTests {

        private static final Long USER_ID = 1L;
        private UpdateRequestDTO updateRequestDTO;

        public record MvcResultAndAuthenticatedUser(User authenticatedUser, Map response) {
        }

        @BeforeEach
        public void setUp() {
            // Get the URL for addUser endpoint
            URL = URLS_BY_METHOD.get("updateUser");
            // Set new user details
            updateRequestDTO = new UpdateRequestDTO();
            updateRequestDTO.setLogin(LOGIN);
            updateRequestDTO.setPassword(PASSWORD);
            updateRequestDTO.setFirstName(FIRST_NAME);
            updateRequestDTO.setLastName(LAST_NAME);
            updateRequestDTO.setRole(ROLE);
        }

        @AfterEach
        public void tearDown() {
            TestSecurityContextHolder.clearContext();
        }

        public MvcResultAndAuthenticatedUser getMvcResultWithAuthenticatedUserSetInContext(String login,
                UserRoleEnum role,
                UpdateRequestDTO updateRequestDTO,
                Long userId,
                ResultMatcher expectedStatus)
                throws Exception {
            // GIVEN
            User authenticatedUser = new User();
            authenticatedUser.setLogin(login);
            authenticatedUser.setRole(role);
            // Create an Authentication object with the authenticated user and their role
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    authenticatedUser,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));

            // Set the authentication in the security context
            SecurityContext securityContext = new SecurityContextImpl();
            securityContext.setAuthentication(auth);
            TestSecurityContextHolder.setContext(securityContext);

            // WHEN
            MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.put(URL, userId)
                    .with(securityContext(securityContext))
                    .content(objectMapper.writeValueAsString(updateRequestDTO))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(expectedStatus)
                    .andExpect(result -> {
                        String contentType = result.getResponse().getContentType();
                        if (contentType == null) {
                            throw new AssertionError("Content-Type est null");
                        } else if (!(contentType.startsWith("application/json") ||
                                contentType.startsWith("application/problem+json"))) {
                            throw new AssertionError(
                                    "The content type must be 'application/json' or 'application/problem+json', but was: "
                                            + contentType);
                        }
                    })
                    .andReturn();

            return new MvcResultAndAuthenticatedUser(authenticatedUser, getResponseBodyAsMap(mvcResult));
        }

        public Map getMvcResultWithUnauthenticatedUser(String login,
                UserRoleEnum role,
                UpdateRequestDTO updateRequestDTO,
                Long userId,
                ResultMatcher expectedStatus)
                throws Exception {
            // WHEN
            MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.put(URL, userId)
                    .content(objectMapper.writeValueAsString(updateRequestDTO))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(expectedStatus)
                    .andExpect(result -> {
                        String contentType = result.getResponse().getContentType();
                        if (contentType == null) {
                            throw new AssertionError("Content-Type est null");
                        } else if (!(contentType.startsWith("application/json") ||
                                contentType.startsWith("application/problem+json"))) {
                            throw new AssertionError(
                                    "The content type must be 'application/json' or 'application/problem+json', but was: "
                                            + contentType);
                        }
                    })
                    .andReturn();

            return getResponseBodyAsMap(mvcResult);
        }

        @Test
        @DisplayName("Given an authenticated 'ADMIN' user, when updateUser is called, then Ok is returned.")
        public void test_updateUser_WhenUserIsAdmin_ShouldReturnOk() throws Exception {
            // GIVEN
            // Mock the mapping from DTO to entity
            User updatedUser = new User();
            updatedUser.setId(USER_ID);
            updatedUser.setLogin(LOGIN);
            updatedUser.setPassword(PASSWORD);
            updatedUser.setFirstName(FIRST_NAME);
            updatedUser.setLastName(LAST_NAME);
            updatedUser.setRole(ROLE);

            when(userDtoMapper.toEntity(updateRequestDTO)).thenReturn(updatedUser);

            // WHEN
            MvcResultAndAuthenticatedUser result = getMvcResultWithAuthenticatedUserSetInContext(
                    "admin",
                    UserRoleEnum.ADMIN,
                    updateRequestDTO,
                    USER_ID,
                    MockMvcResultMatchers.status().isOk());

            // THEN
            verify(userDtoMapper, times(1)).toEntity(updateRequestDTO);
            verify(userService, times(1)).updateUser(result.authenticatedUser(), USER_ID, updatedUser);
            assertThat((String) result.response().get("message")).contains("updated", "successfully");
            assertThat(result.response()).doesNotContainKeys("password", "passwordHash", "passwordSalt");
            assertThat(result.response()).doesNotContainValue(PASSWORD);

        }

        @Test
        @DisplayName("Given an authenticated 'USER' lambda user, when updateUser is called, then FORBIDDEN is returned.")
        public void test_updateUser_WhenUserIsUser_ShouldReturnForbidden() throws Exception {
            // GIVEN
            // WHEN
            MvcResultAndAuthenticatedUser result = getMvcResultWithAuthenticatedUserSetInContext(
                    "user",
                    UserRoleEnum.USER,
                    updateRequestDTO,
                    USER_ID,
                    MockMvcResultMatchers.status().isForbidden());

            // THEN
            verifyNoInteractions(userDtoMapper);
            verifyNoInteractions(userService);
            assertThat(result.response().get("error")).isEqualTo("Admin privileges required");
        }

        @Test
        @DisplayName("Given a nonexistent user updated by an admin, when updateUser is called, then BadRequest is returned.")
        public void test_updateUser_WhenUserDoesNotExist_ShouldReturnBadRequest() throws Exception {
            // GIVEN
            User nonexistentUser = new User();
            when(userDtoMapper.toEntity(updateRequestDTO)).thenReturn(nonexistentUser);
            doThrow(new IllegalArgumentException()).when(userService).updateUser(any(User.class), eq(USER_ID),
                    any(User.class));

            // WHEN
            MvcResultAndAuthenticatedUser result = getMvcResultWithAuthenticatedUserSetInContext(
                    "admin",
                    UserRoleEnum.ADMIN,
                    updateRequestDTO,
                    USER_ID,
                    MockMvcResultMatchers.status().isBadRequest());

            // THEN
            System.out.println("\n\n\n******Response: " + result.response());
            verify(userDtoMapper, times(1)).toEntity(updateRequestDTO);
            verify(userService, times(1)).updateUser(result.authenticatedUser(), USER_ID, nonexistentUser);
        }

        @Test
        @DisplayName("Given an unauthenticated user, when addUser is called, then UNAUTHORIZED is returned.")
        public void test_addUser_WhenUserIsUnauthenticated_ShouldReturnUnauthorized() throws Exception {
            // GIVEN
            // WHEN
            Map response = getMvcResultWithUnauthenticatedUser(
                    null,
                    UserRoleEnum.USER,
                    updateRequestDTO,
                    USER_ID,
                    MockMvcResultMatchers.status().isUnauthorized());

            // THEN
            verifyNoInteractions(userDtoMapper);
            verifyNoInteractions(userService);
            assertThat(response.get("error")).isEqualTo("Authentication required");
        }

        @Nested
        @Tag("updateUser - Request Validation")
        @DisplayName("Tests for updateUser method - Request Validation")
        class UpdateUserRequestValidationTests {
            @Test
            @DisplayName("Given an authenticated 'ADMIN' user, when updateUser is called with empty request body, then BadRequest is returned.")
            public void test_updateUser_WhenRequestIsEmpty_ShouldReturnBadRequest() throws Exception {
                // GIVEN
                // WHEN
                getMvcResultWithAuthenticatedUserSetInContext(
                        "admin",
                        UserRoleEnum.ADMIN,
                        new UpdateRequestDTO(), // empty request body
                        USER_ID,
                        MockMvcResultMatchers.status().isBadRequest());

                // THEN
                verifyNoInteractions(userDtoMapper);
                verifyNoInteractions(userService);
            }

            @Test
            @DisplayName("Given an authenticated 'ADMIN' user, when updateUser is called with invalid request role, then BadRequest is returned.")
            public void test_updateUser_WhenRequestRoleIsInvalid_ShouldReturnBadRequest() throws Exception {
                // GIVEN
                updateRequestDTO.setRole(null); // Invalid role

                // WHEN
                getMvcResultWithAuthenticatedUserSetInContext(
                        "admin",
                        UserRoleEnum.ADMIN,
                        updateRequestDTO,
                        USER_ID,
                        MockMvcResultMatchers.status().isBadRequest());

                // THEN
                verifyNoInteractions(userDtoMapper);
                verifyNoInteractions(userService);
            }
        }

    }

}
