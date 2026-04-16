package com.openclassrooms.etudiant.dto;

import lombok.Data;

@Data
public class UpdateRequestDTO {
    private String firstName;
    private String lastName;
    private String login;
    private String password;
    
}
