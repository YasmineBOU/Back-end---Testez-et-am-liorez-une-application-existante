package com.openclassrooms.etudiant.service;

import com.openclassrooms.etudiant.dto.UpdateRequestDTO;
import com.openclassrooms.etudiant.dto.UserBasicInfoDTO;
import com.openclassrooms.etudiant.dto.UserSummaryDTO;
import com.openclassrooms.etudiant.entities.User;
import com.openclassrooms.etudiant.entities.UserRoleEnum;
import com.openclassrooms.etudiant.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@Tag("UserServiceTest")
@DisplayName("Tests for UserService")
public class UserServiceTest {
    private static final String FIRST_NAME = "John";
    private static final String LAST_NAME = "Doe";
    private static final String LOGIN = "LOGIN";
    private static final String PASSWORD = "PASSWORD";
    private static final UserRoleEnum ROLE = UserRoleEnum.USER;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @InjectMocks
    private UserService userService;

    private static Stream<Arguments> provideInvalidInputs() {
        return Stream.of(
                // authenticatedUser = null, id = any
                Arguments.of(null, 1L),

                // authenticatedUser = any, id = null ou <= 0
                Arguments.of(new User(), null),
                Arguments.of(new User(), 0L),
                Arguments.of(new User(), -1L));
    }

    private static Stream<Arguments> updateUser_provideInvalidInputs() {

        return Stream.of(
                // authenticatedUser = null, userToUpdate = any, id = any
                Arguments.of(null, new User(), 1L),

                // authenticatedUser = any, userToUpdate = null, id = any
                Arguments.of(new User(), null, 1L),

                // authenticatedUser = any, userToUpdate = any, id = null ou <= 0
                Arguments.of(new User(), new User(), null),
                Arguments.of(new User(), new User(), 0L),
                Arguments.of(new User(), new User(), -1L));
    }

    // Register tests
    @Nested
    @Tag("register")
    @DisplayName("Tests for register method")
    class RegisterTests {

        @Test
        @DisplayName("Given a null user, when register is called, then IllegalArgumentException is thrown.")
        public void test_create_null_user_throws_IllegalArgumentException() {
            // GIVEN

            // THEN
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> userService.register(null));
        }

