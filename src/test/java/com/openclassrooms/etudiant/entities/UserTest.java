package com.openclassrooms.etudiant.entities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("UserTest")
@DisplayName("Tests for User entity")
class UserTest {

    @Nested
    @Tag("userDetailsContract")
    @DisplayName("Tests for Spring Security UserDetails contract")
    class UserDetailsContractTests {

        @Test
        @DisplayName("Given a user, when UserDetails methods are called, then contract values are consistent.")
        void test_userDetailsContract_returnsExpectedValues() {
            // GIVEN
            User user = new User();
            user.setLogin("john.login");

            // WHEN / THEN
            assertEquals("john.login", user.getUsername());
            assertTrue(user.getAuthorities().isEmpty());
            assertTrue(user.isAccountNonExpired());
            assertTrue(user.isAccountNonLocked());
            assertTrue(user.isCredentialsNonExpired());
            assertTrue(user.isEnabled());
        }
    }

    @Nested
    @Tag("defaultValues")
    @DisplayName("Tests for entity default values")
    class DefaultValuesTests {

        @Test
        @DisplayName("Given a new user, when role is not explicitly set, then default role is USER.")
        void test_newUser_defaultRoleIsUser() {
            // GIVEN
            User user = new User();

            // THEN
            assertEquals(UserRoleEnum.USER, user.getRole());
        }
    }
}
