package com.openclassrooms.etudiant.dto;

import com.openclassrooms.etudiant.entities.UserRoleEnum;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserBasicInfoDTO {
    private long id;
    private String firstName;
    private String lastName;
    private UserRoleEnum role;
}