        @Test
        @DisplayName("Given an existing user, when register is called, then IllegalArgumentException is thrown.")
        public void test_create_already_exist_user_throws_IllegalArgumentException() {
            // GIVEN
            User user = new User();
            user.setFirstName(FIRST_NAME);
            user.setLastName(LAST_NAME);
            user.setLogin(LOGIN);
            user.setPassword(PASSWORD);
            when(userRepository.findByLogin(any())).thenReturn(Optional.of(user));

            // THEN
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> userService.register(user));

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Given a valid user, when register is called, then the user is saved.")
        public void test_create_user() {
            // GIVEN
            User user = new User();
            user.setFirstName(FIRST_NAME);
            user.setLastName(LAST_NAME);
            user.setLogin(LOGIN);
            user.setPassword(PASSWORD);
            when(passwordEncoder.encode(PASSWORD)).thenReturn("ENCODED_PASSWORD");
            when(userRepository.findByLogin(LOGIN)).thenReturn(Optional.empty());

            // WHEN
            userService.register(user);

            // THEN
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            verify(passwordEncoder, times(1)).encode(PASSWORD);
            assertThat(userCaptor.getValue()).isSameAs(user);
            assertThat(userCaptor.getValue().getPassword()).isEqualTo("ENCODED_PASSWORD");
        }
    }

    // Login tests
    @Nested
    @Tag("login")
    @DisplayName("Tests for login method")
    class LoginTests {
        @Test
        @DisplayName("Given a null login, when login is called, then IllegalArgumentException is thrown.")
        public void test_login_with_null_login_throws_IllegalArgumentException() {
            // THEN
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> userService.login(null, PASSWORD));
        }

        @Test
        @DisplayName("Given invalid credentials, when login is called, then BadCredentialsException is thrown.")
        public void test_login_with_invalid_credentials_throws_BadCredentialsException() {
            // GIVEN
            User user = new User();
            user.setFirstName(FIRST_NAME);
            user.setLastName(LAST_NAME);
            user.setLogin(LOGIN);
            user.setPassword(PASSWORD);
            when(userRepository.findByLogin(LOGIN)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("WRONG_PASSWORD",
                    user.getPassword())).thenReturn(false);

            // THEN
            Assertions.assertThrows(
                    BadCredentialsException.class,
                    () -> userService.login(LOGIN, "WRONG_PASSWORD"));

            verify(jwtService, never()).generateToken(any());
        }

        @Test
        @DisplayName("Given a user that is not found, when login is called, then BadCredentialsException is thrown.")
        public void test_login_with_user_not_found_throws_BadCredentialsException() {
            // GIVEN
            when(userRepository.findByLogin(LOGIN)).thenReturn(Optional.empty());

            // THEN
            Assertions.assertThrows(
                    BadCredentialsException.class,
                    () -> userService.login(LOGIN, PASSWORD));

            verify(jwtService, never()).generateToken(any());
        }

        @Test
        @DisplayName("Given valid credentials, when login is called, then a JWT token is returned.")
        public void test_login_with_valid_credentials_returns_jwt_token() {
            // GIVEN
            User user = new User();
            user.setFirstName(FIRST_NAME);
            user.setLastName(LAST_NAME);
            user.setLogin(LOGIN);
            user.setPassword(PASSWORD);
            when(userRepository.findByLogin(LOGIN)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(PASSWORD, user.getPassword())).thenReturn(true);
            when(jwtService.generateToken(any())).thenReturn("mocked-jwt-token");

            // WHEN
            String token = userService.login(LOGIN, PASSWORD);

            // THEN
            verify(userRepository, times(1)).findByLogin(LOGIN);
            verify(jwtService, times(1)).generateToken(any());
            assertThat(token).isEqualTo("mocked-jwt-token");
        }
    }

    // Add user tests
    @Nested
    @Tag("addUser")
    @DisplayName("Tests for addUser method")
    class AddUserTests {
        @Test
        @DisplayName("Given a null user, when addUser is called, then IllegalArgumentException is thrown.")
        public void test_addUser_with_null_user_throws_IllegalArgumentException() {
            // THEN
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> userService.addUser(null, new User()));
        }

        @Test
        @DisplayName("Given a null authenticated user, when addUser is called, then IllegalArgumentException is thrown.")
        public void test_addUser_with_null_authenticated_user_throws_IllegalArgumentException() {
            // THEN
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> userService.addUser(new User(), null));
        }

        @Test
        @DisplayName("Given an existing user, when addUser is called, then IllegalArgumentException is thrown.")
        public void test_addUser_with_existing_user_throws_IllegalArgumentException() {
            // GIVEN
            User newUser = new User();
            User existingUser = new User();
            newUser.setLogin(LOGIN);

            when(userRepository.findByLogin(LOGIN)).thenReturn(Optional.of(existingUser));

            // THEN
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> userService.addUser(newUser, new User()));

            verify(userRepository, times(1)).findByLogin(LOGIN);
            verify(userRepository, never()).save(newUser);
        }

        @Test
        @DisplayName("Given a null password, when addUser is called, then IllegalArgumentException is thrown.")
        public void test_addUser_with_null_password_throws_IllegalArgumentException() {
            // GIVEN
            User newUser = new User();
            newUser.setPassword(null); // Explicitly set to null for clarity

            // THEN
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> userService.addUser(newUser, new User()));

            verify(userRepository, never()).save(newUser);
        }

        @Test
        @DisplayName("Given valid user and authenticated user, when addUser is called, then the user is saved.")
        public void test_addUser_with_valid_user_and_authenticated_user_saves_user() {
            // GIVEN
            User newUser = new User();
            newUser.setFirstName(FIRST_NAME);
            newUser.setLastName(LAST_NAME);
            newUser.setLogin(LOGIN);
            newUser.setPassword(PASSWORD);
            newUser.setRole(ROLE);
            when(passwordEncoder.encode(PASSWORD)).thenReturn("ENCODED_PASSWORD");
            when(userRepository.findByLogin(LOGIN)).thenReturn(Optional.empty());
            when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            // WHEN
            userService.addUser(newUser, new User());

            // THEN
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository, times(1)).findByLogin(LOGIN);
            verify(userRepository, times(1)).save(userCaptor.capture());
            verify(passwordEncoder, times(1)).encode(PASSWORD);
            assertThat(userCaptor.getValue().getPassword()).isEqualTo("ENCODED_PASSWORD");
            assertThat(userCaptor.getValue().getRole()).isEqualTo(ROLE);
        }

    }

    // Get User tests
    @Nested
    @Tag("getUsers")
    @DisplayName("Tests for getUsers method")
    class GetUsersTests {

        @Test
        @DisplayName("Given a null authenticated user, when getUsers is called, then IllegalArgumentException is thrown.")
        public void test_getUsers_with_null_authenticated_user_throws_IllegalArgumentException() {
            // THEN
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> userService.getUsers(null));
        }

        @Test
        @DisplayName("Given a valid authenticated user, when getUsers is called, then users are retrieved.")
        public void test_getUsers_with_valid_authenticated_user_retrieves_users() {
            // GIVEN
            ArrayList<UserBasicInfoDTO> expectedUserList = new ArrayList<>();
            when(userRepository.findAllUserBasicInfo()).thenReturn(expectedUserList);

            // WHEN
            Iterable<UserBasicInfoDTO> fetchedUsers = userService.getUsers(new User());

            // THEN
            verify(userRepository, times(1)).findAllUserBasicInfo();
            assertThat(fetchedUsers).isSameAs(expectedUserList);
        }

    }

    // Get User by id tests
    @Nested
    @Tag("getUserById")
    @DisplayName("Tests for getUserById method")
    class GetUserByIdTests {

        @ParameterizedTest()
        @MethodSource("com.openclassrooms.etudiant.service.UserServiceTest#provideInvalidInputs")
        @DisplayName("Given an invalid authenticated user and/or id, when getUserById is called, then IllegalArgumentException is thrown.")
        public void test_getUserById_with_invalid_inputs_throws_IllegalArgumentException(User authenticatedUser,
                Long id) {
            // THEN
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> userService.getUserById(authenticatedUser, id));
        }

        @Test
        @DisplayName("Given an unexisting id, when getUserById is called, then IllegalStateException is thrown.")
        public void test_getUserById_with_unexisting_id_throws_IllegalStateException() {
            // GIVEN
            Long unexistingId = 999L;
            when(userRepository.findUserById(unexistingId)).thenThrow(new IllegalStateException());

            // THEN
            Assertions.assertThrows(
                    IllegalStateException.class,
                    () -> userService.getUserById(new User(), unexistingId));
        }

        @Test
        @DisplayName("Given a valid authenticated user and an existing id, when getUserById is called, then the user is retrieved.")
        public void test_getUserById_with_valid_authenticated_user_retrieves_user() {
            long existingId = 1L;
            UserSummaryDTO expectedUser = new UserSummaryDTO();
            expectedUser.setId(existingId);
            // GIVEN
            when(userRepository.findUserById(existingId)).thenReturn(expectedUser);

            // WHEN
            UserSummaryDTO fetchedUserSummaryDTO = userService.getUserById(new User(), existingId);

            // THEN
            verify(userRepository, times(1)).findUserById(existingId);
            assertThat(fetchedUserSummaryDTO).isSameAs(expectedUser);
        }

    }

    // delete user tests
    @Nested
    @Tag("deleteUser")
    @DisplayName("Tests for deleteUser method")
    class DeleteUserTests {

        @ParameterizedTest()
        @MethodSource("com.openclassrooms.etudiant.service.UserServiceTest#provideInvalidInputs")
        @DisplayName("Given an invalid authenticated user and/or id, when deleteUser is called, then IllegalArgumentException is thrown.")
        public void test_deleteUser_with_invalid_id_throws_IllegalArgumentException(User authenticatedUser, Long id) {
            // THEN
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> userService.deleteUser(authenticatedUser, id));
        }

        @Test
        @DisplayName("Given an unexisting id, when deleteUser is called, then IllegalStateException is thrown.")
        public void test_deleteUser_with_unexisting_id_throws_IllegalStateException() {
            // GIVEN
            Long unexistingId = 999L;
            when(userRepository.existsById(unexistingId)).thenReturn(false);

            // THEN
            Assertions.assertThrows(
                    IllegalStateException.class,
                    () -> userService.deleteUser(new User(), unexistingId));

            verify(userRepository, times(1)).existsById(unexistingId);
            verify(userRepository, never()).deleteById(unexistingId);
        }

        @Test
        @DisplayName("Given a valid existing id, when deleteUser is called, then the user is deleted.")
        public void test_deleteUser_with_valid_existing_id_deletes_user() {
            // GIVEN
            Long existingId = 1L;
            when(userRepository.existsById(existingId)).thenReturn(true);

            // WHEN
            userService.deleteUser(new User(), existingId);

            // THEN
            verify(userRepository, times(1)).existsById(existingId);
            verify(userRepository, times(1)).deleteById(existingId);
        }
    }

    // update user tests
    @Nested
    @Tag("updateUser")
    @DisplayName("Tests for updateUser method")
    class UpdateUserTests {

        @ParameterizedTest()
        @MethodSource("com.openclassrooms.etudiant.service.UserServiceTest#updateUser_provideInvalidInputs")
        @DisplayName("Given an invalid authenticated user and/or updateRequestDTO and/or id, when updateUser is called, then IllegalArgumentException is thrown.")
        public void test_updateUser_with_invalid_inputs_throws_IllegalArgumentException(User authenticatedUser,
                User userToUpdate,
                Long id) {
            // THEN
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> userService.updateUser(authenticatedUser, id, userToUpdate));
        }

        @Test
        @DisplayName("Given an unexisting id, when updateUser is called, then IllegalStateException is thrown.")
        public void test_updateUser_with_unexisting_id_throws_IllegalStateException() {
            // GIVEN
            Long unexistingId = 999L;
            when(userRepository.existsById(unexistingId)).thenReturn(false);

            // THEN
            Assertions.assertThrows(
                    IllegalStateException.class,
                    () -> userService.updateUser(new User(), unexistingId, new User()));
        }

        @Test
        @DisplayName("Given a valid existing id and new data, when updateUser is called, then the user is updated.")
        public void test_updateUser_with_valid_existing_id_and_new_data_updates_user() {
            // GIVEN
            long existingId = 1L;
            // Update user with new data
            User userToUpdate = new User();
            userToUpdate.setFirstName("NewFirstName");
            // Existing user data
            User existingUser = new User();
            existingUser.setFirstName(FIRST_NAME);
            existingUser.setLastName(LAST_NAME);
            existingUser.setLogin(LOGIN);
            existingUser.setPassword(PASSWORD);
            existingUser.setRole(ROLE);

            when(userRepository.findById(existingId)).thenReturn(Optional.of(existingUser));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // WHEN
            userService.updateUser(new User(), existingId, userToUpdate);

            // THEN
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository, times(1)).findById(existingId);
            verify(userRepository, times(1)).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getFirstName()).isEqualTo("NewFirstName");
            assertThat(userCaptor.getValue().getLastName()).isEqualTo(LAST_NAME);
            assertThat(userCaptor.getValue().getLogin()).isEqualTo(LOGIN);
            assertThat(userCaptor.getValue().getPassword()).isEqualTo(PASSWORD);
            assertThat(userCaptor.getValue().getRole()).isEqualTo(ROLE);
        }
    }
}
