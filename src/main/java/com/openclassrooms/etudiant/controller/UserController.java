package com.openclassrooms.etudiant.controller;

import com.openclassrooms.etudiant.dto.AddUserRequestDTO;
import com.openclassrooms.etudiant.dto.UpdateRequestDTO;
import com.openclassrooms.etudiant.dto.LoginRequestDTO;
import com.openclassrooms.etudiant.dto.RegisterDTO;
import com.openclassrooms.etudiant.dto.UserSummaryDTO;
import com.openclassrooms.etudiant.entities.User;
import com.openclassrooms.etudiant.mapper.UserDtoMapper;
import com.openclassrooms.etudiant.service.UserService;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j // to delete after debugging
@RestController
@RequestMapping
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    private final UserDtoMapper userDtoMapper;

    @PostMapping("/api/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterDTO registerDTO) {
        userService.register(userDtoMapper.toEntity(registerDTO));
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PostMapping("/api/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO loginRequestDTO) {
        String jwtToken = userService.login(loginRequestDTO.getLogin(), loginRequestDTO.getPassword());
        log.info("Token: '{}'", jwtToken);
        return ResponseEntity.ok(Map.of("token", jwtToken, "message", "Logged successfully !"));
    }
    @PostMapping("/api/add-user")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> addUser(
            @Valid @RequestBody AddUserRequestDTO addUserRequestDTO,
            @AuthenticationPrincipal User authenticatedUser
    ) {
        
        try {
            // Validate authenticated user exists
            if (authenticatedUser == null) {
                log.info("Authentication context missing");
                return new ResponseEntity<>(
                    Map.of("error", "Authentication required"),
                    HttpStatus.UNAUTHORIZED
                );
            }
            log.info("Authenticated user: {}", authenticatedUser.getLogin());
            
            // Map DTO to entity
            User newUser = userDtoMapper.toEntity(addUserRequestDTO);
            log.info("Adding new user with login: {}", newUser.getLogin());
            // Call service to add user
            User createdUser = userService.addUser(newUser, authenticatedUser);
            log.info("User '{}' added successfully with ID: {}", createdUser.getLogin(), createdUser.getId());
            // Return success response (201 Created)
            return new ResponseEntity<>(
                Map.of(
                    "message", "User created successfully",
                    "userId", createdUser.getId(),
                    "login", createdUser.getLogin()
                ),
                HttpStatus.CREATED
            );
            
        } catch (IllegalArgumentException e) {
            // Handles: duplicate login
            log.info("(Duplicate login)Error adding user: {}", e.getMessage());
            return new ResponseEntity<>(
                Map.of("error", e.getMessage()),
                HttpStatus.BAD_REQUEST
            );
        } catch (IllegalStateException e) {
            // Handles: missing authentication context
            log.info("(Missing authentication context) Error adding user: {}", e.getMessage());
            return new ResponseEntity<>(
                Map.of("error", "Authentication required"),
                HttpStatus.UNAUTHORIZED
            );
        }
    }

    @GetMapping("/api/read-user")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> readUser(
        @AuthenticationPrincipal User authenticatedUser
    ) {
          
        try {
            // Validate authenticated user exists
            if (authenticatedUser == null) {
                log.info("Authentication context missing");
                return new ResponseEntity<>(
                    Map.of("error", "Authentication required"),
                    HttpStatus.UNAUTHORIZED
                );
            }
            log.info("Authenticated user: {}", authenticatedUser.getLogin());
            Iterable<UserSummaryDTO> users = userService.getUsers(authenticatedUser);
            return new ResponseEntity<>(users, HttpStatus.OK);
        } catch (IllegalStateException e) {
            // Handles: missing authentication context
            log.info("(Missing authentication context) Error reading users: {}", e.getMessage());
            return new ResponseEntity<>(
                Map.of("error", "Authentication required"),
                HttpStatus.UNAUTHORIZED
            );
        }        
    }

    @GetMapping("/api/read-user/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> readUserById(
        @AuthenticationPrincipal User authenticatedUser,
        @PathVariable Long id
    ) {
          
        try {
            // Validate authenticated user exists
            if (authenticatedUser == null) {
                log.info("Authentication context missing");
                return new ResponseEntity<>(
                    Map.of("error", "Authentication required"),
                    HttpStatus.UNAUTHORIZED
                );
            }
            log.info("Searching for user with id: {}", id);
            UserSummaryDTO user = userService.getUserById(authenticatedUser, id);
            return new ResponseEntity<>(user, HttpStatus.OK);
        } catch (IllegalStateException e) {
            // Handles: missing authentication context
            log.info("(Missing authentication context) Error reading users: {}", e.getMessage());
            return new ResponseEntity<>(
                Map.of("error", "Authentication required"),
                HttpStatus.UNAUTHORIZED
            );
        }        
    }
    
    @GetMapping("/api/delete-user/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteUserById(
        @AuthenticationPrincipal User authenticatedUser,
        @PathVariable Long id
    ) {
          
        try {
            // Validate authenticated user exists
            if (authenticatedUser == null) {
                log.info("Authentication context missing");
                return new ResponseEntity<>(
                    Map.of("error", "Authentication required"),
                    HttpStatus.UNAUTHORIZED
                );
            }
            log.info("Searching for user with id: {}", id);
            userService.deleteUserById(authenticatedUser, id);
            return new ResponseEntity<>(Map.of("message", "User with id " + id + " deleted successfully !"), HttpStatus.OK);
        } catch (IllegalStateException e) {
            // Handles: missing authentication context
            log.info("(Missing authentication context) Error deleting user: {}", e.getMessage());
            return new ResponseEntity<>(
                Map.of("error", "Error deleting user: " + e.getMessage()),
                HttpStatus.NOT_FOUND
            );
        }        
    }
    
    @PatchMapping("/api/update-user/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateUser(
        @Valid @RequestBody UpdateRequestDTO updateRequestDTO,
        @PathVariable Long id,
        @AuthenticationPrincipal User authenticatedUser
        
    ) {
          
        try {
            // Validate authenticated user exists
            if (authenticatedUser == null) {
                log.info("Authentication context missing");
                return new ResponseEntity<>(
                    Map.of("error", "Authentication required"),
                    HttpStatus.UNAUTHORIZED
                );
            }
            log.info("Updating user with id: {}", id);
            userService.updateUser(authenticatedUser, id, updateRequestDTO);
            return new ResponseEntity<>(Map.of("message", "User with id " + id + " updated successfully !"), HttpStatus.OK);
        } catch (IllegalStateException e) {
            // Handles: missing authentication context
            log.info("(Missing authentication context) Error updating user: {}", e.getMessage());
            return new ResponseEntity<>(
                Map.of("error", "Error updating user: " + e.getMessage()),
                HttpStatus.NOT_FOUND
            );
        }        
    }
}
