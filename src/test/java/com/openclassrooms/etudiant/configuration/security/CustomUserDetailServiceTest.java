package com.openclassrooms.etudiant.configuration.security;

import com.openclassrooms.etudiant.entities.User;
import com.openclassrooms.etudiant.entities.UserRoleEnum;
import com.openclassrooms.etudiant.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("CustomUserDetailServiceTest")
@DisplayName("Tests for CustomUserDetailService")
class CustomUserDetailServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailService customUserDetailService;

    @Nested
    @Tag("loadUserByUsername")
    @DisplayName("Tests for loadUserByUsername method")
    class LoadUserByUsernameTests {

        @Test
        @DisplayName("Given an existing login, when loadUserByUsername is called, then matching user details are returned.")
        void test_loadUserByUsername_withExistingLogin_returnsUserDetails() {
            // GIVEN
            String login = "existingUser";
            User user = new User();
            user.setLogin(login);
            user.setPassword("encoded-password");
            user.setRole(UserRoleEnum.ADMIN);

            when(userRepository.findByLogin(login)).thenReturn(Optional.of(user));

            // WHEN
            UserDetails result = customUserDetailService.loadUserByUsername(login);

            // THEN
            assertSame(user, result);
            assertEquals(login, result.getUsername());
            verify(userRepository, times(1)).findByLogin(login);
        }

        @Test
        @DisplayName("Given an unknown login, when loadUserByUsername is called, then UsernameNotFoundException is thrown.")
        void test_loadUserByUsername_withUnknownLogin_throwsUsernameNotFoundException() {
            // GIVEN
            String login = "missingUser";
            when(userRepository.findByLogin(login)).thenReturn(Optional.empty());

            // WHEN
            UsernameNotFoundException exception = assertThrows(
                    UsernameNotFoundException.class,
                    () -> customUserDetailService.loadUserByUsername(login));

            // THEN
            assertEquals("User Not Found with username: " + login, exception.getMessage());
            verify(userRepository, times(1)).findByLogin(login);
        }
    }
}
