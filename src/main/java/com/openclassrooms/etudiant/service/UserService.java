package com.openclassrooms.etudiant.service;

import com.openclassrooms.etudiant.dto.UpdateRequestDTO;
import com.openclassrooms.etudiant.dto.UserBasicInfoDTO;
import com.openclassrooms.etudiant.dto.UserSummaryDTO;
import com.openclassrooms.etudiant.entities.User;
import com.openclassrooms.etudiant.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Registers a new user to the database.
     * 
     * @param user the user to be registered
     * @throws IllegalArgumentException if a user with the same login already exists
     */
    public void register(User user) {
        Assert.notNull(user, "User must not be null");
        log.info("Registering new user");

        Optional<User> optionalUser = userRepository.findByLogin(user.getLogin());
        if (optionalUser.isPresent()) {
            throw new IllegalArgumentException("User with login " + user.getLogin() + " already exists");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
    }

    /**
     * Login with given credentials and return a JWT token.
     *
     * @param login    the user's login
     * @param password the user's password
     * @return a JWT token if the credentials are valid, otherwise an
     *         IllegalArgumentException is thrown
     */
    public String login(String login, String password) {
        log.info("\n\n\nLogin attempt for user: |{}| \n\n\n", login);
        Assert.notNull(login, "Login must not be null");
        Assert.notNull(password, "Password must not be null");
        log.info("Attempting login for user: {}", login);
        Optional<User> user = userRepository.findByLogin(login);
        if (user.isPresent() && passwordEncoder.matches(password, user.get().getPassword())) {
            log.info("User '{}' found and password matches\n\n", login);
            UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                    .username(user.get().getLogin())
                    .password(user.get().getPassword())
                    .roles(user.get().getRole().name())
                    .build();
            return jwtService.generateToken(userDetails);
        } else {
            throw new BadCredentialsException("Invalid credentials");
        }
    }

    public User addUser(User newUser, User authenticatedUser) {
        Assert.notNull(newUser, "New user must not be null");
        Assert.notNull(authenticatedUser, "Authenticated user must not be null");

        // Security: Log who is creating the user
        log.info("User '{}' is creating new user with login '{}'",
                authenticatedUser.getLogin(), newUser.getLogin());

        // Validation: Check if login already exists
        Optional<User> existingUser = userRepository.findByLogin(newUser.getLogin());
        if (existingUser.isPresent()) {
            log.info("Attempt to create duplicate user with login: {}", newUser.getLogin());
            throw new IllegalArgumentException(
                    "User with login '" + newUser.getLogin() + "' already exists");
        }

        // Validation: Ensure password is not empty
        if (newUser.getPassword() == null) {
            log.info("Attempt to create user with empty password");
            throw new IllegalArgumentException("Password must not be null");
        }

        // Security: Encode password
        newUser.setPassword(passwordEncoder.encode(newUser.getPassword()));

        log.info("Creating user with login: {}", newUser.getLogin());
        // Persistence
        User savedUser = userRepository.save(newUser);

        log.info("User '{}' successfully created with login '{}'",
                authenticatedUser.getLogin(), savedUser.getLogin());

        return savedUser;
    }

    public Iterable<UserBasicInfoDTO> getUsers(User authenticatedUser) {
        Assert.notNull(authenticatedUser, "Authenticated user must not be null");
        log.info("User '{}' is retrieving all users", authenticatedUser.getLogin());
        Iterable<UserBasicInfoDTO> users = userRepository.findAllUserBasicInfo();
        log.info("User '{}' retrieved all users: {} users", authenticatedUser.getLogin(),
                users.spliterator().getExactSizeIfKnown());
        return users;
    }

    public UserSummaryDTO getUserById(User authenticatedUser, Long id) {
        Assert.notNull(authenticatedUser, "Authenticated user must not be null");
        Assert.notNull(id, "User ID must not be null");
        Assert.isTrue(id > 0, "User ID must be a positive number");
        log.info("User '{}' is retrieving information for user with id: {}", authenticatedUser.getLogin(), id);
        UserSummaryDTO user = userRepository.findUserById(id);
        if (user != null) {
            log.info("User '{}' found: {}", authenticatedUser.getLogin(), user);
            return user;
        } else {
            log.info("User with id '{}' not found in database", id);
            throw new IllegalStateException("User with id " + id + " not found in database");
        }
    }

    public void deleteUser(User authenticatedUser, Long id) {
        Assert.notNull(authenticatedUser, "Authenticated user must not be null");
        Assert.notNull(id, "User ID must not be null");
        Assert.isTrue(id > 0, "User ID must be a positive number");
        log.info("User '{}' is deleting user with id: {}", authenticatedUser.getLogin(), id);
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            log.info("User with id '{}' successfully deleted by '{}'", id, authenticatedUser.getLogin());
        } else {
            log.info("Attempt to delete non-existent user with id: {}", id);
            throw new IllegalStateException("User with id " + id + " not found in database");
        }
    }

    public void updateUser(User authenticatedUser, Long id, User user) {
        Assert.notNull(id, "User ID must not be null");
        Assert.isTrue(id > 0, "User ID must be a positive number");
        Assert.notNull(authenticatedUser, "Authenticated user must not be null");
        Assert.notNull(user, "User update data must not be null");

        try {
            // Security: Log the update attempt
            log.info("User '{}' is attempting to update user with id '{}'", authenticatedUser.getLogin(), id);

            final User existingUser = userRepository.findById(id)
                    .orElseThrow(() -> new IllegalStateException("User with id " + id + " not found in database"));

            boolean hasNewData = false;

            // Update only non-null fields from the DTO
            if (user.getFirstName() != null && !user.getFirstName().trim().isEmpty()) {
                existingUser.setFirstName(user.getFirstName().trim());
                hasNewData = true;
            }
            if (user.getLastName() != null && !user.getLastName().trim().isEmpty()) {
                existingUser.setLastName(user.getLastName().trim());
                hasNewData = true;
            }
            if (user.getLogin() != null && !user.getLogin().trim().isEmpty()) {
                existingUser.setLogin(user.getLogin().trim());
                hasNewData = true;
            }
            if (user.getPassword() != null && !user.getPassword().trim().isEmpty()) {
                existingUser.setPassword(passwordEncoder.encode(user.getPassword().trim()));
                hasNewData = true;
            }

            if (user.getRole() != null) {
                existingUser.setRole(user.getRole());
                hasNewData = true;
            }

            if (hasNewData) {
                userRepository.save(existingUser);
                log.info("User with id '{}' successfully updated by '{}'", id, authenticatedUser.getLogin());
            } else {
                log.info("No new data provided for update of user with id '{}'", id);
            }
        } catch (Exception e) {
            log.error("Error during update attempt by user '{}': {}", authenticatedUser.getLogin(), e.getMessage());
            throw e; // Re-throw after logging
        }

    }
}
