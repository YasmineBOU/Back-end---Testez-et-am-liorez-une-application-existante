package com.openclassrooms.etudiant.controller;

import com.openclassrooms.etudiant.dto.AddUserRequestDTO;
import com.openclassrooms.etudiant.dto.UpdateRequestDTO;
import com.openclassrooms.etudiant.dto.UserBasicInfoDTO;
import com.openclassrooms.etudiant.dto.LoginRequestDTO;
import com.openclassrooms.etudiant.dto.RegisterDTO;
import com.openclassrooms.etudiant.dto.UserSummaryDTO;
import com.openclassrooms.etudiant.entities.User;
import com.openclassrooms.etudiant.entities.UserRoleEnum;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

    public ResponseEntity<?> userIsAuthenticated(User authenticatedUser) {
        // Check if the authenticated user is present in the security context
        if (authenticatedUser == null) {
            return new ResponseEntity<>(
                    Map.of("error", "Authentication required"),
                    HttpStatus.UNAUTHORIZED);

        }
        // Only ADMIN users can perform user management operations
        if (authenticatedUser.getRole() != UserRoleEnum.ADMIN) {
            return new ResponseEntity<>(
                    Map.of("error", "Admin privileges required"),
                    HttpStatus.FORBIDDEN);

        }
        return null; // indicates authentication is valid
    }

    @PostMapping("/api/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterDTO registerDTO) {
        userService.register(userDtoMapper.toEntity(registerDTO));
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PostMapping("/api/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO loginRequestDTO) {
        String jwtToken = userService.login(loginRequestDTO.getLogin(), loginRequestDTO.getPassword());
        return ResponseEntity.ok(Map.of("token", jwtToken, "message", "Logged successfully !"));
    }

    @PostMapping("/api/add-user")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> addUser(
            @Valid @RequestBody AddUserRequestDTO addUserRequestDTO,
            @AuthenticationPrincipal User authenticatedUser) {
        ResponseEntity<?> authResponse = userIsAuthenticated(authenticatedUser);
        try {
            // Validate authenticated user exists
            if (authResponse != null) {
                return authResponse;
            }

            // Map DTO to entity
            User newUser = userDtoMapper.toEntity(addUserRequestDTO);
            // Call service to add user
            User createdUser = userService.addUser(newUser, authenticatedUser);
            // Return success response (201 Created)
            return new ResponseEntity<>(
                    Map.of(
                            "message", "User created successfully",
                            "userId", createdUser.getId(),
                            "login", createdUser.getLogin()),
                    HttpStatus.CREATED);

        } catch (IllegalArgumentException e) {
            // Handles: duplicate login
            return new ResponseEntity<>(
                    Map.of("error", e.getMessage()),
                    HttpStatus.BAD_REQUEST);
        } catch (IllegalStateException e) {
            // Handles: missing authentication context
            return new ResponseEntity<>(
                    Map.of("error", "Authentication required"),
                    HttpStatus.UNAUTHORIZED);
        }
    }

    @GetMapping("/api/read-user")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> readUser(
            @AuthenticationPrincipal User authenticatedUser) {

        try {
            // Validate authenticated user exists
            ResponseEntity<?> authResponse = userIsAuthenticated(authenticatedUser);
            if (authResponse != null) {
                return authResponse;
            }
            Iterable<UserBasicInfoDTO> users = userService.getUsers(authenticatedUser);
            return new ResponseEntity<>(users, HttpStatus.OK);
        } catch (IllegalStateException e) {
            // Handles: missing authentication context
            return new ResponseEntity<>(
                    Map.of("error", "Authentication required"),
                    HttpStatus.UNAUTHORIZED);
        }
    }

    @GetMapping("/api/read-user/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> readUserById(
            @AuthenticationPrincipal User authenticatedUser,
            @PathVariable Long id) {

        try {
            // Validate authenticated user exists
            ResponseEntity<?> authResponse = userIsAuthenticated(authenticatedUser);
            if (authResponse != null) {
                return authResponse;
            }

            UserSummaryDTO user = userService.getUserById(authenticatedUser, id);
            return new ResponseEntity<>(user, HttpStatus.OK);
        } catch (IllegalStateException e) {
            // Handles: missing authentication context
            return new ResponseEntity<>(
                    Map.of("error", "Authentication required"),
                    HttpStatus.UNAUTHORIZED);
        }
    }

    @DeleteMapping("/api/delete-user/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteUserById(
            @AuthenticationPrincipal User authenticatedUser,
            @PathVariable Long id) {

        // Validate authenticated user exists
        ResponseEntity<?> authResponse = userIsAuthenticated(authenticatedUser);
        if (authResponse != null) {
            return authResponse;
        }

        userService.deleteUser(authenticatedUser, id);
        return new ResponseEntity<>(Map.of("message", "User with id " + id + " deleted successfully !"),
                HttpStatus.OK);

    }

    @PutMapping("/api/update-user/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateUser(
            @Valid @RequestBody UpdateRequestDTO updateRequestDTO,
            @PathVariable Long id,
            @AuthenticationPrincipal User authenticatedUser

    ) {

        // Validate authenticated user exists
        ResponseEntity<?> authResponse = userIsAuthenticated(authenticatedUser);
        if (authResponse != null) {
            return authResponse;
        }
        userService.updateUser(
                authenticatedUser,
                id,
                userDtoMapper.toEntity(updateRequestDTO));
        return new ResponseEntity<>(Map.of(
                "message", "User with id " + id + " updated successfully !"),
                HttpStatus.OK);

    }
}
