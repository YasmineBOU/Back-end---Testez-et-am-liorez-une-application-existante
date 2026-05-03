package com.openclassrooms.etudiant.repository;

import com.openclassrooms.etudiant.dto.UserBasicInfoDTO;
import com.openclassrooms.etudiant.dto.UserSummaryDTO;
import com.openclassrooms.etudiant.entities.User;
import com.openclassrooms.etudiant.entities.UserRoleEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:etudiant_testdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;NON_KEYWORDS=USER",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.globally_quoted_identifiers=true",
        "spring.jpa.show-sql=false"
})
@Tag("UserRepositoryTest")
@DisplayName("Tests for UserRepository")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User createUser(String firstName, String lastName, String login, UserRoleEnum role) {
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setLogin(login);
        user.setPassword("encoded-password");
        user.setRole(role);
        return user;
    }

    @Nested
    @Tag("findByLogin")
    @DisplayName("Tests for findByLogin method")
    class FindByLoginTests {

        @Test
        @DisplayName("Given an existing login, when findByLogin is called, then the matching user is returned.")
        void test_findByLogin_returnsMatchingUser() {
            // GIVEN
            User savedUser = userRepository.saveAndFlush(createUser("Alice", "Martin", "alice", UserRoleEnum.USER));

            // WHEN
            Optional<User> foundUser = userRepository.findByLogin("alice");

            // THEN
            assertThat(foundUser).isPresent();
            assertThat(foundUser.get().getId()).isEqualTo(savedUser.getId());
            assertThat(foundUser.get().getLogin()).isEqualTo("alice");
            assertThat(foundUser.get().getRole()).isEqualTo(UserRoleEnum.USER);
        }
    }

    @Nested
    @Tag("findAllUserBasicInfo")
    @DisplayName("Tests for findAllUserBasicInfo method")
    class FindAllUserBasicInfoTests {

        @Test
        @DisplayName("Given persisted users, when findAllUserBasicInfo is called, then projected basic info is returned.")
        void test_findAllUserBasicInfo_returnsProjectedUsers() {
            // GIVEN
            userRepository.saveAndFlush(createUser("Alice", "Martin", "alice", UserRoleEnum.USER));
            userRepository.saveAndFlush(createUser("Bob", "Dupont", "bob", UserRoleEnum.ADMIN));

            // WHEN
            List<UserBasicInfoDTO> result = userRepository.findAllUserBasicInfo();

            // THEN
            assertThat(result)
                    .hasSize(2)
                    .extracting(UserBasicInfoDTO::getFirstName, UserBasicInfoDTO::getLastName,
                            UserBasicInfoDTO::getRole)
                    .containsExactlyInAnyOrder(
                            tuple("Alice", "Martin", UserRoleEnum.USER),
                            tuple("Bob", "Dupont", UserRoleEnum.ADMIN));
        }
    }

    @Nested
    @Tag("findUserById")
    @DisplayName("Tests for findUserById method")
    class FindUserByIdTests {

        @Test
        @DisplayName("Given an existing user id, when findUserById is called, then the summary projection is returned.")
        void test_findUserById_returnsUserSummaryProjection() {
            // GIVEN
            User savedUser = userRepository.saveAndFlush(createUser("Carol", "White", "carol", UserRoleEnum.ADMIN));

            // WHEN
            UserSummaryDTO result = userRepository.findUserById(savedUser.getId());

            // THEN
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(savedUser.getId());
            assertThat(result.getLogin()).isEqualTo("carol");
            assertThat(result.getRole()).isEqualTo(UserRoleEnum.ADMIN);
            assertThat(result.getCreated_at()).isNotNull();
            assertThat(result.getUpdated_at()).isNotNull();
        }
    }
}
