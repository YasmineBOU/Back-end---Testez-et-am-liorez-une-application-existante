package com.openclassrooms.etudiant.entities;

public enum UserRoleEnum {
    ADMIN("ADMIN"),
    USER("USER");

    private final String role;

    UserRoleEnum(String role) {
        this.role = role;
    }

    public String getRole() {
        return role;
    }
}
