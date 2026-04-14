package com.openclassrooms.etudiant.service;

import com.openclassrooms.etudiant.entities.User;
import com.openclassrooms.etudiant.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Optional;

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
     * @param login the user's login
     * @param password the user's password
     * @return a JWT token if the credentials are valid, otherwise an IllegalArgumentException is thrown
     */
    public String login(String login, String password) {
        log.info("\n\n\nLogin attempt for user: |{}| with \n\n\n", login);
        Assert.notNull(login, "Login must not be null");
        Assert.notNull(password, "Password must not be null");
        log.info("Attempting login for user: {}", login);
        Optional<User> user = userRepository.findByLogin(login);
        if (user.isPresent() && passwordEncoder.matches(password, user.get().getPassword())) {
            log.info("User '{}' found and password matches\n\n", login);
            UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(user.get().getLogin())
                .password(user.get().getPassword())  // mot de passe encodé
                .roles("USER")                 // rôle par défaut
                .build();
            return jwtService.generateToken(userDetails);
        } else {
            throw new IllegalArgumentException("Invalid credentials");
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
                "User with login '" + newUser.getLogin() + "' already exists"
            );
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
}
