package com.openclassrooms.etudiant.dto;

import com.openclassrooms.etudiant.entities.UserRoleEnum;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class UpdateRequestDTO {
    private String firstName;
    private String lastName;
    private String login;
    private String password;
    @NotNull(message = "Role is required")
    private UserRoleEnum role;
}
