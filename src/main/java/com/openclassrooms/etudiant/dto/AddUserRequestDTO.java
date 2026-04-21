package com.openclassrooms.etudiant.dto;

import com.openclassrooms.etudiant.entities.UserRoleEnum;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddUserRequestDTO {
    @NotBlank(message = "First name is required")
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    private String lastName;
    
    @NotBlank(message = "Login is required")
    private String login;
    
    @NotBlank(message = "Password is required")
    private String password;

    @NotNull(message = "Role is required")
    private UserRoleEnum role;
    
}
