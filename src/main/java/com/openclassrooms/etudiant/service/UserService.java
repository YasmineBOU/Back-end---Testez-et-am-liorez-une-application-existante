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
        Assert.notNull(login, "Login must not be null");
        Assert.notNull(password, "Password must not be null");
        Optional<User> user = userRepository.findByLogin(login);
        if (user.isPresent() && passwordEncoder.matches(password, user.get().getPassword())) {
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

        // Validation: Check if login already exists
        Optional<User> existingUser = userRepository.findByLogin(newUser.getLogin());
        if (existingUser.isPresent()) {
            throw new IllegalArgumentException(
                    "User with login '" + newUser.getLogin() + "' already exists");
        }

        // Validation: Ensure password is not empty
        if (newUser.getPassword() == null) {
            throw new IllegalArgumentException("Password must not be null");
        }

        // Security: Encode password
        newUser.setPassword(passwordEncoder.encode(newUser.getPassword()));

        // Persistence
        User savedUser = userRepository.save(newUser);

        return savedUser;
    }

    public Iterable<UserBasicInfoDTO> getUsers(User authenticatedUser) {
        Assert.notNull(authenticatedUser, "Authenticated user must not be null");
        Iterable<UserBasicInfoDTO> users = userRepository.findAllUserBasicInfo();

        return users;
    }

    public UserSummaryDTO getUserById(User authenticatedUser, Long id) {
        Assert.notNull(authenticatedUser, "Authenticated user must not be null");
        Assert.notNull(id, "User ID must not be null");
        Assert.isTrue(id > 0, "User ID must be a positive number");

        UserSummaryDTO user = userRepository.findUserById(id);
        if (user != null) {
            return user;
        } else {
            throw new IllegalStateException("User with id " + id + " not found in database");
        }
    }

    public void deleteUser(User authenticatedUser, Long id) {
        Assert.notNull(authenticatedUser, "Authenticated user must not be null");
        Assert.notNull(id, "User ID must not be null");
        Assert.isTrue(id > 0, "User ID must be a positive number");
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
        } else {
            throw new IllegalStateException("User with id " + id + " not found in database");
        }
    }

    public void updateUser(User authenticatedUser, Long id, User user) {
        Assert.notNull(id, "User ID must not be null");
        Assert.isTrue(id > 0, "User ID must be a positive number");
        Assert.notNull(authenticatedUser, "Authenticated user must not be null");
        Assert.notNull(user, "User update data must not be null");

        try {
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
            }
        } catch (Exception e) {
            log.error("Error during update attempt by user '{}': {}", authenticatedUser.getLogin(), e.getMessage());
            throw e; // Re-throw after logging
        }

    }
}
